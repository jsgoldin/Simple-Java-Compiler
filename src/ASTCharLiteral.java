class ASTCharLiteral extends ASTExpression
{

	public ASTCharLiteral(char value, int line)
	{
		value_ = value;
		line_ = line;
	}

	public char value()
	{
		return value_;
	}

	public int line()
	{
		return line_;
	}

	public void setline(int line)
	{
		line_ = line;
	}

	public void setvalue(char value)
	{
		value_ = value;
	}

	public Object Accept(ASTVisitor V)
	{
		return V.VisitCharLiteral(this);
	}

	private char value_;
	private int line_;
}
