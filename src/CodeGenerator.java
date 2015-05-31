import java.io.*;

class CodeGenerator implements AATVisitor
{

	public CodeGenerator(String output_filename)
	{
		try
		{
			output = new PrintWriter(new FileOutputStream(output_filename));
		} catch (IOException e)
		{
			System.out.println("Could not open file " + output_filename
					+ " for writing.");
		}
		EmitSetupCode();
	}

	public Object VisitSequential(AATSequential statement)
	{
		statement.left().Accept(this);
		statement.right().Accept(this);
		return null;
	}

	public Object VisitMove(AATMove statement)
	{
		if (statement.lhs() instanceof AATRegister)
		{
			// get the value of where we are storing it
			AATRegister reg = (AATRegister) statement.lhs();
			String regString = reg.register().toString();

			// get the value of what we are storing
			statement.rhs().Accept(this);

			emit("addi " + regString + ", " + Register.ACC() + ", 0");

		} else if (statement.lhs() instanceof AATMemory)
		{
			/* emit code for right hand side */

			// get the value of where we are storing it
			AATMemory lhsMem = (AATMemory) statement.lhs();
			lhsMem.mem().Accept(this);

			emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
			emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

			// left side should be at 4(esp)

			// get the value of what we are storing
			statement.rhs().Accept(this);

			// load left side into reg
			emit("lw " + Register.Tmp1() + ", " + "4(" + Register.ESP() + ")");
			emit("addi " + Register.ESP() + ", " + Register.ESP() + ", 4");

			// " + Register.ACC() + " contains value of what were storing
			// $t1 contains address

			emit("sw " + Register.ACC() + ", 0(" + Register.Tmp1() + ")");

		} else
		{
			System.out
					.println("ERROR: LHS of move statement must be AATRegister or AATMemory, got: "
							+ statement.lhs());
		}
		return null;
	}

	public Object VisitEmpty(AATEmpty statement)
	{
		return null;
	}

	public Object VisitLabel(AATLabel statement)
	{
		emit(statement.label() + ":");
		return null;
	}

	public Object VisitConstant(AATConstant expression)
	{
		emit("addi " + Register.ACC() + ", $zero, " + expression.value());
		return null;
	}

	public Object VisitRegister(AATRegister expression)
	{
		// push the value in the register on the stack
		emit("addi " + Register.ACC() + ", " + expression.register() + ", 0");
		return null;
	}

