/* Represents a variable. 
 *       Keeps track of the variables type and offset.
 *       Not sure what offset is for.
 *
 *       The type points to a type in the type environment.
 */
class VariableEntry
{

	public VariableEntry(Type type)
	{
		type_ = type;
	}

	public VariableEntry(Type type, int offset)
	{
		type_ = type;
		offset_ = offset;
	}

	public Type type()
	{
		return type_;
	}

	public int offset()
	{
		return offset_;
	}

	public void settype(Type type)
	{
		type_ = type;
	}

	public void setoffset(int offset)
	{
		offset_ = offset;
	}

	private Type type_;
	private int offset_;

	/* Debug methods added by Jordan */

	public String toString()
	{
		return "<VariableEntry : { type:" + type_ + "}>";
	}

	public String getContents()
	{
		String temp = "\t\t<type:" + type_ + ">";
		return temp;
	}
}
