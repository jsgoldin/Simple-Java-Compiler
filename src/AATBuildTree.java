import java.util.Vector;

public class AATBuildTree
{

	/*
	 * 		|		Statements 		|
	 * 		V				  	 	V
	 */

	public AATStatement forStatement(AATStatement init, AATExpression test,
			AATStatement increment, AATStatement body)
	{

		// construct the labels first
		AATLabel forTestLabel = new AATLabel(new Label());
		AATLabel forBodyLabel = new AATLabel(new Label());

		/* construct from the bottom right up */
		AATConditionalJump forTestJump = new AATConditionalJump(test,
				forBodyLabel.label());
		AATSequential forTestSeq = new AATSequential(forTestLabel, forTestJump);
		AATSequential forBodySeq = new AATSequential(body, increment);
		AATSequential forBodySeqWrapper = new AATSequential(forBodyLabel,
				forBodySeq);
		AATSequential galles = new AATSequential(forBodySeqWrapper, forTestSeq);
		AATJump jumpToForTest = new AATJump(forTestLabel.label());
		AATSequential gallesAndJump = new AATSequential(jumpToForTest, galles);
		AATSequential forStatementTree = new AATSequential(init, gallesAndJump);

		return forStatementTree;
	}

	public AATStatement assignmentStatement(AATExpression lhs, AATExpression rhs)
	{

		AATMove assignmentStatement = new AATMove(lhs, rhs);
		return assignmentStatement;
	}

	public AATStatement emptyStatement()
	{
		return new AATEmpty();
	}

	public AATStatement ifStatement(AATExpression test, AATStatement ifbody,
			AATStatement elsebody)
	{
		// construct the labels first
		AATLabel ifTrueLabel = new AATLabel(new Label());
		AATLabel ifEndLabel = new AATLabel(new Label());

		// starting from the bottom right of the tree
		AATSequential a = new AATSequential(ifbody, ifEndLabel);
		AATSequential b = new AATSequential(ifTrueLabel, a);

		AATJump jumpToEnd = new AATJump(ifEndLabel.label());
		AATSequential c = new AATSequential(jumpToEnd, b);
		AATSequential d = new AATSequential(elsebody, c);
		AATConditionalJump ifTest = new AATConditionalJump(test,
				ifTrueLabel.label());
		AATSequential e = new AATSequential(ifTest, d);

		return e;
	}

	public AATStatement whileStatement(AATExpression test,
			AATStatement whilebody)
	{
		// construct the labels first
		AATLabel whileTestLabel = new AATLabel(new Label());
		AATLabel WhileStartLabel = new AATLabel(new Label());

		// starting from the bottom right of the tree
		AATConditionalJump whileTestJump = new AATConditionalJump(test,
				WhileStartLabel.label());
		AATSequential a = new AATSequential(whileTestLabel, whileTestJump);
		AATSequential b = new AATSequential(whilebody, a);
		AATSequential c = new AATSequential(WhileStartLabel, b);
		AATJump jumpToTest = new AATJump(whileTestLabel.label());
		AATSequential d = new AATSequential(jumpToTest, c);

		return d;
	}

	public AATStatement dowhileStatement(AATExpression test,
			AATStatement dowhilebody)
	{
		// construct the labels first
		AATLabel whileTestLabel = new AATLabel(new Label());
		AATLabel WhileStartLabel = new AATLabel(new Label());

		// starting from the bottom right of the tree
		AATConditionalJump whileTestJump = new AATConditionalJump(test,
				WhileStartLabel.label());
		AATSequential a = new AATSequential(whileTestLabel, whileTestJump);
		AATSequential b = new AATSequential(dowhilebody, a);
		AATSequential c = new AATSequential(WhileStartLabel, b);

		return c;
	}

	public AATStatement callStatement(Vector actuals, Label name)
	{
		return new AATCallStatement(name, actuals);
	}

	public AATStatement sequentialStatement(AATStatement first,
			AATStatement second)
	{
		AATSequential seq = new AATSequential(first, second);
		return seq;
	}

	public AATStatement returnStatement(AATExpression value, Label functionend)
	{
		Vector<AATStatement> statements = new Vector<AATStatement>();
		statements.add(new AATMove(new AATRegister(Register.Result()), value));
		statements.add(new AATJump(functionend));
		return createSequenceTree(statements);
	}

