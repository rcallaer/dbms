
public class WhereCondition 
{
	private String identifier;
	private String operator;
	private String constant_or_identifier;
	
	public WhereCondition()
	{
		identifier = "";
		operator = "";
		constant_or_identifier = "";
	}
	
	public WhereCondition(String id, String op, String ci)
	{
		identifier = id;
		operator = op;
		constant_or_identifier = ci;
	}
	
	public void setIdentifier(String id)
	{
		identifier = id;
	}
	
	public void setOperator(String op)
	{
		operator = op;
	}
	
	public void setConstant(String ci)
	{
		constant_or_identifier = ci;
	}
	
	public String getIdentifier()
	{
		return identifier;
	}
	
	public String getOperator()
	{
		return operator;
	}
	
	public String getConstant()
	{
		return constant_or_identifier;
	}
	
	public boolean rightHalfIsIdentifier()
	{
		return check(constant_or_identifier);
	}
	
	//checks for a valid identifier
	private boolean check(String nm)
	{
		if (nm.toUpperCase().equals("FROM") 
			|| nm.toUpperCase().equals("WHERE")
			|| nm.toUpperCase().equals("SUM")
			|| nm.toUpperCase().equals("AVERAGE")
			|| nm.toUpperCase().equals("MIN")
			|| nm.toUpperCase().equals("MAX")
			|| nm.toUpperCase().equals("COUNT")
			|| nm.toUpperCase().equals("AND")
			|| nm.toUpperCase().equals("OR")
			|| nm.toUpperCase().equals("SET")
			|| nm.toUpperCase().equals("OUTPUT")
			|| nm.toUpperCase().equals("INTO")
			|| nm.toUpperCase().equals("KEY")
			|| nm.toUpperCase().equals("PRIMARY")
			|| nm.toUpperCase().equals("TABLE")
			|| nm.toUpperCase().equals("INPUT")
			|| nm.toUpperCase().equals("SELECT")
			|| nm.toUpperCase().equals("CREATE")
			|| nm.toUpperCase().equals("RENAME")
			|| nm.toUpperCase().equals("UPDATE")
			|| nm.toUpperCase().equals("INSERT")
			|| nm.toUpperCase().equals("DELETE")
			|| nm.toUpperCase().equals("DESCRIBE")
			|| nm.toUpperCase().equals("LET")
			|| nm.toUpperCase().equals("AGGREGATE")
			|| nm.toUpperCase().equals("DATABASE")
			|| nm.toUpperCase().equals("VALUES")
			|| nm.toUpperCase().equals("INTEGER")
			|| nm.toUpperCase().equals("TEXT")
			|| nm.toUpperCase().equals("FLOAT"))
			return false;
		
		boolean ch = false;
		//checks to see if the passed is empty
		//if(nm.isEmpty())
		//	ch = false;
		//else it contains something
		//else
		//{
			int hld = (int)nm.charAt(0);

			//checks to see if the token starts with numeric
			if(hld >= 48 && hld <= 57)
				ch = false;
			//if does not start with a numeric
			else
			{
				//checks to make sure the length is correct
				if(nm.length() > 20)
					ch = false;
				else
				{
					//else it iterates through looking for any invalid characters
					for(int i = 0; i < nm.length(); i++)
					{
						hld = (int)nm.charAt(i);
						if((hld >= 65 && hld <= 90) || (hld >= 97 && hld <= 122) || (hld >= 48 && hld <= 57))
							ch = true;
						else
							{ch = false; i = nm.length() + 1;}
					}
				}
			}
		//}
		//returns a boolean true or false based on the above
		return ch;
	}
}