	public Object VisitOperator(AATOperator expression)
	{

		expression.left().Accept(this);
		// value is in " + Register.ACC() + ", store it on the stack
		emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
		emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

		if (expression.right() != null)
		{
			// case for unary operator
			expression.right().Accept(this);
		}

		// pop left operand into reg
		emit("lw " + Register.Tmp1() + ", 4(" + Register.ESP() + ")");
		emit("addi " + Register.ESP() + ", " + Register.ESP() + ", 4");

		// left operand value is in Register.Tmp1()
		// right operand value is in " + Register.ACC() + "

		switch (expression.operator())
		{
		case AATOperator.PLUS:
		{
			emit("add " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.MINUS:
		{
			emit("sub " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.MULTIPLY:
		{
			emit("mul " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.DIVIDE:
		{
			emit("div " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.AND:
		{
			emit("and " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.OR:
		{
			emit("or " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}
		case AATOperator.EQUAL:
		{
			Label equalLabel = new Label("ISTRUE");
			Label endLabel = new Label("END");
			emit("beq " + Register.Tmp1() + ", " + Register.ACC() + ", "
					+ equalLabel);
			emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 0"); // false
			emit("j " + endLabel);
			emit(equalLabel + ":");
			emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 1"); // true
			emit(endLabel + ":");
			break;
		}
		case AATOperator.NOT_EQUAL:
		{
			Label trueLabel = new Label("ISTURE");
			Label endLabel = new Label("END");
			emit("bne " + Register.Tmp1() + ", " + Register.ACC() + ", "
					+ trueLabel);
			emit("li " + Register.ACC() + ", 0"); // false
			emit("j " + endLabel);
			emit(trueLabel + ":");
			emit("li " + Register.ACC() + ", 1"); // true
			emit(endLabel + ":");
			break;
		}
		case AATOperator.LESS_THAN:
		{
			emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}

		case AATOperator.LESS_THAN_EQUAL:
		{
			emit("addi " + Register.ACC() + ", " + Register.ACC() + ", 1");
			emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", "
					+ Register.ACC());
			break;
		}

		case AATOperator.GREATER_THAN:
		{
			emit("slt " + Register.ACC() + ", " + Register.ACC() + ", "
					+ Register.Tmp1());
			break;
		}

		case AATOperator.GREATER_THAN_EQUAL:
		{
			emit("addi " + Register.Tmp1() + ", " + Register.Tmp1() + ", 1");
			emit("slt " + Register.ACC() + ", " + Register.ACC() + ", "
					+ Register.Tmp1());
			break;
		}
		case AATOperator.NOT:
		{

			/*
			 * 1 xor 1 -> 0 0 xor 1 -> 1
			 */

			emit("li " + Register.Tmp2() + ", 1");
			emit("xor " + Register.ACC() + " , " + Register.Tmp1() + ", "
					+ Register.Tmp2());
			break;
		}
		default:
		{
			System.out.println("ERROR: Unknown operator "
					+ expression.operator());
			break;
		}
		}

		return null;
	}

	public Object VisitMemory(AATMemory expression)
	{
		expression.mem().Accept(this);
		emit("lw " + Register.ACC() + ", 0(" + Register.ACC() + ")");
		return null;
	}

	public Object VisitCallExpression(AATCallExpression expression)
	{
		AATCallStatement callStatement = new AATCallStatement(
				expression.label(), expression.actuals());
		callStatement.Accept(this);
		emit("move " + Register.ACC() + ", " + Register.Result());
		return null;
	}

	public Object VisitCallStatement(AATCallStatement statement)
	{
		for (int i = statement.actuals().size() - 1; i >= 0; i--)
		{
			AATExpression aatExp = (AATExpression) statement.actuals()
					.elementAt(i);
			aatExp.Accept(this);
			// value of arg should be in $ACC
			// push onto stack
			emit("sw " + Register.ACC() + ", 0(" + Register.SP() + ")");
			emit("addi " + Register.SP() + ", " + Register.SP() + ", "
					+ (-1 * MachineDependent.WORDSIZE));
		}

		// jump to function
		emit("jal " + statement.label());
		emit("addi " + Register.SP() + ", " + Register.SP() + ", "
				+ (MachineDependent.WORDSIZE * statement.actuals().size()));
		return null;
	}

	public Object VisitConditionalJump(AATConditionalJump statement)
	{
		statement.test().Accept(this);
		emit("bne " + Register.ACC() + ", " + Register.Zero() + ", "
				+ statement.label());
		return null;
	}

	public Object VisitJump(AATJump statement)
	{
		emit("j " + statement.label());
		return null;
	}

	public Object VisitReturn(AATReturn statement)
	{
		emit("jr " + Register.ReturnAddr());
		return null;
	}

	public Object VisitHalt(AATHalt halt)
	{
		/*
		 * Don't need to implement halt -- you can leave this as it is, if you
		 * like
		 */
		return null;
	}

	private void emit(String assem)
	{
		assem = assem.trim();
		if (assem.charAt(assem.length() - 1) == ':')
		{
			output.println(assem);
		} else
		{
			output.println("\t" + assem);
		}
	}

	public void GenerateLibrary()
	{
		emit("Print:");
		emit("lw $a0, 4(" + Register.SP() + ")");
		emit("li $v0, 1");
		emit("syscall");
		emit("li $v0,4");
		emit("la $a0, sp");
		emit("syscall");
		emit("jr $ra");
		emit("Println:");
		emit("li $v0,4");
		emit("la $a0, cr");
		emit("syscall");
		emit("jr $ra");
		emit("Read:");
		emit("li $v0,5");
		emit("syscall");
		emit("jr $ra");
		emit("allocate:");
		emit("la " + Register.Tmp1() + ", HEAPPTR");
		emit("lw " + Register.Result() + ",0(" + Register.Tmp1() + ")");
		emit("lw " + Register.Tmp2() + ", 4(" + Register.SP() + ")");
		emit("sub " + Register.Tmp2() + "," + Register.Result() + ","
				+ Register.Tmp2());
		emit("sw " + Register.Tmp2() + ",0(" + Register.Tmp1() + ")");
		emit("jr $ra");
		emit(".data");
		emit("cr:");
		emit(".asciiz \"\\n\"");
		emit("sp:");
		emit(".asciiz \" \"");
		emit("HEAPPTR:");
		emit(".word 0");
		output.flush();
	}

	private void EmitSetupCode()
	{
		emit(".globl main");
		emit("main:");
		emit("addi " + Register.ESP() + "," + Register.SP() + ",0");
		emit("addi " + Register.SP() + "," + Register.SP() + ","
				+ -MachineDependent.WORDSIZE * STACKSIZE);
		emit("addi " + Register.Tmp1() + "," + Register.SP() + ",0");
		emit("addi " + Register.Tmp1() + "," + Register.Tmp1() + ","
				+ -MachineDependent.WORDSIZE * STACKSIZE);
		emit("la " + Register.Tmp2() + ", HEAPPTR");
		emit("sw " + Register.Tmp1() + ",0(" + Register.Tmp2() + ")");
		emit("sw " + Register.ReturnAddr() + "," + MachineDependent.WORDSIZE
				+ "(" + Register.SP() + ")");
		emit("jal main1");
		emit("li $v0, 10");
		emit("syscall");
	}

	private final int STACKSIZE = 1000;
	private PrintWriter output;
	/* Feel Free to add more instance variables, if you like */

}
