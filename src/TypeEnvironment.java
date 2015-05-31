/*
 Maps the type (as a string) to an instance of Type
 */
public class TypeEnvironment
{

	static final int TABLESIZE = 503;

	public TypeEnvironment()
	{
		htable = new HashTable(TABLESIZE);

		/* add built in types to the type environment */
		htable.insert("int", IntegerType.instance());
		htable.insert("boolean", BooleanType.instance());
		htable.insert("void", VoidType.instance());
		htable.insert("char", CharacterType.instance());
	}

	public Type find(String key)
	{
		return (Type) htable.find(key);
	}

	public int size()
	{
		return htable.numelements();
	}

	public void insert(String key, Type type)
	{
		htable.insert(key, type);
	}

	public void beginScope()
	{
		htable.beginScope();
	}

	public void endScope()
	{
		htable.endScope();
	}

	private HashTable htable;

	/* Debug method added by Jordan */
	public void printTypeEnv()
	{
		System.out.println("<-- PRINTING TYPE ENV -->");
		htable.printHashTable();
		System.out.println("<-- DONE PRINTING TYPE ENV -->");
	}

}