	public AATStatement functionDefinition(AATStatement body, int framesize,
			Label start, Label end)
	{

		AATLabel functionStartLabel = new AATLabel(start);

		AATLabel functionEndLabel = new AATLabel(end);

		// registers
		AATRegister FP = new AATRegister(Register.FP());
		AATRegister SP = new AATRegister(Register.SP());
		AATRegister ReturnAddr = new AATRegister(Register.ReturnAddr());
		AATRegister temp1 = new AATRegister(Register.Tmp1());

		// calculate the addresses to save FP and ReturnAddr
		AATExpression savedFramePointerAddr = new AATOperator(SP,
				new AATConstant(framesize), AATOperator.MINUS);
		AATExpression savedStackPointerAddr = new AATOperator(SP,
				new AATConstant(framesize + MachineDependent.WORDSIZE),
				AATOperator.MINUS);
		AATExpression savedReturnAddressAddr = new AATOperator(SP,
				new AATConstant(framesize + (MachineDependent.WORDSIZE * 2)),
				AATOperator.MINUS);

		// calculate the new SP address
		AATExpression newSPAddr = new AATOperator(SP, new AATConstant(framesize
				+ (MachineDependent.WORDSIZE * 3)), AATOperator.MINUS);

		Vector<AATStatement> functionStatements = new Vector<AATStatement>();

		functionStatements.add(functionStartLabel); // start label

		/* activation record setup */
		functionStatements.add(new AATMove(
				new AATMemory(savedFramePointerAddr), FP)); // save the old FP
		functionStatements.add(new AATMove(
				new AATMemory(savedStackPointerAddr), SP)); // save the old SP
		
		// save the return address
		functionStatements.add(new AATMove(
				new AATMemory(savedReturnAddressAddr), ReturnAddr)); 

		// move the FP the old SP (which is pointing to the start of the new
		// activation record)
		functionStatements.add(new AATMove(FP, SP));
		
		// Move the SP to the end of the new activation record
		functionStatements.add(new AATMove(SP, newSPAddr)); 

		/* function body */
		functionStatements.add(body);

		/* activation record cleanup */
		// SP is currently pointing at end of the activation record
		savedReturnAddressAddr = new AATOperator(SP, new AATConstant(
				MachineDependent.WORDSIZE), AATOperator.PLUS);
		savedStackPointerAddr = new AATOperator(SP, new AATConstant(
				MachineDependent.WORDSIZE * 2), AATOperator.PLUS);
		savedFramePointerAddr = new AATOperator(SP, new AATConstant(
				MachineDependent.WORDSIZE * 3), AATOperator.PLUS);

		functionStatements.add(functionEndLabel);

		functionStatements.add(new AATMove(ReturnAddr, new AATMemory(
				savedReturnAddressAddr)));
		functionStatements.add(new AATMove(FP, new AATMemory(
				savedFramePointerAddr)));
		functionStatements.add(new AATMove(SP, new AATMemory(
				savedStackPointerAddr)));

		/* function return */
		functionStatements.add(new AATReturn());

		return createSequenceTree(functionStatements);
	}

	/*
	 * 		|		Expressions		|
	 * 		V				  	 	V
	 */

	public AATExpression baseVariable(int offset)
	{
		return new AATMemory(new AATOperator(new AATRegister(Register.FP()),
				new AATConstant(offset), AATOperator.MINUS));
	}

	public AATExpression constantExpression(int value)
	{
		return new AATConstant(value);
	}

	public AATExpression operatorExpression(AATExpression left,
			AATExpression right, int operator)
	{

		AATOperator aatOperator = new AATOperator(left, right, operator);

		return aatOperator;
	}

	public AATExpression callExpression(Vector actuals, Label name)
	{
		return new AATCallExpression(name, actuals);
	}

	public AATExpression classVariable(AATExpression base, int offset)
	{
		return new AATMemory(new AATOperator(base, new AATConstant(offset),
				AATOperator.MINUS));
	}

	public AATExpression arrayVariable(AATExpression base, AATExpression index,
			int elementSize)
	{

		AATExpression offsetInArray = new AATOperator(index, new AATConstant(-1
				* elementSize), AATOperator.MULTIPLY);
		AATOperator finalOffset = new AATOperator(base, offsetInArray,
				AATOperator.PLUS);

		AATMemory address = new AATMemory(finalOffset);

		return address;
	}

	public AATExpression allocate(AATExpression size)
	{
		Vector<AATExpression> actuals = new Vector<AATExpression>();
		actuals.add(size);
		return new AATCallExpression(Label.AbsLabel("allocate"), actuals);
	}

	// helper function to create sequence trees
	// assumes there are at least 2 statements, otherwise you can't make a
	// sequence tree
	public AATStatement createSequenceTree(Vector<AATStatement> statements)
	{
		if (statements.size() == 0)
		{
			return emptyStatement();
		} else if (statements.size() == 1)
		{
			return statements.get(0);
		} else
		{

			AATStatement cur = statements.get(statements.size() - 1);

			for (int i = statements.size() - 2; i >= 0; i--)
			{
				cur = new AATSequential(statements.get(i), cur);
			}
			return cur;
		}
	}

}
