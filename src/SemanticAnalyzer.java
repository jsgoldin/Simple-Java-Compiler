import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor
{

	public TypeEnvironment typeEnv;
	public FunctionEnvironment funcEnv;
	public VariableEnvironment varEnv;
	public AATBuildTree buildTree;
	public int variableOffset;
	public Label curFuncEndLabel;
	public Type curFunctionReturnType = null;
	public static final int indentstep = 3;
	public int indentlevel = 0;

	public SemanticAnalyzer()
	{
		typeEnv = new TypeEnvironment();
		funcEnv = new FunctionEnvironment();
		funcEnv.addBuiltinFunctions();
		varEnv = new VariableEnvironment();
		buildTree = new AATBuildTree();
		variableOffset = 0;
	}

	public Object VisitProgram(ASTProgram program)
	{
		if (program.classes() != null)
		{
			program.classes().Accept(this);
		}

		Vector<AATStatement> aatFunctionDefs = null;
		if (program.functiondefinitions() != null)
		{
			aatFunctionDefs = (Vector<AATStatement>) program
					.functiondefinitions().Accept(this);
		}

		if (aatFunctionDefs != null)
		{
			return buildTree.createSequenceTree(aatFunctionDefs);
		} else
		{
			// no functions exist!
			return null;
		}
	}

	public Object VisitClasses(ASTClasses classes)
	{
		int i;

		for (i = 0; i < classes.size(); i++)
		{
			classes.elementAt(i).Accept(this);
		}
		return null;
	}

	public Object VisitClass(ASTClass classs)
	{
		VariableEnvironment tempVarEnv = null;

		variableOffset = 0;
		if (classs.variabledefs() != null)
		{
			tempVarEnv = (VariableEnvironment) classs.variabledefs().Accept(
					this);
		}

		Type classType = new ClassType(tempVarEnv);
		typeEnv.insert(classs.name(), classType);

		indentlevel--;
		return null;
	}

	public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs)
	{
		VariableEnvironment tempVarEnv = new VariableEnvironment();
		VariableEntry varEntry;
		for (int i = 0; i < variabledefs.size(); i++)
		{
			varEntry = (VariableEntry) variabledefs.elementAt(i).Accept(this);
			varEntry.setoffset(variableOffset);
			if (varEntry != null)
			{
				tempVarEnv.insert(variabledefs.elementAt(i).name(), varEntry);
			}
			variableOffset += MachineDependent.WORDSIZE;
		}
		return tempVarEnv;
	}

	public Object VisitInstanceVariableDef(ASTInstanceVariableDef variabledef)
	{
		String array = "";
		for (int i = 0; i < variabledef.arraydimension(); i++)
		{
			array = array + "[]";
		}

		String typeName = variabledef.type();
		Type typeInstance = typeEnv.find(typeName);

		Type examinedType = examineType(variabledef.type(),
				variabledef.arraydimension(), variabledef.line());

		return new VariableEntry(examinedType);
	}

	public Object VisitFunctionDefinitions(ASTFunctionDefinitions functiondefs)
	{
		Vector<AATStatement> aatFunctionDefs = new Vector<AATStatement>();
		int i;
		for (i = 0; i < functiondefs.size(); i++)
		{
			AATStatement aatFunc = (AATStatement) functiondefs.elementAt(i)
					.Accept(this);
			if (aatFunc != null)
			{
				aatFunctionDefs.add(aatFunc);
			}
		}
		return aatFunctionDefs;
	}

	public Object VisitPrototype(ASTPrototype prototype)
	{
		// Create a function entry for the prototype

		Vector formals = new Vector(5);
		for (int i = 0; i < prototype.formals().size(); i++)
		{
			ASTFormal curFormal = prototype.formals().elementAt(i);
			Type examinedType = examineType(curFormal.type(),
					curFormal.arraydimension(), curFormal.line());
			formals.add(examinedType);
		}

		Label startlabel = new Label(prototype.name());
		Label endlabel = new Label(prototype.name() + "END");

		Type examinedType = examineType(prototype.type(), 0, prototype.line());
		FunctionEntry newFuncEntry = new FunctionEntry(examinedType, formals,
				startlabel, endlabel);

		// insert the function entry into the function environoment
		funcEnv.insert(prototype.name(), newFuncEntry);

		return null;
	}

	public AATStatement VisitFunction(ASTFunction function)
	{

		boolean errorFlag = false;
		// verify the formals of the function are all valid types

		// save the types, needed later for prototype creation/checking
		Vector functionArgTypes = new Vector(5);

		for (int i = 0; i < function.formals().size(); i++)
		{
			ASTFormal curFormal = function.formals().elementAt(i);
			Type examinedType = examineType(curFormal.type(),
					curFormal.arraydimension(), curFormal.line());
			functionArgTypes.add(examinedType);

			if (examinedType == null)
			{
				errorFlag = true;
			}
		}

		// Check if a function entry exists for the visisted function,
		// if not create one and add it to the function environment
		FunctionEntry funcEntry = funcEnv.find(function.name());
		if (funcEntry == null)
		{
			/* prototype does not exist */

			// do not bother creating prototype if definition types are invalid
			if (!errorFlag)
			{
				// create the prototype
				Type functionReturnType = examineType(function.type(), 0,
						function.line());
				Label start = new Label(function.name());
				Label end = new Label(function.name() + "END");
				FunctionEntry newFuncEntry = new FunctionEntry(
						functionReturnType, functionArgTypes, start, end);
				funcEnv.insert(function.name(), newFuncEntry);
			}
		} else
		{
			/* prototype already exists */

			// verify the definition matches it
			if (funcEntry.result() != typeEnv.find(function.type()))
			{
				CompError
						.message(function.line(),
								"Prototype return type does not match function return type");
				errorFlag = true;
			}

			if (funcEntry.formals().size() != function.formals().size())
			{
				CompError
						.message(function.line(),
								"Prototype args does not match length of function args!");
				errorFlag = true;
			} else
			{
				for (int i = 0; i < functionArgTypes.size(); i++)
				{
					// check that the prototype and function definition have
					// the same arguements
					ASTFormal curFormal = function.formals().elementAt(i);
					Type protoTypeArgType = (Type) funcEntry.formals()
							.elementAt(i);
					Type functionArgType = (Type) functionArgTypes.elementAt(i);

					if (protoTypeArgType != functionArgType)
					{
						CompError
								.message(function.line(),
										"Function definition args do not match prototype args");
						errorFlag = true;
					}
				}
			}
		}

		varEnv.beginScope();

		curFuncEndLabel = funcEnv.find(function.name()).endlabel();

		variableOffset = 0;
		// add the parameters to the variable environment
		for (int i = 0; i < function.formals().size(); i++)
		{
			variableOffset -= MachineDependent.WORDSIZE;
			ASTFormal curFormal = function.formals().elementAt(i);
			Type funcArgType = (Type) functionArgTypes.elementAt(i);
			varEnv.insert(curFormal.name(), new VariableEntry(funcArgType,
					variableOffset));
		}

		indentlevel++;
		if (function.formals() != null)
		{
			function.formals().Accept(this);
		}

		curFunctionReturnType = (Type) typeEnv.find(function.type());
		Type protoReturnType = funcEnv.find(function.name()).result();

		// make sure the return type of the function matches its prototype
		if (curFunctionReturnType != protoReturnType)
		{
			CompError.message(function.line(),
					"Function definition return type \""
							+ curFunctionReturnType
							+ "\" doesn't match prototype return type \""
							+ protoReturnType + "\"!");
		}

		variableOffset = 0;
		AATStatement bodySeq = (AATStatement) function.body().Accept(this);
		int frameSize = variableOffset;
		variableOffset = 0;

		varEnv.endScope();
		curFunctionReturnType = null;

		funcEntry = funcEnv.find(function.name());

		if (funcEntry != null)
		{
			return buildTree.functionDefinition(bodySeq, frameSize,
					funcEntry.startlabel(), funcEntry.endlabel());
		} else
		{
			return null;
		}
	}

	public Object VisitFormals(ASTFormals formals)
	{
		return null;
	}

	public Object VisitFormal(ASTFormal formal)
	{

		Type t = examineType(formal.type(), formal.arraydimension(),
				formal.line());

		return null;
	}

	public AATStatement VisitStatements(ASTStatements statements)
	{
		varEnv.beginScope();
		Vector<AATStatement> aatStatements = new Vector<AATStatement>();
		for (int i = 0; i < statements.size(); i++)
		{

			AATStatement aatStatement = (AATStatement) statements.elementAt(i)
					.Accept(this);

			if (aatStatement != null)
			{
				aatStatements.add(aatStatement);
			}

		}
		varEnv.endScope();

		// convert the statements to a tree

		return buildTree.createSequenceTree(aatStatements);
	}

	/*
	 * 		|		Statements 		|
	 * 		V				  	 	V
	 */
	
	
	/*
	 * Called when visiting the declaration of a variable. The variable should
	 * be added to the variable environment.
	 */
	public AATStatement VisitVariableDefStatement(
			ASTVariableDefStatement variabledef)
	{
		// Construct the VariableEntry for representing the variable in the
		// variable environment

		// The VariableEntry needs to know the type of the variable that was
		// declared. We can look it up in the type environment using the string

		Type leftSideType = examineType(variabledef.type(),
				variabledef.arraydimension(), variabledef.line());

		VariableEntry varEntry = new VariableEntry(leftSideType, variableOffset);
		varEnv.insert(variabledef.name(), varEntry);
		variableOffset += MachineDependent.WORDSIZE;

		// at this point we know if the type being declared is okay
		// if not the type name key will point to null in the type environment

		ASTExpression init = variabledef.init();
		if (init != null)
		{

			TypeClass rightHandTypeClass = (TypeClass) init.Accept(this); // visit
																			// an
																			// expression
			Type rightHandType = rightHandTypeClass.type();

			// null return type or right hand side indicates it is malformed
			// only type check left and right side if it is not null

			if (rightHandType == null || leftSideType == null)
			{
				// a non existant type was used in an assignment statement
				if (rightHandType == null)
				{
					CompError
							.message(variabledef.line(),
									"Right side type of assignment statement not recognized: ");
				} else if (leftSideType != null)
				{
					CompError
							.message(variabledef.line(),
									"Left side type of assignment statement not recognized: ");
				}
			} else if (rightHandType != leftSideType)
			{
				CompError.message(variabledef.line(),
						"Type mismatch on assignment: " + leftSideType + " != "
								+ rightHandType);
			} else
			{
				AATStatement defStatement = buildTree.assignmentStatement(
						buildTree.baseVariable(varEntry.offset()),
						rightHandTypeClass.value());
				return defStatement;
			}
		}

		return null;
	}

	public Object VisitAssignmentStatement(ASTAssignmentStatement assign)
	{
		// make sure the types are the same on either side of the equals sign

		TypeClass leftSideTypeClass = (TypeClass) assign.variable()
				.Accept(this);
		TypeClass rightSideTypeClass = (TypeClass) assign.value().Accept(this);
		if (leftSideTypeClass == null || rightSideTypeClass == null)
		{
			return null;
		}

		Type leftSideType = leftSideTypeClass.type();
		Type rightSideType = rightSideTypeClass.type();

		if (leftSideType != rightSideType && leftSideType != null
				&& rightSideType != null)
		{
			CompError.message(assign.line(), "Type mismatch on assignment: "
					+ leftSideType + " != " + rightSideType);
		}

		AATStatement assignStatement = buildTree.assignmentStatement(
				leftSideTypeClass.value(), rightSideTypeClass.value());

		return assignStatement;
	}

	public AATStatement VisitDoWhileStatement(ASTDoWhileStatement dowhile)
	{
		TypeClass testTypeClass = (TypeClass) dowhile.test().Accept(this);
		Type testType = testTypeClass.type();
		if (testType != BooleanType.instance())
		{
			CompError.message(dowhile.line(),
					"Do while test was not a boolean!");
		}

		AATStatement aatDoWhileBody = (AATStatement) dowhile.body()
				.Accept(this);
		return buildTree
				.dowhileStatement(testTypeClass.value(), aatDoWhileBody);

	}

	public AATStatement VisitEmptyStatement(ASTEmptyStatement empty)
	{
		return buildTree.emptyStatement();
	}

	public Object VisitForStatement(ASTForStatement forstmt)
	{
		AATStatement aatForInit = (AATStatement) forstmt.initialize().Accept(
				this);

		TypeClass testTypeClass = (TypeClass) forstmt.test().Accept(this);
		if (testTypeClass == null)
		{
			return null;
		}

		Type testType = testTypeClass.type();

		AATStatement aatForInc = (AATStatement) forstmt.increment()
				.Accept(this);
		AATStatement aatForBody = (AATStatement) forstmt.body().Accept(this);

		if (testType != BooleanType.instance())
		{
			CompError.message(forstmt.line(),
					"For loop test was not a boolean!");
		}

		return buildTree.forStatement(aatForInit, testTypeClass.value(),
				aatForInc, aatForBody);
	}

	public AATStatement VisitFunctionCallStatement(
			ASTFunctionCallStatement functioncall)
	{

		// make sure function is in function env
		// and that args match up
		// make sure the return type is void

		AATStatement aatFuncStatement = null;

		FunctionEntry funcEntry = funcEnv.find(functioncall.name());

		if (funcEntry != null)
		{

			/*
			 * Do I need this? It makes it illegal to call non void functions as
			 * statements if (funcEntry.result() != VoidType.instance()) {
			 * CompError.message(functioncall.line(), functioncall.name() +
			 * " is not a void function"); }
			 */

			// make sure parameters are good
			if (funcEntry.formals().size() != functioncall.size())
			{
				CompError
						.message(functioncall.line(),
								"function proto args length differs from function call args length");
			} else
			{

				Vector<AATExpression> aatFuncArgs = new Vector<AATExpression>();

				for (int i = 0; i < funcEntry.formals().size(); i++)
				{
					Type funcEntryArgType = (Type) funcEntry.formals()
							.elementAt(i);
					TypeClass funcCallClassType = (TypeClass) functioncall
							.elementAt(i).Accept(this);
					Type funcCallArgType = funcCallClassType.type();
					aatFuncArgs.add(funcCallClassType.value());

					if (funcCallArgType != funcEntryArgType)
					{
						CompError.message(functioncall.line(),
								"Function call args differ from prototype");
						break;
					}
				}

				// create the AAT
				aatFuncStatement = buildTree.callStatement(aatFuncArgs,
						funcEntry.startlabel());

			}
		} else
		{
			CompError.message(functioncall.line(),
					"function proto was not found for function call");
		}

		if (aatFuncStatement != null)
		{
			return aatFuncStatement;
		} else
		{
			return null;
		}

	}

	public AATStatement VisitIfStatement(ASTIfStatement ifstmt)
	{
		// make sure test is of type boolean

		TypeClass ifTestTypeClass = (TypeClass) ifstmt.test().Accept(this);
		Type ifTestType = ifTestTypeClass.type();

		if (ifTestType != BooleanType.instance())
		{
			CompError.message(ifstmt.line(), "If test was not boolean!");
		}

		AATStatement aatThen = (AATStatement) ifstmt.thenstatement().Accept(
				this);

		AATStatement aatElse = null;
		if (ifstmt.elsestatement() != null)
		{
			aatElse = (AATStatement) ifstmt.elsestatement().Accept(this);
		} else
		{
			aatElse = buildTree.emptyStatement();
		}

		return buildTree.ifStatement(ifTestTypeClass.value(), aatThen, aatElse);

	}

	public AATStatement VisitWhileStatement(ASTWhileStatement whilestatement)
	{

		TypeClass whileTestClass = (TypeClass) whilestatement.test().Accept(
				this);
		Type whileTest = whileTestClass.type();

		if (whileTest != BooleanType.instance())
		{
			CompError.message(whilestatement.line(),
					"While test was not boolean!");
		}

		AATStatement aatWhileBody = (AATStatement) whilestatement.body()
				.Accept(this);

		return buildTree.whileStatement(whileTestClass.value(), aatWhileBody);

	}

	public AATStatement VisitReturnStatement(ASTReturnStatement ret)
	{
		TypeClass returnTypeClass = null;
		Type returnType = VoidType.instance();

		if (curFunctionReturnType != null)
		{
			// we are in a function,
			// whenever we see a return statement
			// make sure it matches the return type of the
			// function

			if (ret.value() == null)
			{
				// case when void function has return statements
				// set return value even though it won't be used
				// also change return type to int
				ret.setvalue(new ASTIntegerLiteral(0, 0));
				returnTypeClass = new TypeClass(IntegerType.instance(),
						new AATConstant(0));
			} else
			{
				returnTypeClass = (TypeClass) ret.value().Accept(this);
				returnType = returnTypeClass.type();
			}

			if (returnType != curFunctionReturnType)
			{
				CompError.message(ret.line(), "Return type \"" + returnType
						+ "\" doesn't match funtion def return type \""
						+ curFunctionReturnType + "\"");
			}

		} else
		{
			// report an error, return statement
			// in something that is not a function
			CompError.message(ret.line(),
					"Return statement outside of a function!");
		}

		// label of the function start?
		// is that in the func ENV?
		return buildTree.returnStatement(returnTypeClass.value(),
				curFuncEndLabel);
	}

	/*
	 * 		|		Expressions 	|
	 * 		V				  	 	V
	 */

	public TypeClass VisitIntegerLiteral(ASTIntegerLiteral literal)
	{

		AATExpression constExp = buildTree.constantExpression(literal.value());
		return new TypeClass(IntegerType.instance(), constExp);
	}

	public TypeClass VisitBooleanLiteral(ASTBooleanLiteral boolliteral)
	{
		int val;
		if (boolliteral.value() == true)
		{
			val = 1;
		} else
		{
			val = 0;
		}
		return new TypeClass(BooleanType.instance(),
				buildTree.constantExpression(val));
	}

	public Object VisitCharLiteral(ASTCharLiteral literal)
	{
		System.out.println("visited char literal!");
		return new TypeClass(CharacterType.instance(),
				buildTree.constantExpression((int) literal.value()));
	}

	public TypeClass VisitOperatorExpression(ASTOperatorExpression opexpr)
	{
		TypeClass leftTypeClass = (TypeClass) opexpr.left().Accept(this);
		TypeClass rightTypeClass = (TypeClass) opexpr.right().Accept(this);

		Type leftType = null;
		Type rightType = null;

		if (leftTypeClass == null || rightTypeClass == null)
		{
			return null;
		}

		leftType = leftTypeClass.type();
		rightType = rightTypeClass.type();

		// make sure the operand types work with the operator

		int operator = opexpr.operator();
		if (operator >= 1 && operator <= 4)
		{
			if ((IntegerType.instance() != leftType || IntegerType.instance() != rightType)
					&& (leftType != null && rightType != null))
			{
				CompError.message(opexpr.line(),
						"improper operands types, both must be integers");
				return null;
			} else
			{
				return new TypeClass(IntegerType.instance(),
						buildTree.operatorExpression(leftTypeClass.value(),
								rightTypeClass.value(), operator));
			}
		} else if (operator >= 9 && operator <= 12)
		{
			if ((IntegerType.instance() != leftType || IntegerType.instance() != rightType)
					&& (leftType != null && rightType != null))
			{
				CompError.message(opexpr.line(),
						"improper operands types, both must be integers");
				return null;
			} else
			{
				return new TypeClass(BooleanType.instance(),
						buildTree.operatorExpression(leftTypeClass.value(),
								rightTypeClass.value(), operator));
			}
		} else if (operator == 5 || operator == 6)
		{
			if ((BooleanType.instance() != leftType || BooleanType.instance() != rightType)
					&& (leftType != null && rightType != null))
			{
				CompError.message(opexpr.line(),
						"improper operands types, both must be boolean");
				return null;
			} else
			{
				return new TypeClass(BooleanType.instance(),
						buildTree.operatorExpression(leftTypeClass.value(),
								rightTypeClass.value(), operator));
			}
		} else if (operator == 7 || operator == 8)
		{
			// operands can be boolean or int, must they must be the same type
			if ((leftType != rightType)
					&& (leftType != null && rightType != null))
			{
				CompError.message(opexpr.line(), "Malformed expression types: "
						+ leftType + " " + rightType);
				return null;
			} else
			{
				return new TypeClass(BooleanType.instance(),
						buildTree.operatorExpression(leftTypeClass.value(),
								rightTypeClass.value(), operator));
			}
		} else
		{
			// bad operator, don't think this will ever happen
			return null;
		}
	}

	public TypeClass VisitFunctionCallExpression(
			ASTFunctionCallExpression functioncall)
	{

		// confirm the function exists in the function env

		// ensure its paramaters match the prototype in the func env

		// return the type so the left hand side can be checked

		FunctionEntry funcEntry = funcEnv.find(functioncall.name());
		Vector<AATExpression> funcAATArgs = new Vector<AATExpression>();

		if (funcEntry == null)
		{
			CompError.message(functioncall.line(), "Did not find\""
					+ functioncall.name() + "\" in the function environment");
			return null;
		} else
		{
			// make sure the paramters match

			if (functioncall.size() != funcEntry.formals().size())
			{
				CompError
						.message(functioncall.line(),
								"Function call does not have the same number of args as its definition");
			} else
			{

				for (int i = 0; i < functioncall.size(); i++)
				{

					TypeClass funcCallArgTypeClass = (TypeClass) functioncall
							.elementAt(i).Accept(this);
					Type functionCallArgType = funcCallArgTypeClass.type();
					Type funcPrototypeArgType = (Type) funcEntry.formals()
							.elementAt(i);

					funcAATArgs.add(funcCallArgTypeClass.value());

					if (funcPrototypeArgType != functionCallArgType)
					{
						CompError.message(functioncall.line(),
								"Type mismatch in function call parameters");
						break;
					}
				}
			}

			if (funcEntry != null)
			{
				return new TypeClass(funcEntry.result(),
						buildTree.callExpression(funcAATArgs,
								funcEntry.startlabel()));
			} else
			{
				return null;
			}

		}
	}

	public TypeClass VisitUnaryOperatorExpression(
			ASTUnaryOperatorExpression operator)
	{

		TypeClass operandTypeClass = (TypeClass) operator.operand()
				.Accept(this);

		if (operator.operator() == 1)
		{
			// make sure operand is boolean type
			Type operandType = operandTypeClass.type();

			if (operandType != BooleanType.instance())
			{
				CompError.message(operator.line(),
						"Invalid type used on unary operator!");
				return null;
			} else
			{

				// set rhs even though it won't be used
				return new TypeClass(BooleanType.instance(),
						buildTree.operatorExpression(operandTypeClass.value(),
								buildTree.constantExpression(0),
								AATOperator.NOT));
			}
		} else
		{
			// 0 is bad operator
			// not sure how to handle
			return null;
		}
	}

	public Object VisitNewClassExpression(ASTNewClassExpression newclass)
	{
		// make sure class is a valid type

		Type classType = (Type) typeEnv.find(newclass.type());

		if (classType == null)
		{
			CompError.message(newclass.line(), "Class type not found!");
			return null;
		} else
		{

			// need to allocate space for class instance vars
			ClassType convertedToClassType = (ClassType) classType;
			int neededSize = convertedToClassType.variables().size()
					* MachineDependent.WORDSIZE;

			AATExpression newExp = buildTree.allocate(buildTree
					.constantExpression(neededSize));
			return new TypeClass(classType, newExp);
		}
	}

	public TypeClass VisitNewArrayExpression(ASTNewArrayExpression newarray)
	{

		// XXX: How is not possible to type check
		// the expressions inside the brackets!
		// ASTNewArrayExpression only has one ASTExpression member,
		// I don't see how I can use that to see all of the slots!
		// As of now the this is valid and it shouldn't be:
		// int x[] = new int[true];

		TypeClass newArrayElem = (TypeClass) newarray.elements().Accept(this);

		if (newArrayElem.type() != IntegerType.instance())
		{
			CompError.message(newarray.line(),
					"Tried index an array with a non int");
		}

		// Construct the type name string
		String typeName = newarray.type();
		for (int i = 0; i < newarray.arraydimension(); i++)
		{
			typeName += "[]";
		}

		// caclulate space needed for the array
		AATExpression allocationSize = buildTree.operatorExpression(
				newArrayElem.value(),
				new AATConstant(MachineDependent.WORDSIZE),
				AATOperator.MULTIPLY);

		AATExpression allocateExp = buildTree.allocate(allocationSize);

		return new TypeClass(examineType(newarray.type(),
				newarray.arraydimension(), newarray.line()), allocateExp);
	}

	public TypeClass VisitVariableExpression(
			ASTVariableExpression variableexpression)
	{
		// can visit several expressions from here:
		// base
		// array
		// class

		return (TypeClass) variableexpression.variable().Accept(this);
	}

	public TypeClass VisitBaseVariable(ASTBaseVariable base)
	{
		// all variables expressions start with a base

		// make sure the variable exists in the top level var env

		VariableEntry varEntry = varEnv.find(base.name());

		if (varEntry != null)
		{
			return new TypeClass(varEntry.type(),
					buildTree.baseVariable(varEntry.offset()));
		} else
		{
			// the base doesn't exist, report an error!
			CompError.message(base.line(), base.name() + " does not exist");
			return null;
		}
	}

	public TypeClass VisitClassVariable(ASTClassVariable classvariable)
	{

		// check out its base

		// the base MUST be a ClassType

		TypeClass baseTestTypeClass = (TypeClass) classvariable.base().Accept(
				this);
		Type baseTestType = baseTestTypeClass.type();

		if (baseTestType == null)
		{
			return null;
		} else
		{
			ClassType baseRealType = null;

			if (!baseTestType.getClass().toString().equals("class ClassType"))
			{
				// the base is not a class type,
				CompError
						.message(classvariable.line(),
								" Tried to access a class variable whose base is not a class");
				return null;
			} else
			{
				// the base is a class type
				// cast it
				baseRealType = (ClassType) baseTestType;

				VariableEntry varEntry = baseRealType.variables().find(
						classvariable.variable());

				if (varEntry != null)
				{
					// look up the type of the class variable in the base's
					// varEvn
					// and return it
					return new TypeClass(baseRealType.variables()
							.find(classvariable.variable()).type(),
							buildTree.classVariable(baseTestTypeClass.value(),
									varEntry.offset()));
				} else
				{
					// the class variable is not a member of its base!
					CompError.message(classvariable.line(),
							classvariable.variable()
									+ " is not a member of its base class");
					return null;
				}
			}
		}
	}

	public TypeClass VisitArrayVariable(ASTArrayVariable array)
	{

		TypeClass typeOfIndexTypeClass = (TypeClass) array.index().Accept(this);
		Type typeOfIndex = typeOfIndexTypeClass.type();

		if (typeOfIndex != IntegerType.instance())
		{
			CompError.message(array.line(),
					"Index of array was not an integer: [" + typeOfIndex + "]");
		}

		TypeClass baseTestTypeClass = (TypeClass) array.base().Accept(this);

		if (baseTestTypeClass != null)
		{
			Type baseTestType = baseTestTypeClass.type();

			// the base must be an array type
			if (!baseTestType.getClass().toString().equals("class ArrayType"))
			{
				// the base is not an array type
				CompError
						.message(array.line(),
								" Tried to get an element of a base that is not an ArrayType variable");
				return null;
			} else
			{
				// cast it
				ArrayType arrayType = (ArrayType) baseTestType;
				return new TypeClass(arrayType.type(),
						buildTree.arrayVariable(baseTestTypeClass.value(),
								typeOfIndexTypeClass.value(),
								MachineDependent.WORDSIZE));
			}
		} else
		{
			// base was invalid
			return null;
		}
	}

	void Print(String word)
	{
		int i;
		for (i = 0; i < indentstep * indentlevel; i++)
		{
			System.out.print(" ");
		}
		System.out.println(word);
	}

	/*
	 * This method checks if the base type is valid and adds new array types to
	 * the type environment as needed.
	 * 
	 * Returns the type from the typeEnvironment if found. Returns null and
	 * reports an error otherwise.
	 */
	public Type examineType(String typeName, int arraydimension, int line)
	{
		Type declVarType = typeEnv.find(typeName);

		if (declVarType == null)
		{
			// The base type of the variable being declared was not recognized!
			CompError.message(line, "Unrecognized type: " + typeName);
			return null;
		} else
		{
			// the base type was recognized!
			// if its an array we have to
			// make sure a type exists for the array dim
			// After that we can just add an entry to the variable environment
			if (arraydimension > 0)
			{
				// first check if the array type exists in the typeEnv
				String desiredArrayTypeName = typeName;
				for (int i = 0; i < arraydimension; i++)
				{
					desiredArrayTypeName = desiredArrayTypeName + "[]";
				}
				declVarType = typeEnv.find(desiredArrayTypeName);
				if (declVarType == null)
				{
					// the array type we want is larger than anything that
					// exists in the typeEnv

					// we need to find the next largest existing one and add on
					// to it.
					String curArrayTypeName = typeName;
					while (typeEnv.find(curArrayTypeName + "[]") != null)
					{
						curArrayTypeName += "[]";
					}

					// when the loop ends curArrayTypeName will contain the last
					// biggest array type name

					// now we keep adding new array types with increasingly big
					// dimensiosn until we arrive
					// at the target dimension

					Type newArrayType = null;
					while (!curArrayTypeName.equals(desiredArrayTypeName))
					{
						// Get the last biggest Type
						Type lastBiggestType = typeEnv.find(curArrayTypeName);

						curArrayTypeName += "[]";

						// create a new array type entry
						newArrayType = new ArrayType(lastBiggestType);
						typeEnv.insert(curArrayTypeName, newArrayType);
					}
					// when this loop finishes newArrayType will contain
					// the type for the array being declared
					declVarType = newArrayType;
				}
			}
			return declVarType;
		}
	}

	class TypeClass
	{

		Type type_;
		AATExpression value_;

		public TypeClass(Type type, AATExpression value)
		{
			type_ = type;
			value_ = value;
		}

		public Type type()
		{
			return type_;
		}

		public AATExpression value()
		{
			return value_;
		}

		public void settype(Type type)
		{
			type_ = type;
		}

	}

}
