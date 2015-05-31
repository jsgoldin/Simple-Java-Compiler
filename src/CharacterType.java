public class CharacterType extends Type
{

	private CharacterType()
	{

	}

	public static CharacterType instance()
	{
		if (_instance == null)
		{
			_instance = new CharacterType();
		}
		return _instance;
	}

	static private CharacterType _instance;

}
