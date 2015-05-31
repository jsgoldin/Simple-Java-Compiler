/* The Variable environment maps the String name of a variable to its VariableEntry. 
   A VariableEntry contains a pointer to a type in the type environment and offset of the variable.
 */
public class VariableEnvironment
{

	static final int TABLESIZE = 503;

	public VariableEnvironment()
	{
		htable = new HashTable(TABLESIZE);
	}

	public VariableEntry find(String key)
	{
		return (VariableEntry) htable.find(key);
	}

	public void insert(String key, VariableEntry entry)
	{
		htable.insert(key, entry);
	}

	public int size()
	{
		return htable.numelements();
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
	public void printVarEnv()
	{
		System.out.println("<-- PRINTING VAR ENV -->");
		htable.printHashTable();
		System.out.println("<-- DONE PRINTING VAR ENV -->");
	}
}
