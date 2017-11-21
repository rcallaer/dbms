/**************************************************
 * A simple Exception class that handles incorrect 
 * syntax by printing an error message or outputting 
 * to the appropriate file.
 *************************************************/

import java.io.PrintWriter;

public class SyntaxException extends Exception
{
	private static final long serialVersionUID = 1L;

	public SyntaxException()
	{
		super("ERROR - FOUND \"\", EXPECTING \"command\"");
	}
	
	public SyntaxException(String line, String found, String expecting, PrintWriter pw, boolean print)
	{
		//super("> "+ lineERROR - FOUND \"" + found + "\", EXPECTING \"" + expecting + "\"");
		super("> "+ line + "\nERROR - FOUND \"" + found + "\", EXPECTING \"" + expecting + "\"");
		
		if (print)
		{
			pw.println(">"+line);
			pw.println("ERROR - FOUND \"" + found + "\", EXPECTING \"" + expecting + "\"");
		}
	}
}
