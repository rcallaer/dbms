
// TODO: Auto-generated Javadoc
/**
 * The Class TableAttribute.
 */
public class TableAttribute 
{
	
	/** The domain. */
	private String name, domain;
	
	/** The primary key. */
	private boolean primaryKey;
	
	/**
	 * Instantiates a new table attribute.
	 */
	public TableAttribute()
	{
		name = "";
		domain = "";
		primaryKey = false;
	}
	
	/**
	 * Instantiates a new table attribute.
	 *
	 * @param n the n
	 * @param d the d
	 */
	public TableAttribute(String n, String d)
	{
		name = n;
		domain = d.toLowerCase();
	}
	
	/**
	 * Gets the domain length.
	 *
	 * @return the domain length
	 */
	public int getDomainLength()
	{
		if(domain.equals("text"))
			return 100;
		else if(domain.equals("float"))
			return 8;
		else
			return 4;
	}
	
	/**
	 * Sets the name.
	 *
	 * @param n the new name
	 */
	public void setName(String n)
	{
		name = n;
	}
	
	/**
	 * Sets the domain.
	 *
	 * @param d the new domain
	 */
	public void setDomain(String d)
	{
		domain = d;
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gets the domain.
	 *
	 * @return the domain
	 */
	public String getDomain()
	{
		return domain;
	}
	
	/**
	 * Make primary key.
	 */
	public void makePrimaryKey()
	{
		primaryKey = true;
	}
	
	/**
	 * Make not primary key.
	 */
	public void makeNotPrimaryKey()
	{
		primaryKey = false;
	}
	
	/**
	 * Checks if is primary key.
	 *
	 * @return true, if is primary key
	 */
	public boolean isPrimaryKey()
	{
		return primaryKey;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String returnString;
		
		if(primaryKey)
			returnString = String.format("%-20s %-10s %-15s %-12s", name, domain, getDomainLength(), "primary key");
		else
			returnString = String.format("%-20s %-10s %-5s", name, domain, getDomainLength());

		return returnString;
	}
}
