
import java.io.PrintWriter;


public class OtherException extends Exception
{
	public OtherException()
	{
		super("ERROR - FOUND \"\", EXPECTING \"command\"");
	}
	
	public OtherException(String message, PrintWriter pw, boolean print)
	{
		super("ERROR - " + message);

		if (print)
			pw.println("ERROR - " + message);
	}
}