public class StringType extends Type
{

	private StringType()
	{

	}

	public static StringType instance()
	{
		if (_instance == null)
		{
			_instance = new StringType();
		}
		return _instance;
	}

	static private StringType _instance;

}
