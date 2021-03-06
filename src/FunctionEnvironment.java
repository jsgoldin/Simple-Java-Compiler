import java.util.Vector;

public class FunctionEnvironment
{

	static final int TABLESIZE = 503;

	public FunctionEnvironment()
	{
		htable = new HashTable(TABLESIZE);
	}

	public FunctionEntry find(String key)
	{
		return (FunctionEntry) htable.find(key);
	}

	public int size()
	{
		return htable.numelements();
	}

	public void addBuiltinFunctions()
	{
		Vector formals;
		formals = new Vector(0);
		htable.insert("Read", new FunctionEntry(IntegerType.instance(),
				formals, Label.AbsLabel("Read"), Label.AbsLabel("Readend")));
		htable.insert(
				"Println",
				new FunctionEntry(VoidType.instance(), formals, Label
						.AbsLabel("Println"), Label.AbsLabel("Printlnend")));

		formals = new Vector(1);
		formals.addElement(IntegerType.instance());
		htable.insert("Print", new FunctionEntry(VoidType.instance(), formals,
				Label.AbsLabel("Print"), Label.AbsLabel("Printend")));
	}

	public void insert(String key, FunctionEntry entry)
	{
		htable.insert(key, entry);
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
	public void printFuncEnv()
	{
		System.out.println("<-- PRINTING FUNC ENV -->");
		htable.printHashTable();
		System.out.println("<-- DONE PRINTING FUNC ENV -->");
	}

}
