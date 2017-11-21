
public class SetCondition 
{
	private String identifier;
	private String constant;
	
	public SetCondition()
	{
		identifier = "";
		constant = "";
	}
	
	public SetCondition(String id, String ci)
	{
		identifier = id;
		constant = ci;
	}
	
	public void setIdentifier(String id)
	{
		identifier = id;
	}
	
	public void setConstant(String ci)
	{
		constant = ci;
	}
	
	public String getIdentifier()
	{
		return identifier;
	}
	
	public String getConstant()
	{
		return constant;
	}
}
