import java.util.ArrayList;


public class AggregateCondition 
{
	private String type;
	private String identifier;
	
	public AggregateCondition()
	{
		type = "";
		identifier = "";
	}
	
	public AggregateCondition(String t, String id)
	{
		type = t;
		identifier = id;
	}
	
	public void setIdentifier(String id)
	{
		identifier = id;
	}
	
	public void setOperator(String t)
	{
		type = t;
	}
	
	public String getIdentifier()
	{
		return identifier;
	}
	
	public String getOperator()
	{
		return type;
	}
	
	public double aggrOperationInteger(String type, ArrayList<Integer> values)
	{
		if (type.toLowerCase().equals("count")) {
			return values.size();
		}
		else if (type.toLowerCase().equals("min")) {
			int minvalue=values.get(0);
			for (int value : values) {
				if (value < minvalue)
					minvalue = value;
			}
			return minvalue;
		}
		else if (type.toLowerCase().equals("max")) {
			int maxvalue=values.get(0);
			for (int value : values) {
				if (value > maxvalue)
					maxvalue = value;
			}
			return maxvalue;
		}
		else if(type.toLowerCase().equals("sum"))
		{
			int sum = 0;
			
			for( int value : values)
			{
				sum += value;
			}
			
			return sum;
		}
		else if(type.toLowerCase().equals("average"))
		{
			double sum = 0;
			
			for( int value : values)
			{
				sum += value;
			}
			return sum/values.size();
		}
		return 0;
	}
	
	public double aggrOperationDouble(String type, ArrayList<Double> values)
	{
		if (type.toLowerCase().equals("count")) {
			return values.size();
		}
		else if (type.toLowerCase().equals("min")) {
			double minvalue=values.get(0);
			for (double value : values) {
				if (value < minvalue)
					minvalue = value;
			}
			return minvalue;
		}
		else if (type.toLowerCase().equals("max")) {
			double maxvalue=values.get(0);
			for (double value : values) {
				if (value > maxvalue)
					maxvalue = value;
			}
			return maxvalue;
		}
		else if(type.toLowerCase().equals("sum"))
		{
			double sum = 0;
			
			for(double value : values)
			{
				sum += value;
			}
			
			return sum;
		}
		else if(type.toLowerCase().equals("average"))
		{
			double sum = 0;
			
			for( double value : values)
			{
				sum += value;
			}
			return sum/values.size();
		}
		return 0;
	}
	
	public int aggrOperationText(ArrayList<String> values)
	{
		return values.size();
	}
}
