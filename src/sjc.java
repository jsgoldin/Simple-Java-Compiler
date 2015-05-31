import java.io.File;

public class sjc
{

	public static void main(String args[])
	{
		simplejava parser;
		if (args.length < 1)
		{
			System.out.print("Usage: java sjc <filename>");
			return;
		}
		try
		{
			parser = new simplejava(new java.io.FileInputStream(args[0]));
		} catch (java.io.FileNotFoundException e)
		{
			System.out.println("File " + args[0] + " not found.");
			return;
		}
		try
		{

			String fileName = new File(args[0]).getName();

            String outputPath = "";
            if (args.length >= 2) {
                outputPath = args[1] + "/" + fileName + ".s";
            } else {
                outputPath =  fileName + ".s";
            }

			ASTProgram prog = parser.program();
			SemanticAnalyzer sa = new SemanticAnalyzer();

			CodeGenerator cg = new CodeGenerator(outputPath);
			AATStatement assem = (AATStatement) prog.Accept(sa);
			if (!CompError.anyErrors())
			{
				assem.Accept(cg);
				cg.GenerateLibrary();
			}
		} catch (ParseException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Parsing Failed");
		}
	}

}
