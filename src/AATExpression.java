public abstract class AATExpression
{

	public abstract Object Accept(AATVisitor V);

	public String sJavaString = "simple Java not set";

	public String getSJava()
	{
		return sJavaString;
	}

}
