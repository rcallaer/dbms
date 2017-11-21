/****************************************************************
 * Ryan Allaer, Christopher Bowman, Raden Tonev, Zachary Riegger
 * COSC471 Winter 2011
 * Professor Ikeji
 * DDMS Project Part 1 (Syntax Parser)
 * 
 * A basic syntax parsing program that will eventually be used
 * to implement a full DBMS.
 ****************************************************************/
import java.util.*;
import java.io.*;

public class DBMSMain
{
	private static StringTokenizer st;
	private static PrintWriter prwtr;
	private static String wholeLine;
	private static File directory = new File(System.getProperty("user.dir")+"/");
	private static File currentDB = new File(System.getProperty("user.dir")+"/");
	private static ArrayList<String> attributeNames, tableNames; 
	private static ArrayList<ArrayList<String>> values;
	private static ArrayList<SetCondition> sets;
	private static ArrayList<AggregateCondition> aggregates;
	private static ArrayList<File> tableFiles;
	private static ArrayList<TableAttribute> tableAttributes, joinedTableAttributes;
	private static ArrayList<WhereCondition> whereConditions;
	
	public static void main(String args[]) throws IOException
	{
		//initializes a scanner
		Scanner scan = new Scanner(System.in);

		String hld = "";//will hold the command
		String hld2 = "";//will hold additional lines
		boolean ch = true;//control variable
		StringTokenizer tok;
		int numComm;//control 
		tableFiles = new ArrayList<File>();
		
		//do while loop to loop till exit is entered
		do
		{
			System.out.print("--> ");
			hld = scan.nextLine();
			
			//loops till a ; is input
			while(!hld.contains(";"))
			{
				System.out.print("--> ");
				hld2 = scan.nextLine();
				if(hld2.equals(";"))
					hld = hld + hld2;
				else if(hld2.equals(" ") || hld2.equals("") || hld2.equals("\t") || hld2.equals("\n") || hld2.equals("\r"))
					;
				else
					hld = hld + " " + hld2;
			}
		
			//tokenizes the input list with ; as delimeter
			tok = new StringTokenizer(hld,";");
			//loops through all the tokens
			while(tok.hasMoreTokens())
			{
				hld = tok.nextToken() + ";";
				if(hld.replaceAll("\\s+", "").equals(";"))
					break;
				hld2 = hld;
				
				//replace certain expressions for easier handling
				hld = hld.replaceAll("\\(", " ( ");
				hld = hld.replaceAll("\\)", " ) ");
				hld = hld.replaceAll("\\s*;", ";");
				
				//set the global line
				wholeLine = hld;
				
				st = new StringTokenizer(hld);
				
				//set the first token to upper case to negate case sensitivity 
				//holds the command value
				hld = st.nextToken();
				
				if(hld.endsWith(";"))
					hld = hld.substring(0, hld.indexOf(";"));
				numComm = getCom(hld.toUpperCase());//calls a function to assign the command a value
				
				//send the command its appropriate function
				redirect(hld2, numComm, false);
			}
		}
		while(ch == true);	
		System.exit(0);
	}
	
	//a method to handle comments
	private static void comment(String hld, boolean f)
	{
		if(hld.contains(";"))
		{
			String nm = remSemiColon(hld);
			
			if(nm.contains("/*") && nm.endsWith("*/"))
				printMess("",13,f);
			else
				printMess(nm,15,f);
		}
		else
			printMess(hld,10,f);
	}
	
	//a method for the use command of the dbms
	private static void use(boolean f) throws SyntaxException, OtherException
	{
		//if the statment is only one character long error
		if(st.countTokens() == 0)
			printMess(";",1,f);
		//if it is the right amount of characters
		else if(st.countTokens() == 1)
		{
			String nm = st.nextToken();
			//checks to see if it terminates
			if(nm.contains(";"))
			{
				nm = remSemiColon(nm);//removes semicolon
				if(check(nm) == true)
				{
					//make sure that the database directory exists
					File checkDB = new File(directory.getAbsolutePath()+"/"+nm+"/");
					
					if(!checkDB.isDirectory())
						throw new OtherException("DATABASE DOES NOT EXIST", prwtr, f);
					//if so, set the current database variable
					else if(checkDB.exists())
						currentDB = checkDB;
					//otherwise print an error
					else
						throw new OtherException("DATABASE DOES NOT EXIST", prwtr, f);
					printMess(" ",13,f);//correct syntax
				}
				else
					printMess(nm,1,f);
			}
			//prints error message if not terminal
			else
				printMess(nm,10,f);
		}
		//not the right amount of characters
		else
			error("INVALID STATEMENT",f);
	}
	
	private static void create(String tmp, boolean f) throws SyntaxException, OtherException, IOException
	{
		if(st.hasMoreTokens() == false)
			printMess(";",12,f);
		else
		{
			//grabs the next token
			String nextComm = st.nextToken();
			
			if(nextComm.endsWith(";"))
				nextComm = nextComm.substring(0, nextComm.indexOf(";"));
			//checks to see if the token is TABLE
			if((nextComm.toUpperCase()).equals("TABLE"))
				table(tmp, f);
			//checks to see if the token is DATABASE
			else if ((nextComm.toUpperCase()).equals("DATABASE"))
				database(f);
			//error message is given if there is anything other than those two commands
			else
			{
				printMess(nextComm, 12, f);
			}
		}
	}
	
	private static void table(String tmp, boolean f) throws SyntaxException, OtherException, IOException
	{
		if(!st.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
		String nm = st.nextToken();//grabs the next token
		StringTokenizer tok2;//sets up a new tokenizer
		StringTokenizer tok3;//sets up a new tokenizer
		boolean ch = true;//control variable
		boolean first = true;
		String nm2 = "";
		//checks to see the if it is an identifier
		if(check(nm) == true)
		{
			if(tmp.contains("(") && tmp.contains(")"))
			{
				tableAttributeList(nm, f);
			}
			else
				printMess(tmp,3,f);
		}
		else
			printMess(nm,1,f);
	}
	
	private static void tableAttributeList(String currentToken, boolean f) throws SyntaxException, OtherException, IOException
	{
		StringTokenizer attrListTokens;
		final String tableName = currentToken;
		tableAttributes = new ArrayList<TableAttribute>();
		
		String attributeList = "";
		boolean valid = false;
		int control = 0;

		//construct the attribute list from the rest of the line
		while (st.hasMoreTokens())
		{
			attributeList = attributeList + st.nextToken() + " ";
		}

		//create a tokenizer for the list
		attributeList = attributeList.replaceAll("\\s*,", ", ");
		attrListTokens = new StringTokenizer(attributeList.replaceAll("\\s+", " "), " (),;", true);
		
		//read in first value as ( and ensure validity
		if (!attrListTokens.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
		currentToken = attrListTokens.nextToken();
		if (!currentToken.equals("("))
			throw new SyntaxException(wholeLine, currentToken, "(", prwtr, f);
		
		//loop through the rest of the list
		while(attrListTokens.hasMoreTokens())
		{
			TableAttribute ta = new TableAttribute();
			
			//ignore assumed space
			currentToken = attrListTokens.nextToken();
			
			//check identifier validity
			if (!attrListTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
			currentToken = attrListTokens.nextToken();
			if (!check(currentToken))
				throw new SyntaxException(wholeLine, currentToken, "identifier", prwtr, f);
			
			ta.setName(currentToken);
			
			//ignore assumed space
			currentToken = attrListTokens.nextToken();
			
			//check domain validity
			if (!attrListTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "domain", prwtr, f);
			if (!currentToken.equals(" "))
				throw new SyntaxException(wholeLine, currentToken, "domain", prwtr, f);
			currentToken = attrListTokens.nextToken();
			if (!currentToken.toUpperCase().equals("INTEGER") 
				&& !currentToken.toUpperCase().equals("FLOAT") 
				&& !currentToken.toUpperCase().equals("TEXT"))
				throw new SyntaxException(wholeLine, currentToken, "domain", prwtr, f);
			
			ta.setDomain(currentToken);
			
			//retrieve the PRIMARY, comma, or ) token
			if (!attrListTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "PRIMARY, comma, or )", prwtr, f);
			currentToken = attrListTokens.nextToken();
			
			//checks for primary without consuming the main tokenizer
			StringTokenizer arg = new StringTokenizer(wholeLine.substring(wholeLine.indexOf(" ( ")));
			boolean primary = false;
			String dummy = "";
			for(int i = 0; i < 4;i++)
				dummy = arg.nextToken();

			//check for PRIMARY only on the first iteration 
			if (control == 0 && dummy.toUpperCase().equals("PRIMARY"))
				primary = true;
			
			//if there's a primary run through it's pattern
			if (primary)
			{
				//get the token
				currentToken = attrListTokens.nextToken();
				
				//check for primary
				if(currentToken.toUpperCase().equals("PRIMARY"))
				{
					//ignore space
					attrListTokens.nextToken();
					
					//get KEY token
					if (!attrListTokens.hasMoreTokens())
						throw new SyntaxException(wholeLine, ";", "KEY", prwtr, f);
					currentToken = attrListTokens.nextToken();
					
					//ensure the next token is KEY
					if(!currentToken.toUpperCase().equals("KEY"))
						throw new SyntaxException(wholeLine, currentToken, "KEY", prwtr, f);
					
					ta.makePrimaryKey();
					
					//get next token
					currentToken = attrListTokens.nextToken();
				}
			}
			
			//if the token is space check for an ending ) and ;
			if(currentToken.equals(" "))
			{
				if (!attrListTokens.hasMoreTokens())
					throw new SyntaxException(wholeLine, ";", ")", prwtr, f);
				currentToken = attrListTokens.nextToken();
				
				if (currentToken.equals(")"))
				{
					currentToken = attrListTokens.nextToken();
					
					if (currentToken.equals(";"))
					{
						tableAttributes.add(ta);
						valid = true;
						break;
					}
					else
						throw new SyntaxException(wholeLine, currentToken, ";", prwtr, f);
				}
				else
					throw new SyntaxException(wholeLine, currentToken, "comma or )", prwtr, f);
			}
			//if the token is a comma loop around
			else if(currentToken.equals(","))
			{
				tableAttributes.add(ta);
				control++;
				continue;
			}
			else
				throw new SyntaxException(wholeLine, currentToken, "comma or )", prwtr, f);
		}
		
		if (valid && currentToken.equals(";"))
		{
			
			 HashSet<String> set = new HashSet<String>();
			 for (int i = 0; i < tableAttributes.size(); i++) 
			 {
				 boolean val = set.add(tableAttributes.get(i).getName());
				 if (val == false) 
				 {
					 throw new OtherException("Table attributes cannot have the same name.", prwtr,f);
				 }
			 }

			
			createTable(tableName, f);
			printMess("", 13, f);
		}
	}
	
	public static void createTable(String tableName, boolean f) throws OtherException, IOException
	{
		//create the binary data file for the table
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".dat");
		boolean success = dataFile.createNewFile();
		
		//if the current database variable hasn't been altered, no database is in use
		if(currentDB.getAbsolutePath().equals(directory.getAbsolutePath()))
			throw new OtherException("NO DATABASE IN USE", prwtr, f);
		//if the file couldn't be created, table already exists
		else if (!success)
			throw new OtherException("TABLE ALREADY EXISTS", prwtr, f);
		else
		{
			//create the index file, metadata file and binary search tree
			File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".mtd");
			File indexFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".itf");
			ObjectOutputStream index = new ObjectOutputStream(new FileOutputStream(indexFile));
			PrintWriter metaWriter = new PrintWriter(metaFile);
			BinarySearchTree indexTree = new BinarySearchTree();
			
			//write all table attributes to the metadata file
			int recordSize = 1; //start at 1 for the deletion byte
			//int recordSize = 0;
			for(TableAttribute ta : tableAttributes)
			{
				recordSize+=ta.getDomainLength();
			}
			
			metaWriter.println(recordSize);
			metaWriter.println("Attributes           Types      Length(in Bytes)");
			for(TableAttribute ta : tableAttributes)
			{
				metaWriter.println(ta);
			}
			
			//dump the blank binary search tree to the index file
			index.writeObject(indexTree);
			
			//tableFiles.add(dataFile);
			indexFile.setReadable(true);
			indexFile.setWritable(true);
			metaWriter.close();
			index.close();
		}
	}
	
	private static void database(boolean f) throws SyntaxException, OtherException
	{
		if (!st.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
		//grabs the next token
		String nm = st.nextToken();
		boolean ch;
		//checks to make sure it is an identifier
		if(nm.contains(";"))
			ch = check(nm.substring(0, nm.length()-1));
		else
			ch = check(nm);
		//if it passes as an identifier
		if(ch == true)
		{
			//checks to see if it ends with a ; or not
			if(nm.contains(";"))
			{
				//remove ending semicolon
				nm = nm.substring(0, nm.length()-1);

				//get the top level directory, attach the new database name,
				//and create the new directory
				String dir = directory.getAbsolutePath()+"/"+nm+"/";
				boolean success = (new File(dir)).mkdir();
				
				//print an error if file couldn't be created
				if(!success)
					throw new OtherException("DATABASE ALREADY EXISTS", prwtr, f);
				
				//otherwise print OK
				printMess("",13,f);//correct syntax
			}
			//sends an error that says it needs to end with a ;
			else
				printMess(nm,10,f);
		
		}
		//gives an error if an identifier is not found
		else
			printMess(nm,1,f);
	}
	
	//check if token is from or where
	private static boolean chFromWhere(String ch)
	{
		ch = ch.toUpperCase();
		if(ch.equals("FROM"))
			return false;
		else if(ch.equals("WHERE"))
			return false;
		else
			return true;
	}
	
	private static void select(boolean f, boolean let) throws SyntaxException, OtherException, IOException, ClassNotFoundException
	{	
		if((st.hasMoreTokens())==true)
		{
			String nm = st.nextToken();
			String tmp;
			StringTokenizer tok1;
			tok1 = new StringTokenizer(nm,",");
			tmp=tok1.nextToken();
			attributeNames = new ArrayList<String>();
			tableNames = new ArrayList<String>();
			
			//identifier validity
			if (!check(tmp) && !tmp.equals("*")) {
				if(tmp.endsWith(";"))
					throw new SyntaxException(wholeLine, ";", "FROM", prwtr, f);
				throw new SyntaxException(wholeLine, tmp, "identifier", prwtr, f);
			}
			boolean ch = true;
			
			//checks the list of identifiers
			while(chFromWhere(nm) == true && ch == true)
			{
				if(nm.contains(","))
				{
					StringTokenizer st5 = new StringTokenizer(nm,",");
					while(st5.hasMoreTokens() && ch == true)
					{
						nm = st5.nextToken();
						if(check(nm) == true)
						{
							attributeNames.add(nm);
							ch = true;
						}
						else
							ch = false;
					}
				}
			
				else
				{
					if(check(nm) == true || nm.equals("*"))
					{
						attributeNames.add(nm);
						ch = true;
					}
					else
						ch = false;
				}
			
				if (!st.hasMoreTokens())
					//printMess(";",11,f);
					throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
				else
					if(ch == true)
						nm = st.nextToken();
			}
		
			if(ch == true)
			{
				StringTokenizer st5;
				nm = nm.toUpperCase();
				boolean endFunc = false;
				if(nm.equals("FROM")) { 
					
				  if (!st.hasMoreTokens())
					throw new SyntaxException(wholeLine, ";","identifier",prwtr,f );
				
					nm = st.nextToken();
					//loop through tokens till WHERE or something else
					while(chFromWhere(nm) == true && ch == true)
					{
						//has a terminal in it
						if(nm.contains(";"))
						{
							//is a list
							if(nm.contains(","))
							{
							
								st5 = new StringTokenizer(nm,",");
								boolean cntrl = false;
								//loops through till the end
								while(st5.hasMoreTokens() && cntrl == false)
								{
									nm = st5.nextToken();
								
									//if it is the terminal
									if(nm.contains(";"))
									{
										nm = remSemiColon(nm);
										//checks to see if it is an identifier
										if(check(nm) == true)
										{
											tableNames.add(nm);
											
											
											//if(let)
											//{
												/*
												 * HAVE TO ADD LET BOOLEAN TO JOIN
												 */
											//	join("", true, f);
												//selectFromFile(false);
											//}
											if(!let)
											{
												join("", true, f);
												//printMess("",13,f);
											}
										}
										else
										{
											printMess(nm,1,f);
											cntrl = true;
										}
									}
									else
									{
										if(check(nm) == true)
										{
											tableNames.add(nm);
											cntrl = false;
										}
										else
										{
											printMess(nm,1,f);
											cntrl = true;
										}
									}
								}
								
								ch = false;
							}
							//else it is not a list
							else
							{
								//removes the semicolon
								nm = remSemiColon(nm);
								if(check(nm) == true)
								{
									tableNames.add(nm);
									
									if(tableNames.size() > 1)
									{
										//if(let)
										//{
										
											//selectFromFile(false);
										//	join("", false,f);
										//}
										if(!let)
										{
											//selectFromFile(true);
											join("",true,f);
										}
									}
									else
									{
										if(let)
										{
										
											selectFromFile(false, f);
											//join("", true);
										}
										else
										{
											selectFromFile(true, f);
											//join("",true);
										}
									}
									//printMess("",13,f);//correct syntax
								}
								else
									printMess(nm,1,f);
								ch = false;
							}
							endFunc = true;
						}
						//is not a terminal
						else
						{
							//is a list
							if(nm.contains(","))
							{
								st5 = new StringTokenizer(nm,",");
								boolean cntrl = false;
								//loops through till the end
								while(st5.hasMoreTokens() && cntrl == false)
								{
									nm = st5.nextToken();
								
									//if it is the terminal
									if(nm.contains(";"))
									{
										nm = remSemiColon(nm);
										//checks to see if it is an identifier
										if(check(nm) == true)
										{
											tableNames.add(nm);
											ch = true;
										}
										else
										{
											printMess(nm,1,f);
											ch = false;
										}
									}
									else
									{
										if(check(nm) == true)
										{
											tableNames.add(nm);
											cntrl = false;
										}
										else
										{
											printMess(nm,1,f);
											ch = false;
										}
									}
								}
							
							}
							//else it is not a list
							else
							{
								//checks to see if it is a identifier
								if(check(nm) == true)
								{
									tableNames.add(nm);
									ch = true;
								}
								else
								{
									printMess(nm,1,f);
									ch = false;
								}
							}
							endFunc = false;
						}
					
						if(ch == true) {
							if (!st.hasMoreTokens())
								throw new SyntaxException(wholeLine, ";","identifier",prwtr,f );		
							nm = st.nextToken();
							if(nm.toUpperCase().equals("WHERE;"))
								throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
						}
					}
				
					if(endFunc == false)
					{
						if(!let)
						{
							if(tableNames.size() > 1)
								join(nm, true,f);
							else
								selectWithWhere(nm, !let, f);
						}
					}
				}  
				else
					printMess(nm,4,f);
			}
			else
				printMess(nm,1,f);
		}
		else
			//printMess(";",12,f);
			throw new SyntaxException(wholeLine, ";","identifier",prwtr,f );		
	}
	
	private static ArrayList<ArrayList<String>> selectFromFile(boolean display, boolean f) throws IOException, OtherException, ClassNotFoundException
	{	
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	
    	//store the size of a record
    	int recordLength = Integer.parseInt(metaInfo.nextLine());
    	metaInfo.close();
    	
    	ArrayList<String> selectedValues = new ArrayList<String>();
    	ArrayList<ArrayList <String>> selectedValuesx2 = new ArrayList<ArrayList<String>>();
    	tableAttributes = new ArrayList<TableAttribute>();
    	tableAttributes = getTableAttributes(tableNames.get(0), f);
    	ArrayList<Integer> attributePositions = new ArrayList<Integer>();
    	
    	//if * was entered, add all attribute positions to be selected
    	if(attributeNames.get(0).equals("*"))
    	{
    		for(int i = 0; i < tableAttributes.size();i++)
    			attributePositions.add(i);
    	}
    	//otherwise get only the selected attribute positions corresponding to the specified attribute names
    	else
    		attributePositions = getAttributePositions(tableNames.get(0), attributeNames, f);
    	
    	//make sure there aren't more entered attribute names than table attributes
    	if(attributeNames.size() > tableAttributes.size())
    	{
    		tableAccess.close(); 
    		throw new OtherException("Too many attributes entered. Table has "+tableAttributes.size()+" attributes.", prwtr, false);
    	}
    	
    	//store the file length and the number of records in the file
    	long dataFileLength = tableAccess.length();
    	long totalFileRecords = dataFileLength/recordLength;
    	
    	int index = 0;
		ArrayList<String> names = new ArrayList<String>();
		
		//loop through the table attributes to store the attribute names
		for(TableAttribute currentAttribute : tableAttributes)
		{
			//store whether the current index corresponds to one of the selected attributes
			boolean selectValue = attributePositions.contains(index);
			
			//only add names from selected attributes
			if(selectValue)
				names.add(currentAttribute.getName());
			
			index++;
		}
		//add the array of selected attribute names as the first row
		selectedValuesx2.add(names);
    	
		//iterate through each record in the file
    	for(int i = 0; i < totalFileRecords; i++)
    	{
    		index = 0; //reset the table attribute index
    		tableAccess.seek(i*recordLength); //seek to the next record
    		
    		selectedValues = new ArrayList<String>();
    		
    		//read the deletion marker
    		char deletionMarker = (char)tableAccess.read();
    			
    		//loop through the table attributes to get the selected attribute values from the file
    		for(TableAttribute currentAttribute : tableAttributes)
    		{		
    			int domainLength = currentAttribute.getDomainLength();
    			
    			//store whether the current index corresponds to one of the selected attributes
    			boolean selectValue = attributePositions.contains(index);
    			String value = "";
    			
    			//current attribute is an int
    			if(domainLength == 4)
    			{
    				//only store a value from an attribute that was selected, and not deleted
    				if(selectValue && deletionMarker != 'D')
        			{
    					int constant = tableAccess.readInt();
    					value = Integer.toString(constant);
        			}
    				//otherwise skip over
    				else
    					tableAccess.readInt();
    			}
    			//current attribute is a float
    			else if(domainLength == 8)
    			{
    				//only store a value from an attribute that was selected, and not deleted
    				if(selectValue && deletionMarker != 'D')
        			{
    					double constant = tableAccess.readDouble();
    					value = Double.toString(constant);
        			}
    				//otherwise skip
    				else
    					tableAccess.readDouble();
    			}
    			//current attribute is text
    			else
    			{
    				//only store a value from an attribute that was selected, and not deleted
    				if(selectValue && deletionMarker != 'D')
        			{				
    					byte [] b = new byte[100];
    					tableAccess.read(b);
    					
    					String newValue = new String(b);
    					
    					newValue = newValue.replaceAll("\b", "");
    					
    					value = newValue;
    					
        			}
    				//otherwise skip
    				else
    				{
    					byte [] b = new byte[100];
    					tableAccess.read(b);
    				}
    			}
    			
    			//only add values to the array from an attribute that was selected, and not deleted
    			if(selectValue && deletionMarker != 'D')
    				selectedValues.add(value);
    			
    			index++;//increment the attribute index
    		}
    		
    		//add the array of selected attribute values as the next row if it's not deleted
    		if(deletionMarker != 'D')
    			selectedValuesx2.add(selectedValues);
    	}
    	tableAccess.close();
    	
    	//don't print anything if there's a where condition or let query
    	if(display)
    	{
	    	//loop through all selected values and print them
	    	for(ArrayList<String> as : selectedValuesx2)
	    	{
	    		for(String value : as)
	    		{
	    			if(f)
	    				prwtr.printf("%-15s", value);
	    			else
	    				System.out.printf("%-15s", value);
	    		}
	    		if(f)
    				prwtr.println();
    			else
    				System.out.println();
	    	}
    	}
    	
    	return selectedValuesx2;
	}
	
	private static ArrayList<ArrayList<String>> selectWithWhere(String currentToken, boolean display, boolean f) throws IOException, OtherException, ClassNotFoundException, SyntaxException
	{
		ArrayList<ArrayList<String>> selections = selectFromFile(false,f);
		ArrayList<ArrayList<String>> whereSelections = new ArrayList<ArrayList<String>>();
		ArrayList<Integer> positions = checkWhereCondition(currentToken, false);
		
		int index = 0;
		
		//for every record in the selected attributes
		for(ArrayList<String> record : selections)
		{
			//print the record only if it corresponds to one of the positions of records 
			//that satisfy the where condition(s)
			boolean printRecord = index == 0 || positions.contains(index);
			
			ArrayList<String> selectedRecord = new ArrayList<String>();
			
			//for every value in a record
			for(String value : record)
			{
				//the current record is one that satisfied a where condition
				if(printRecord)
				{
					//print and add it to the array list for storage
					if(display)
					{
						if(f)
							prwtr.printf("%-15s", value);
						else
							System.out.printf("%-15s", value);
					}
					selectedRecord.add(value);
				}
			}
			
			//the current record is one that satisfied a where condition
			if(printRecord)
			{
				//print new line and add the array list to the 2D array list for storage
				if(display)
				{
					if(f)
						prwtr.println();
					else
						System.out.println();
				}
				whereSelections.add(selectedRecord);
			}
			index++;
		}
		
		//if(display)
			//printMess("",13,f);
		return whereSelections;
	}
	
	public static ArrayList<ArrayList<String>> join(String currentToken, boolean display, boolean f) throws IOException, OtherException, ClassNotFoundException, SyntaxException
	{
		String joinedTableName = "_temp2";
		ArrayList<String> joinedAttributeNames = attributeNames;
		attributeNames = new ArrayList<String>();
		attributeNames.add("*");
		ArrayList<ArrayList <String>> firstTable = selectFromFile(false, f);
		ArrayList<ArrayList <String>> newTable = new ArrayList<ArrayList <String>>();
		String firstTableName = tableNames.get(0);
		tableNames.remove(0);
		
		ArrayList<String> allTables = tableNames;
		//ArrayList<String> rename;
		
		ArrayList<String> firstTableRenames = firstTable.get(0);
		/*rename = new ArrayList<String>();
		
		for(String name : firstTableRenames)
		{
			rename.add(firstTableName+"_"+name);
		}
		
		firstTableRenames = rename;*/
		
		int index = 0;
		for(String table : allTables)
		{
			tableNames = new ArrayList<String>();
			tableNames.add(table);
			String nextTableName = tableNames.get(0);
			ArrayList<ArrayList <String>> nextTable = selectFromFile(false, f);
			
			ArrayList<String> firstTableNames;
			//if(index == 0)
			//	firstTableNames = firstTableRenames;
			//else
				firstTableNames = firstTable.get(0);
			
			ArrayList<String> nextTableNames = nextTable.get(0);
			
			/*rename = new ArrayList<String>();
				
			for(String name : nextTableNames)
			{
				rename.add(nextTableName+"_"+name);
			}
				
			nextTableNames = rename;*/
			
			firstTableNames.addAll(nextTableNames);
			
			newTable.add(firstTableNames);
			
			for(int i = 1;i<firstTable.size();i++)
			{
				ArrayList<String> tempTable = firstTable.get(i);
				ArrayList<String> copyOfTempTable = (ArrayList<String>)tempTable.clone();
				
				for(int j = 1;j<nextTable.size();j++)
				{
					ArrayList<String> tempTable2 = nextTable.get(j);
					copyOfTempTable = (ArrayList<String>)tempTable.clone();
					copyOfTempTable.addAll(tempTable2);
					
					newTable.add(copyOfTempTable);
				}
			}
			
			firstTable = newTable;
			newTable = new ArrayList<ArrayList<String>>();
			index++;
		}
		
	/*	for(ArrayList<String> record : firstTable)
		{
			for(String value : record)
			{
				System.out.print(value + " ");
			}
			System.out.println();
		}*/
		
		ArrayList<TableAttribute> newTableAttributes = new ArrayList<TableAttribute>();
		ArrayList<String> recordNames = firstTable.get(0);
		ArrayList<String> firstRecords = firstTable.get(1);
		
		for(int i = 0; i < recordNames.size();i++)
		{
			//ArrayList<String> record = firstTable.get(i);
			TableAttribute ta = new TableAttribute();

			ta.setName(recordNames.get(i));
			String constant = checkConstant(firstRecords.get(i));
				
			if(constant.equals("int"))
				constant = "integer";
					
			ta.setDomain(constant);
			newTableAttributes.add(ta);
		}
		
		tableAttributes = joinedTableAttributes = newTableAttributes;
		
		createTable(joinedTableName, false);
		
		firstTable.remove(0);
		
		values = firstTable;
		
		tableNames = new ArrayList<String>();
		
		tableNames.add(joinedTableName);
		
		fileInsert(f);
		
		tableAttributes = getTableAttributes(joinedTableName, f);
		
		attributeNames = joinedAttributeNames;
		/*attributeNames = new ArrayList<String>();
		
		for(TableAttribute attribute : tableAttributes)
		{
			attributeNames.add(attribute.getName());
		}*/
		
		ArrayList<ArrayList<String>> returnValues;
		if(wholeLine.toUpperCase().contains(" WHERE "))
		{
			returnValues = selectWithWhere(currentToken, display, f);
		}
		else
		{
			returnValues = selectFromFile(display, f);
		}
		
		fileDelete(joinedTableName,f);
		
		return returnValues;
	}
	
	private static ArrayList<Integer> getAttributePositions(String tableName, ArrayList<String> attributeNames, boolean f) throws IOException, OtherException
	{
		ArrayList<Integer> positions = new ArrayList<Integer>();
		boolean found = false;
		
		tableAttributes = getTableAttributes(tableName, f);
		
	    for(String identifier : attributeNames)
	    {
	    	int attributeIndex = 0;
	    	found = false;
	    	
		   	for(TableAttribute ta : tableAttributes)
			{		
				if(ta.getName().equals(identifier))
				{
					positions.add(attributeIndex);
					found = true;
				}
				attributeIndex++;
			}
		   	
		   	if(!found)
		   		throw new OtherException("Found: \""+identifier+"\". Expecting: table attribute", prwtr, f);
	   	}
	    Collections.sort(positions);
		return positions;
	}
	
	//returns a list of all table attributes in the specified table
	private static ArrayList<TableAttribute> getTableAttributes(String tableName, boolean f) throws IOException, OtherException
	{
		File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".mtd");
		
		if(!metaFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
		
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	ArrayList<TableAttribute> tas = new ArrayList<TableAttribute>();
    	
    	//skip the first two lines of the meta file
    	metaInfo.nextLine();
    	metaInfo.nextLine();
    	while(metaInfo.hasNextLine())
		{
    		TableAttribute tempTA = new TableAttribute();
			String [] tokens = metaInfo.nextLine().split("\\s+");
			
			//set the name and domain of the attribute
			tempTA.setName(tokens[0]);
			tempTA.setDomain(tokens[1]);
				
			//make it a primary key if it has a primary key token
			if(tokens.length > 3)
				tempTA.makePrimaryKey();
			
			//add the attribute to the array list to be returned
			tas.add(tempTA);
			
		}
    	metaInfo.close();
    	
    	return tas;
	}
	
	private static void describe(boolean f) throws IOException, OtherException
	{
		//checks to see if there are more tokens
		if((st.hasMoreTokens())==true)
		{
			//grabs the next token
			String nm = st.nextToken();
			//checks to make sure that there is a terminal
			if(nm.endsWith(";"))
			{
				//removes that terminal character
				nm = remSemiColon(nm);
				//sees if it says all
				if(nm.equals("ALL") || nm.equals("ALl") || nm.equals("AlL") || nm.equals("All") || nm.equals("aLL") || nm.equals("alL") || nm.equals("aLl") || nm.equals("all"))
				{
					Scanner fileReader;
					
					//get the directory of the current database
					File dir = new File(currentDB.getAbsolutePath());
					
					//store in an array all the files in the directory that end with .mtd
					File[] matches = dir.listFiles(new FilenameFilter()
					{
					  public boolean accept(File dir, String name)
					  {
					     return name.endsWith(".mtd");
					  }
					});
					
					if(matches.length == 0)
			    		throw new OtherException("NO TABLES EXIST",prwtr,f);
					
					//for each metadata file, print its contents to the screen
					for (File file : matches)
					{
						fileReader = new Scanner(new FileInputStream(file));
						
						//get the table name from the file name
						String tableName = file.getName().substring(0,file.getName().indexOf(".mtd"));
						
						//print the table name and each line of the file
						if(f)
							prwtr.println(tableName.toUpperCase());
						else
							System.out.println(tableName.toUpperCase());
						
						//skip first line for record size in bytes
						fileReader.nextLine();
						while(fileReader.hasNextLine())
						{
							if(f)
								prwtr.println(fileReader.nextLine());
							else
								System.out.println(fileReader.nextLine());
						}
						if(f)
							prwtr.println("------------------------------------------------");
						else
							System.out.println("------------------------------------------------");
						fileReader.close();
					}
					//printMess("",13,f);
				}
				//checks to see if it is an identifier
				else
				{
					if(check(nm) == true)
					{
						//get the metadata file from this table and print its contents to the screen
						File file = new File(currentDB.getAbsolutePath()+"/"+nm+".mtd");
						
						if(!file.exists())
				    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
						
						Scanner fileReader = new Scanner(new FileInputStream(file));
						String tableName = file.getName().substring(0,file.getName().indexOf(".mtd"));
						
						if(f)
							prwtr.println(tableName.toUpperCase());
						else
							System.out.println(tableName.toUpperCase());
						
						//skip first line for record size in bytes
						fileReader.nextLine();
						while(fileReader.hasNextLine())
						{
							if(f)
								prwtr.println(fileReader.nextLine());
							else
								System.out.println(fileReader.nextLine());
						}
						if(f)
							prwtr.println("------------------------------------------------");
						else
							System.out.println("------------------------------------------------");
						//printMess("",13,f);
					}
					else
						printMess(nm,1,f);
				}
			}
			else
				printMess(nm,10,f);
		}
		else
			printMess(";",1,f);
	}
	
	private static void let(boolean f) throws SyntaxException, OtherException, IOException, ClassNotFoundException
	{
		if((st.hasMoreTokens())==true)
		{
			String nm = st.nextToken();
			String tableName = "";
		
			if(check(nm) == true)
			{
				tableName = nm;
				if (!st.hasMoreTokens())
					throw new SyntaxException(wholeLine, ";","key",prwtr,f );
					
				nm = st.nextToken();
				nm = nm.toUpperCase();
				//checks to see if it says key
				if(nm.equals("KEY"))
				{
					if (!st.hasMoreTokens())
						throw new SyntaxException(wholeLine, ";","identifier",prwtr,f );
						
					nm = st.nextToken();
					if(check(nm) == true)
					{
						String key = nm;
						
						if (!st.hasMoreTokens())
							throw new SyntaxException(wholeLine, ";","SELECT",prwtr,f );
							
						nm = st.nextToken();
						nm = nm.toUpperCase();
						//checks to see if is  select of not
						if(nm.equals("SELECT"))
						{
							
							select(f, true);
							
							ArrayList<TableAttribute> tempTAs = getTableAttributes(tableNames.get(0), f);
							
							ArrayList<ArrayList <String>> selectedData;
							
							if(tableNames.size() > 1)
							{
								selectedData = join(tableNames.get(0),false,f);
								tempTAs = joinedTableAttributes;
							}
							else if(wholeLine.toUpperCase().contains(" WHERE "))
								selectedData = selectWithWhere(tableNames.get(0), false,f);
							else
								selectedData = selectFromFile(false, f);
							
							ArrayList<String> attributeNames = selectedData.get(0);
						
							tableAttributes = new ArrayList<TableAttribute>();
							
							for(TableAttribute currentAttribute : tempTAs)
							{
								if(attributeNames.contains(currentAttribute.getName()))
								{
									if(key.equals(currentAttribute.getName()))
										currentAttribute.makePrimaryKey();
									
									if(!key.equals(currentAttribute.getName()) && currentAttribute.isPrimaryKey())
										currentAttribute.makeNotPrimaryKey();
									
									tableAttributes.add(currentAttribute);
								}
							}
							
							ArrayList<TableAttribute> tempTableAttributes = new ArrayList<TableAttribute>();
							
							int indexOfKey = 0, i = 0;
							for(TableAttribute currentAttribute : tableAttributes)
							{
								if(currentAttribute.isPrimaryKey())
								{
									tempTableAttributes.add(currentAttribute);
									indexOfKey = i;
								}
								i++;
							}
							
							for(TableAttribute currentAttribute : tableAttributes)
							{
								if(!currentAttribute.isPrimaryKey())
									tempTableAttributes.add(currentAttribute);
							}
							
							tableAttributes = tempTableAttributes;
							
							createTable(tableName, f);
							
							selectedData.remove(0);
							
							ArrayList<ArrayList <String>> selectedDataOrdered = new ArrayList<ArrayList <String>>();
							
							for(ArrayList<String> record : selectedData)
							{
								ArrayList<String> newRecord = new ArrayList<String>();
								i = 0;
								
								for(String value : record)
								{
									if(i == indexOfKey)
									{
										newRecord.add(value);
									}
									
									i++;
								}
								
								i = 0;
								for(String value : record)
								{
									if(i != indexOfKey)
									{
										newRecord.add(value);
									}
									i++;
								}
								
								selectedDataOrdered.add(newRecord);
							}
							
							/*for(ArrayList<String> record : selectedData)
							{
								ArrayList<String> s = new ArrayList<String>();
								i = 0;
								
								for(String value : record)
								{
									if(i != indexOfKey)
									{
										s.add(value);
									}
									
									i++;
								}
								selectedDataOrdered.add(s);
							}*/
							
						/*	for(ArrayList<String> record : selectedDataOrdered)
							{
								
								for(String value : record)
								{
									System.out.print(value + " ");
								}
								System.out.println();
							}*/
							
							values = selectedDataOrdered;
							
							tableNames = new ArrayList<String>();
							
							tableNames.add(tableName);
							
							fileInsert(f);
							
							printMess("",13,f);
						}
						else
							printMess(nm,8,f);
					}
					//looks for identifier
					else
						printMess(nm,1,f);
				}
				//looks for select
				else if (nm.equals("SELECT"))
				{
					select(f, true);
					
					ArrayList<TableAttribute> tempTAs = getTableAttributes(tableNames.get(0), f);
					
					//ArrayList<ArrayList <String>> selectedData = selectFromFile(false, f);
					
					ArrayList<ArrayList <String>> selectedData;
					
					if(tableNames.size() > 1)
					{
						selectedData = join(tableNames.get(0),false,f);
						tempTAs = joinedTableAttributes;
					}
					else if(wholeLine.toUpperCase().contains(" WHERE "))
						selectedData = selectWithWhere(tableNames.get(0), false,f);
					else
						selectedData = selectFromFile(false, f);
					
					ArrayList<String> attributeNames = selectedData.get(0);
				
					tableAttributes = new ArrayList<TableAttribute>();
					
					for(TableAttribute ta : tempTAs)
					{
						if(attributeNames.contains(ta.getName()))
						{
							tableAttributes.add(ta);
						}
					}
					
					createTable(tableName, f);
					
					selectedData.remove(0);
					
					values = selectedData;
					
					tableNames = new ArrayList<String>();
					
					tableNames.add(tableName);
					
					fileInsert(f);
					
					printMess("",13,f);
				}
				else
					printMess(nm,9,f);
			}
			else
				printMess(nm,1,f);
		}
		else
			printMess("INVALID CHARACTER",14,f);
	}
	
	private static void insert(boolean f) throws SyntaxException, IOException, 
												 OtherException, ClassNotFoundException
	{
		tableNames = new ArrayList<String>();
            if(st.hasMoreTokens())
            {
            	String token = st.nextToken();
                token = token.toUpperCase();
                if(token.equals("INTO"))
                {
                    insertInto(f);
                }
                else
                {
                    throw new SyntaxException(wholeLine, token, "INTO", prwtr, f);
                }
            }
            else
            {
            	throw new SyntaxException(wholeLine, ";", "INTO", prwtr, f);
            }
	}
	
        private static void insertInto(boolean f) throws SyntaxException, IOException, 
        												 OtherException, ClassNotFoundException
        {
            if(st.hasMoreTokens())
            {
                String token = st.nextToken();
                if(check(token) == true)
                {
                	tableNames.add(token);
                    insertTableNm(f);
                }
                else
                {
                	throw new SyntaxException(wholeLine, token, "constant", prwtr, f);
                }
            }
            else
            {
            	throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
            }
        }
	private static void insertTableNm(boolean f) throws SyntaxException, IOException, 
														OtherException, ClassNotFoundException
        {
            if(st.hasMoreTokens())
            {
                String token = st.nextToken();
                token = token.toUpperCase();

                if(token.equals("VALUES"))
                {
                    insertValues(f);
                }
                else
                {
                	throw new SyntaxException(wholeLine, token, "VALUES", prwtr, f);
                }
            }
            else
            {
            	throw new SyntaxException(wholeLine, ";", "VALUES", prwtr, f);
            }
        }
        private static void insertValues(boolean f) throws SyntaxException, IOException, 
        													OtherException, ClassNotFoundException
        {
            String token = "";
            String badInput = "";
            boolean flag = true;
            boolean flag2 = true;
            int ctr = 0;
            int numberOfRecords = 0;
            
            values = new ArrayList<ArrayList<String>>();
            
            //removes whitespace from the token
            while(st.hasMoreTokens())
            {
                token += st.nextToken();
            }
            StringTokenizer st2 = new StringTokenizer(token,"()",true);
            
            while(st2.hasMoreTokens())
            {
                token = "";
                ctr = 0;
                while(st2.hasMoreTokens() && ctr < 3)
                {
                    token += st2.nextToken();
                    ctr++;
                    
                    if(ctr == 1)
                    {
                    	if (token.charAt(0) != '(')
                			throw new SyntaxException(wholeLine, Character.toString(token.charAt(0)), "(", prwtr, f);
                    }
                    else if(ctr == 3)
                    {
                    	if (token.charAt(token.length()-1) != ')')
                			throw new SyntaxException(wholeLine, Character.toString(token.charAt(token.length()-1)), ")", prwtr, f);
                    	
                    	if(st2.hasMoreTokens())
                    	{
                    		String t = st2.nextToken();
                    		if (t.equals(";"))
                    			break;
                    		if (!t.equals(","))
                    			throw new SyntaxException(wholeLine, t, "comma", prwtr, f);
                    	}
                    }
                }
                if((token.charAt(0) != '(' || token.charAt(token.length() - 1) != ')') && token.charAt(0) != ';')
                {
                    flag = false;
                    break;
                }
                else
                {
                    if(token.equals(";") == false)
                    {
                    	String [] s = token.substring(1, token.length() - 1).split(",");
                    	ArrayList<String> innerList = new ArrayList();
                    	for (int i = 0; i< s.length;i++)
                    	{ 
                            if(checkConstant(s[i]).equals("fail"))
                            {
                                flag2 = false;
                                badInput = s[i];
                                
                            }
                            else
                            	innerList.add(s[i]);
                    	}
                    	values.add(innerList);
                    }
                }
                
                numberOfRecords++;
            }
            if(flag2 == false)
            {
            	throw new SyntaxException(wholeLine, badInput, "constant", prwtr, f);
            }
            if(flag == true)
            {
            	fileInsert(f);
            	
            	
            	
            	printMess("DONE", 13, f);
            }
            else
            {
            	throw new SyntaxException(wholeLine, "", "parenthesis", prwtr, f);
            }
        }
        
    private static void fileInsert(boolean f) throws SyntaxException, IOException, 
										OtherException, ClassNotFoundException
    {
    	File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	File treeFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".itf");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	ObjectInputStream treeStream = new ObjectInputStream(new FileInputStream(treeFile));
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	BinarySearchTree indexTree = new BinarySearchTree();
    	indexTree = (BinarySearchTree)treeStream.readObject();
    	treeStream.close();
    	
    	tableAttributes = new ArrayList<TableAttribute>();
    	
    	int recordLength = Integer.parseInt(metaInfo.nextLine());
    	long dataFileLength = tableAccess.length();
    	long totalFileRecords = dataFileLength/recordLength;
    	long pointer = 0;
    	boolean makePrimaryIndex = false;
    	
    	metaInfo.nextLine();
    	
    	while(metaInfo.hasNextLine())
		{
			TableAttribute tempTA = new TableAttribute();
			String [] line = metaInfo.nextLine().split("\\s+");
			
			tempTA.setName(line[0]);
			tempTA.setDomain(line[1]);
				
			if(line.length > 3)
				tempTA.makePrimaryKey();
			tableAttributes.add(tempTA);
			
		}
    	metaInfo.close();
    	
    	//error checking
    	/*for (ArrayList<String> record : values)
    	{
    		int attributeIndex = 0;
    		if(record.size() != tableAttributes.size())
    		{
    			tableAccess.close(); 
    			throw new OtherException("Number of insert values doesn't match table attributes.",prwtr,f);
    		}
    		
    		for(String constant : record)
    		{
    			TableAttribute temp = tableAttributes.get(attributeIndex);
    			String currentConstant = checkConstant(constant);
    			
    			//a primary key exists in the metadata file
    			if(temp.isPrimaryKey())
    			{
    				//search the tree for duplicate keys
    				if(indexTree.search(constant))
    				{
    					tableAccess.close(); 
    					throw new OtherException ("Primary key value already exists.", prwtr, f);
    				}
    			}
    			boolean one = !currentConstant.equals(temp.getDomain());
    			boolean two = currentConstant.equals("int");
    			boolean three = !temp.getDomain().equals("integer");
    			
    			if(one && three)
    			{
    				tableAccess.close(); 
    				throw new OtherException("Domain type mismatch. Found: "+currentConstant+" Expecting: "+temp.getDomain(),prwtr, f);
    			}
    			else if(temp.getDomain().equals("integer") && !currentConstant.equals("int"))
    			{
    				tableAccess.close(); 
    				throw new OtherException("Domain type mismatch. Found: "+currentConstant+" Expecting: "+temp.getDomain(),prwtr, f);
    			}
    			attributeIndex++;
    		}
    	}*/
    	
    	//loop through each record of constants
    	for (ArrayList<String> record : values)
    	{
    		int attributeIndex = 0;
    		
    		if(record.size() != tableAttributes.size())
    		{
    			tableAccess.close(); 
    			throw new OtherException("Number of insert values doesn't match table attributes.",prwtr,f);
    		}
    		
    		for(String constant : record)
    		{
    			TableAttribute temp = tableAttributes.get(attributeIndex);
    			String currentConstant = checkConstant(constant);
    			
    			//a primary key exists in the metadata file
    			if(temp.isPrimaryKey())
    			{
    				//search the tree for duplicate keys
    				if(indexTree.search(constant))
    				{
    					tableAccess.close(); 
    					throw new OtherException ("Primary key value already exists.", prwtr, f);
    				}
    			}
    			boolean one = !currentConstant.equals(temp.getDomain());
    			boolean two = currentConstant.equals("int");
    			boolean three = !temp.getDomain().equals("integer");
    			
    			if(one && three)
    			{
    				tableAccess.close(); 
    				throw new OtherException("Domain type mismatch. Found: "+currentConstant+" Expecting: "+temp.getDomain(),prwtr, f);
    			}
    			else if(temp.getDomain().equals("integer") && !currentConstant.equals("int"))
    			{
    				tableAccess.close(); 
    				throw new OtherException("Domain type mismatch. Found: "+currentConstant+" Expecting: "+temp.getDomain(),prwtr, f);
    			}
    			attributeIndex++;
    		}
    		pointer = tableAccess.length()/recordLength;
    		
    		fileInsertRecord(record,indexTree, pointer, f);
    	}
    	tableAccess.close();
    	
    	treeFile.delete();
    	
    	File treeFile2 = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".itf");
    	
    	ObjectOutputStream treeOutStream = new ObjectOutputStream(new FileOutputStream(treeFile2));
    	treeOutStream.writeObject(indexTree);
    	
    	treeOutStream.close();
    }
    
    private static void fileInsertRecord(ArrayList<String> record, BinarySearchTree indexTree, long pointer, boolean f) throws FileNotFoundException, IOException, OtherException, ClassNotFoundException
    {
    	File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    
    	int attributeIndex = 0;
		
		tableAccess.seek(tableAccess.length());
		
		//write the deletion marker byte to the file
		tableAccess.writeByte('n');
		
		//loop through each constant in the record
		for(String constant : record)
		{
			tableAccess.seek(tableAccess.length());
			TableAttribute temp = tableAttributes.get(attributeIndex);
			
			//try to insert into the binary search tree
			if(temp.isPrimaryKey())
			{
				if(indexTree.search(constant))
				{
					tableAccess.close(); 
					throw new OtherException ("Primary key value already exists.", prwtr, f);
				}
				if(attributeIndex == 0)
					indexTree.insert(constant, pointer);
			}
				
			String type = checkConstant(constant);
        	
			if(type == "float")
        	{
        		double c = Double.parseDouble(constant);
        			
        		tableAccess.writeDouble(c);
        	}
        	else if(type == "int")
        	{
        		int integer = Integer.parseInt(constant);
        			
        		tableAccess.writeInt(integer);
        	}
        	else
        	{
        		while(constant.length() < 100)
        		{
        			constant += "\b";
        		}

        		tableAccess.writeBytes(constant);
        	}
		
			attributeIndex++;
		}
		tableAccess.close();
    }
	
	private static void update(boolean f) throws SyntaxException, OtherException, ClassNotFoundException, IOException
	{
		//grab next token
		if (!st.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "table name", prwtr, f);
		String currentToken = st.nextToken();
		tableNames = new ArrayList<String>();
		boolean valid;
				
		//continue if valid table name found
		if(check(currentToken))
		{
			//grab next token
			if (!st.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "SET", prwtr, f);
			
			tableNames.add(currentToken);
			
			currentToken = st.nextToken();
				
			//continue if "SET" found
			if (currentToken.toUpperCase().equals("SET"))
			{
				//grab next token
				if (!st.hasMoreTokens())
					throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
				currentToken = st.nextToken();
				
				//check the SET condition validity
				valid = checkSetCondition(currentToken, f);

				//continue only if SET condition is valid
				if(valid)
				{
    				//line has no where clause so set condition validity is enough to print VALID
    				if (!wholeLine.toUpperCase().contains(" WHERE "))
    				{	
    					fileUpdate(f);
    					printMess("DONE", 13, f);
    				}
    				//WHERE condition exists, so check it
    				else
    				{
    					//catch up to the global string tokenizer (which wasn't used in checkSetCondition)
    					while(!currentToken.toUpperCase().contains("WHERE"))
    						currentToken = st.nextToken();

    					//check WHERE condition
   						//checkWhereCondition(currentToken, f);
    					updateWhere(currentToken, f);
    					
    					printMess("DONE",13,f);
   					}
				}
			}
			else
				throw new SyntaxException(wholeLine, currentToken, "SET", prwtr, f);
		}
		else
			throw new SyntaxException(wholeLine, currentToken, "identifier", prwtr, f);
			
	}
	
	private static boolean checkSetCondition(String currentToken, boolean f)
	{
		String current = "";
		StringTokenizer sst;
		String temp = "";
		sets = new ArrayList<SetCondition>();

		try
		{
    		if (!st.hasMoreTokens() && !currentToken.endsWith(";"))
    			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
    		
    		if(wholeLine.toUpperCase().contains("WHERE"))
    			//create a string from the current token to "WHERE" in the line
    			temp = wholeLine.substring(wholeLine.indexOf(currentToken), wholeLine.toUpperCase().indexOf("WHERE"));
    		else
    			//create a string from the current token to the end of the line
    			temp = wholeLine.substring(wholeLine.indexOf(currentToken));
    		
    		//remove whitespaces, and set up a new tokenizer with the proper delimiters
    		temp = temp.replaceAll("\\s+", "");
    		sst =  new StringTokenizer(temp, ",=;", true);
    		current = sst.nextToken();
    		boolean one = false;
    		
    		//make sure first token isn't blank
    		if (current.isEmpty())
    			throw new SyntaxException(wholeLine, " ", "identifier", prwtr, f);
    		
    		//ensure there are more tokens
    		if (!sst.hasMoreTokens())
    		{	
    			while(st.hasMoreTokens() && !current.toUpperCase().equals("WHERE"))
					current = st.nextToken();
				throw new SyntaxException(wholeLine, current, "equals sign", prwtr, f);
    		}
    		
    		//check first token validity
			if(!check(current))
				throw new SyntaxException(wholeLine, current, "identifier", prwtr, f);
    		
    		//check each token that conforms to the set condition format
    		while(sst.hasMoreTokens())
    		{   
    			SetCondition tempSC = new SetCondition();
    			
    			//check attribute name validity
    			if(!check(current))
    				throw new SyntaxException(wholeLine, current, "identifier", prwtr, f);
    			
    			tempSC.setIdentifier(current);
    				
    			//grab the equals token
    			if(sst.hasMoreTokens())
    				current = sst.nextToken();
    			else
    			{
    				while(st.hasMoreTokens() && !current.toUpperCase().equals("WHERE"))
    					current = st.nextToken();	
    				throw new SyntaxException(wholeLine, current, "equals sign", prwtr, f);
    			}
    				 
    			//check the equals token validity
    			if(!current.equals("="))
    				throw new SyntaxException(wholeLine, current, "equals sign", prwtr, f);
    				
    			//grab the identifier/constant token
    			if(sst.hasMoreTokens())
    				current = sst.nextToken();
    			else
    			{
    				while(st.hasMoreTokens() && !current.toUpperCase().equals("WHERE"))
    					current = st.nextToken();
    				throw new SyntaxException(wholeLine, current, "constant or identifier", prwtr, f);
    			}
  
    			//check constant/identifier validity
    			if (checkConstant(current).equals("fail") && !check(current))
    				throw new SyntaxException(wholeLine, current, "constant or identifier", prwtr, f);
    			
    			tempSC.setConstant(current);
    				
    			sets.add(tempSC);
    			
    			//grab next token if there are any
    			if(sst.hasMoreTokens())
    				current = sst.nextToken();
    			//otherwise end the loop
    			else
    				break;
    			
    			//check for comma or semicolon, return an error if not found
    			if (current.equals(","))
    			{
    				//ensure there are more tokens before looping around again
    				if(!sst.hasMoreTokens())
    				{
        				while(st.hasMoreTokens() && !current.toUpperCase().equals("WHERE"))
        					current = st.nextToken();
        				throw new SyntaxException(wholeLine, current, "identifier", prwtr, f);
        			}
    				current = sst.nextToken();
    				
    				//prevents the while loop from ending 
    				if(!sst.hasMoreTokens())
    				{
        				while(st.hasMoreTokens() && !current.toUpperCase().equals("WHERE"))
        					current = st.nextToken();
        				throw new SyntaxException(wholeLine, current, "equals sign", prwtr, f);
        			}
    			}
    			//if a semicolon is encountered, the condition is considered valid so end the loop
    			else if(current.equals(";"))
    				break;
    			else
    				throw new SyntaxException(wholeLine, current, "comma or semicolon", prwtr, f);
    		}	
    		
    		return true;
		}
		catch(SyntaxException e)
		{
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Updates a file using the first table name in the global ArrayList of table names,
	 * the global ArrayList of tableAttributes, and the global ArrayList of set conditions.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws OtherException Any other exception.
	 * @throws ClassNotFoundException 
	 */
	private static void fileUpdate(boolean f) throws IOException, OtherException, ClassNotFoundException
	{
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	
    	//store the size of a record
    	int recordLength = Integer.parseInt(metaInfo.nextLine());
    	metaInfo.close();
    	
    	//ArrayList<String> updateAttributes = getUpdateAttributeNames(tableNames.get(0), sets);
    	tableAttributes = new ArrayList<TableAttribute>();
    	tableAttributes = getTableAttributes(tableNames.get(0), f);
    	
    	//store the file length and the number of records in the file
    	long dataFileLength = tableAccess.length();
    	long totalFileRecords = dataFileLength/recordLength;
    	tableAccess.close();
    	
		ArrayList<String> s = new ArrayList<String>();
		
		//for each set condition in the sets that were entered
		for(SetCondition set : sets)
		{
			if(!isAttribute(tableAttributes, set.getIdentifier()))
				throw new OtherException("Found: "+set.getIdentifier()+ " Expecting: table attribute",prwtr,f); 
			if(check(set.getConstant()) && !isAttribute(tableAttributes, set.getConstant()))
				throw new OtherException("Found: "+set.getConstant()+ " Expecting: table attribute",prwtr,f); 
			
			//for every record in the file, update the table with the entered attribute and the entered constant
			for(int i = 0; i < totalFileRecords; i++)
			{
				setValue(tableNames.get(0), set.getIdentifier(), set.getConstant(), i, recordLength,'n', f);
			}
		}

	}
	
	private static void updateWhere(String currentToken, boolean f) throws SyntaxException, OtherException, IOException, ClassNotFoundException
	{
		ArrayList<Integer> recordPositions = checkWhereCondition(currentToken, f);
		
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	
    	//store the size of a record
    	int recordLength = Integer.parseInt(metaInfo.nextLine());
    	metaInfo.close();
    	
		tableAttributes = new ArrayList<TableAttribute>();
    	tableAttributes = getTableAttributes(tableNames.get(0), f);
    	
    	//store the file length and the number of records in the file
    	long dataFileLength = tableAccess.length();
    	long totalFileRecords = dataFileLength/recordLength;
    	tableAccess.close();
    	
		for(SetCondition set : sets)
		{
			if(!isAttribute(tableAttributes, set.getIdentifier()))
				throw new OtherException("Found: "+set.getIdentifier()+ " Expecting: table attribute",prwtr,f); 
			if(check(set.getConstant()) && !isAttribute(tableAttributes, set.getConstant()))
				throw new OtherException("Found: "+set.getConstant()+ " Expecting: table attribute",prwtr,f); 
			
			//for every record that satisfied a where condition
			for(int i : recordPositions)
			{
				setValue(tableNames.get(0), set.getIdentifier(), set.getConstant(), i-1, recordLength,'n',f);
			}
		}
	}
	
	private static boolean isAttribute(ArrayList<TableAttribute> tas, String attributeName)
	{
		for(TableAttribute ta : tas)
		{
			if(attributeName.equals(ta.getName()))
				return true;
		}
    	
		return false;
	}
	
	private static void rename(boolean f) throws SyntaxException, OtherException, IOException
	{
		//grab next token
		if (!st.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
		String currentToken = st.nextToken(), tableName;
		StringTokenizer attrListTokens;
		String attributeList = "";
		ArrayList<String> attributeNames = new ArrayList<String>();
		tableAttributes = new ArrayList<TableAttribute>();
		boolean valid = false;
			
		//ensure identifier is found
		if(!check(currentToken))
		{
			if(currentToken.endsWith(";"))
				throw new SyntaxException(wholeLine, ";", "(", prwtr, f);
			throw new SyntaxException(wholeLine, currentToken, "identifier", prwtr, f);
		}
		
		tableName = currentToken;
		
		//grab next token
		if (!st.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "(", prwtr, f);
		currentToken = st.nextToken();

		attributeList = wholeLine.substring(wholeLine.indexOf(currentToken));
		attributeList = attributeList.replaceAll("\\s+", "");
		attrListTokens = new StringTokenizer(attributeList, "(),;", true);
		
		if (!attrListTokens.hasMoreTokens())
			throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
		currentToken = attrListTokens.nextToken();
		
		if (!currentToken.equals("("))
			throw new SyntaxException(wholeLine, currentToken, "(", prwtr, f);
		
		while(attrListTokens.hasMoreTokens())
		{
			currentToken = attrListTokens.nextToken();
			
			if(!check(currentToken))
				throw new SyntaxException(wholeLine, currentToken, "identifier", prwtr, f);
			
			attributeNames.add(currentToken);
			
			if (!attrListTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
			currentToken = attrListTokens.nextToken();
			
			if(currentToken.equals(")"))
			{
				if (!attrListTokens.hasMoreTokens())
					throw new SyntaxException(wholeLine, ";", "identifier", prwtr, f);
				currentToken = attrListTokens.nextToken();
				
				if (currentToken.equals(";"))
				{
					valid = true;
					break;
				}
				else
					throw new SyntaxException(wholeLine, currentToken, ";", prwtr, f);
			}
			else if(currentToken.equals(","))
				continue;
			else
				throw new SyntaxException(wholeLine, currentToken, "comma or )", prwtr, f);
		}
		
		while(st.hasMoreTokens())
			st.nextToken();
		if (valid && currentToken.equals(";"))
		{
			File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".mtd");
			
			//print an error if no metadata file and thus table exists
			if(!metaFile.exists())
				throw new OtherException("NO SUCH TABLE EXISTS", prwtr,f);
			
			Scanner metaInfo = new Scanner (new FileInputStream(metaFile));
			int attributes = 0;
			
			String recordSize = metaInfo.nextLine();
			
			//skip the information line of the file
			metaInfo.nextLine();
			while(metaInfo.hasNextLine())
			{
				TableAttribute tempTA = new TableAttribute();
				String [] line = metaInfo.nextLine().split("\\s+");
				
				if(attributeNames.size() > attributes)
				{
					tempTA.setName(attributeNames.get(attributes));
					tempTA.setDomain(line[1]);
					
					if(line.length > 3)
						tempTA.makePrimaryKey();
					tableAttributes.add(tempTA);
				}
				attributes++;
				
			}
			metaInfo.close();
			
			if(attributes != attributeNames.size())
				throw new OtherException("Number of atrributes does not match table. Table has "+attributes+" attributes.", prwtr,f);
			
			//delete the old file
			metaFile.delete();
			
			//create a new file
			metaFile.createNewFile();
			PrintWriter metaWriter = new PrintWriter (new FileOutputStream(metaFile));
			
			//print the table attributes to the new file with the new attribute names
			metaWriter.println(recordSize);
			metaWriter.println("Attributes           Types      Length(in Bytes)");
			for(TableAttribute ta : tableAttributes)
			{
				metaWriter.println(ta);
			}
			metaWriter.close();
			
			printMess("DONE", 13, f);
		}
	}
	
	private static void delete(boolean f) throws SyntaxException, OtherException, IOException, ClassNotFoundException
	{
		//checks to see if there are more tokens
		
		if((st.hasMoreTokens()) == true)
		{
			String nm = st.nextToken();
			//grabs the next token
			if(nm.contains(";"))
			{
				nm = remSemiColon(nm);
				//checks to see if it is an identifier or not
				if(check(nm) == true)
				{	
					fileDelete(nm,f);
			    	
					if(f)
						prwtr.println("Table "+nm.toUpperCase()+" deleted.");
					else
						System.out.println("Table "+nm.toUpperCase()+" deleted.");
				}
				else
					printMess(nm,1,f);
			}
			
			else
			{
				if(check(nm) == true)
				{
					String tableName = nm;
					//grabs the next token and sends it to relationship to handle it
					if(st.hasMoreTokens())
					{	
						nm = st.nextToken();
						//relationship(nm,f);
						
						File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".mtd");
						
						if(!metaFile.exists())
				    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
						
				    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
				    	
				    	int recordLength = Integer.parseInt(metaInfo.nextLine());
				    	metaInfo.close();
				    	
						tableNames = new ArrayList<String>();
						tableNames.add(tableName);
						ArrayList<Integer> deletePositions = checkWhereCondition(nm,f);
						
						if(deletePositions.isEmpty())
							throw new OtherException("No records to delete.",prwtr,f);
						
						for(int i : deletePositions)
						{
							setValue(tableName,"","",i-1,recordLength,'D',f);
						}
						
						printMess("DONE",13,f);
					}
					//else it needs a wehre clause
					else
						printMess(";",6,f);
				}
				else
					printMess(nm,1,f);
			}
		}
		//else error is thrown
		else
			printMess(";",14,f);
	}
	
	private static void fileDelete(String tableName, boolean f) throws OtherException
	{
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".dat");
    	File treeFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".itf");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".mtd");
    	
    	if(!metaFile.exists())
    		throw new OtherException("Table "+tableName.toUpperCase()+" could not be deleted because it doesn't exist.",prwtr,f);
    	if(!dataFile.exists())
    		throw new OtherException("Table "+tableName.toUpperCase()+" could not be deleted because it doesn't exist.",prwtr,f);
    	
    	dataFile.delete();
    	metaFile.delete();
    	treeFile.delete();
	}
	
	private static void aggregate(boolean f) throws OtherException, IOException, ClassNotFoundException
	{
		try
		{
			//grab next token
			if (!st.hasMoreTokens())
				throw new SyntaxException(wholeLine, ";", "command", prwtr, f);
			String currentToken = st.nextToken();
			
					if (checkAggrCondition(currentToken, f)) 
					{
						ArrayList<ArrayList<String>> selectedValues;
						
						attributeNames = new ArrayList<String>();
						
						for(AggregateCondition aggregate : aggregates)
						{
							attributeNames.add(aggregate.getIdentifier());
						}
						
						if(tableNames.size() == 1)
						{	
								if (!wholeLine.toUpperCase().contains(" WHERE "))
		    						selectedValues = selectFromFile(false,f);
		    					else
		    						selectedValues = selectWithWhere(currentToken, false,f);
						}
						else
						{
							selectedValues = join(currentToken, false,f);
						}
						
								ArrayList<ArrayList<String>> selectedValuesOrdered = new ArrayList<ArrayList <String>>();
								
								int index2 = 0;
								for(ArrayList<String> record : selectedValues)
								{
									ArrayList<String> newRecord = new ArrayList<String>();
									
									for(int i = 0; i < selectedValues.size();i++)
									{
										ArrayList<String> tempRecord = selectedValues.get(i);
										int index = 0;
										
										for(String value : tempRecord)
										{
											for(int j = 0;j < tempRecord.size();j++)
											{
												if(j == index2 && j == index)
													newRecord.add(value);
											}
											index++;
										}
										
									}
									index2++;
									
									if(!newRecord.isEmpty())
										selectedValuesOrdered.add(newRecord);
								}
								
							
								for(ArrayList<String> record : selectedValuesOrdered)
								{
									if(record.size() < 2)
										throw new OtherException ("No records to aggregate.",prwtr,f);
									
									for(AggregateCondition aggregate : aggregates)
									{
										String attributeName = record.get(0);
										String domain = checkConstant(record.get(1));
										ArrayList<Integer> integerValues = new ArrayList<Integer>();
										ArrayList<Double> doubleValues = new ArrayList<Double>();
										ArrayList<String> textValues = new ArrayList<String>();
	
										if(attributeName.equals(aggregate.getIdentifier()))
										{
											if(domain.equals("text") && !aggregate.getOperator().toUpperCase().equals("COUNT"))
												throw new OtherException("Cannot take "+aggregate.getOperator().toUpperCase()+" of text.",prwtr,f);
											
											int index = 0;
											
											for(String value : record)
											{
												if(index != 0)
												{
													if(domain.equals("int"))
													{
														integerValues.add(Integer.parseInt(value));
													}
													else if(domain.equals("float"))
													{
														doubleValues.add(Double.parseDouble(value));
													}
													else
													{
														textValues.add(value);
													}
												}
												index++;
											}
											double result;
											int resultInt;
											
											//float aggregate
											if(domain.equals("float"))
											{
												result = aggregate.aggrOperationDouble(aggregate.getOperator(), doubleValues);
												
												if(f)
												{	
													prwtr.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": ");
													
													if(aggregate.getOperator().toUpperCase().equals("count"))
														prwtr.printf("%.0f \n",result);
													else
														prwtr.printf("%.2f \n",result);
												}
												else
												{
													System.out.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": ");
													
													if(aggregate.getOperator().toUpperCase().equals("COUNT"))
														System.out.printf("%.0f \n",result);
													else
														System.out.printf("%.2f \n",result);
												}

											}
											//integer aggregate
											else if(domain.equals("int"))
											{
												if(aggregate.getOperator().equals("average"))
												{
													result = aggregate.aggrOperationInteger(aggregate.getOperator(), integerValues);
													if(f)
													{	
														prwtr.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": ");
														prwtr.printf("%.2f \n",result);
													}
													else
													{
														System.out.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": ");
														System.out.printf("%.2f \n",result);
													}
												}
												else
												{
													resultInt = (int)aggregate.aggrOperationInteger(aggregate.getOperator(), integerValues);
													if(f)
														prwtr.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": "+resultInt+"\n");
													else
														System.out.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": "+resultInt+"\n");
												}
											}
											//text aggregate
											else
											{
												resultInt = aggregate.aggrOperationText(textValues);
												if(f)
													prwtr.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": "+resultInt+"\n");
												else
													System.out.print(aggregate.getOperator().toUpperCase() + " of "+attributeName+": "+resultInt+"\n");
											}
										}
	
									}
									
								}
	    					}
    						//printMess(" ", 13, f);
    					//WHERE condition exists, so check it
    					/*else
    					{
    						//catch up to the global string tokenizer (which wasn't used in checkSetCondition)
    						while(!currentToken.toUpperCase().contains(" WHERE "))
    							currentToken = st.nextToken();
    						
    						//check WHERE condition
    						//relationship(currentToken, f);
    						checkWhereCondition(currentToken, f);
    					}*/
					
			
		}
		catch (SyntaxException e)
		{
			System.out.println(e.getMessage());
		}	
		
	}
	private static boolean checkAggrCondition(String currentToken, boolean f)  {
		String current = "";
		StringTokenizer sst;
		String temp = "";
		tableNames = new ArrayList<String>();
		aggregates = new ArrayList<AggregateCondition>();
	try	{
		
		if(wholeLine.toUpperCase().contains(" WHERE "))
			//create a string from the current token to "WHERE" in the line
			temp = wholeLine.substring(wholeLine.indexOf(currentToken), wholeLine.toUpperCase().indexOf("WHERE"));
		else
			temp = wholeLine.substring(wholeLine.indexOf(currentToken));
    		
    		//remove whitespaces, and set up a new tokenizer with the proper delimiters
    	temp = temp.replaceAll(",\\s*", " , ");
    	temp = temp.replaceAll(";\\s*", " ;");
	
    		
    		sst= new StringTokenizer(temp);
    	

    		current = sst.nextToken();
    		if (!sst.hasMoreTokens() && !currentToken.endsWith(";"))
    			throw new SyntaxException(wholeLine, ";", "FROM", prwtr, f);
    		while (sst.hasMoreTokens()) {
    			AggregateCondition currentAggrCondition = new AggregateCondition();
    	
    				if (current.toUpperCase().equals("SUM") 
    						|| current.toUpperCase().equals("AVERAGE") 
    						|| current.toUpperCase().equals("COUNT")
    						||	current.toUpperCase().equals("MIN") 
    						||current.toUpperCase().equals("MAX")) {
    					
    					currentAggrCondition.setOperator(current);
    					
    					if (!sst.hasMoreTokens())
    						throw new SyntaxException(wholeLine, ";", "attribute name", prwtr, f);
    					current = sst.nextToken();
    					if (check(current)) 
    					{
    						
    						currentAggrCondition.setIdentifier(current);
    						
    						aggregates.add(currentAggrCondition);
    					
    						if (!sst.hasMoreTokens())
    							throw new SyntaxException(wholeLine, ";", "FROM", prwtr, f);
    						current=sst.nextToken();
    					}
    					else
    						throw new SyntaxException(wholeLine, current, "identifier", prwtr, f);
    					
    				} //end if
    				else
    					throw new SyntaxException(wholeLine, current, "(SUM | AVERAGE | COUNT | MIN | MAX)", prwtr, f);
    				if (current.equals(",")) 
        				current = sst.nextToken();
    				else if (current.toUpperCase().equals("FROM")) 
    				{
    					if (!sst.hasMoreTokens())
    						throw new SyntaxException(wholeLine, ";", "attribute name", prwtr, f);
    					current = sst.nextToken();
    					if (check(current)) 
    					{

    						
    						String tableNameLine = wholeLine.substring(wholeLine.indexOf(current));
    						
    						StringTokenizer currentToEnd = new StringTokenizer(tableNameLine);
    						String tempCurrent = "";
    						
    						tableNameLine = "";
    						while(currentToEnd.hasMoreTokens())
    						{
    							tempCurrent = currentToEnd.nextToken();
    							
    							if(tempCurrent.toUpperCase().equals("WHERE"))
    								tempCurrent = " WHERE ";
    							tableNameLine += tempCurrent;
    						}
    						//tableNameLine = tableNameLine.replaceAll("\\s+","");
    						StringTokenizer tableTokens = new StringTokenizer(tableNameLine, " ,;",true);
    						
    						
    						
    						while (tableTokens.hasMoreTokens() )
    						{
    							
    							currentToken = tableTokens.nextToken();
    							if(!check(currentToken))
    								throw new SyntaxException(wholeLine, currentToken, "identifier", prwtr, f);
        						tableNames.add(currentToken);
    							
        						currentToken = tableTokens.nextToken();
    							
        						if(currentToken.equals(" "))
        						{
        							currentToken = tableTokens.nextToken();
        							
        							if(currentToken.equals(";"))
        								return true;
        							else if(currentToken.equals(","))
        								continue;
        							else if(currentToken.toUpperCase().equals("WHERE"))
        								return true;
        							else
        								throw new SyntaxException(wholeLine, currentToken, "comma, semicolon WHERE", prwtr, f);
        						}
    							if(currentToken.equals(";"))
    								return true;
    							else if(currentToken.equals(","))
    								continue;
    							else
    								throw new SyntaxException(wholeLine, currentToken, "comma, semicolon, or WHERE", prwtr, f);
    						}
    						//while()
    						
    						return true;
    					}
    					else
    						throw new SyntaxException(wholeLine, current, "identifier", prwtr, f);
    				}
    				else
    					throw new SyntaxException(wholeLine, current, "FROM", prwtr, f);
    		} //end while 	
		} //end try
		catch(SyntaxException e)
		{
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}
	private static void input(boolean f) throws IOException, SyntaxException
	{
		if(st.hasMoreTokens())
		{
			//grabs the next token
			String nm = st.nextToken();
			Scanner sc;
			String hld = "";
			StringTokenizer ist;
			if(check(nm) || nm.contains(";") && check(nm.substring(0,nm.length()-1)))
			{
				StringTokenizer fix = new StringTokenizer(wholeLine.substring(wholeLine.indexOf(nm)));
				String checkSemicolon = "";
				
				if(fix.hasMoreTokens())
					fix.nextToken();
				if(fix.hasMoreTokens())
					checkSemicolon = fix.nextToken();
				
				//if it is just input and contains ; then correct syntax
				if(nm.contains(";") || checkSemicolon.equals(";"))//peekLast(nm).equals(";"))
				{
					
					nm = remSemiColon(nm);
					//checks to see if the file exists
					File fb = new File(nm);
					
					//if it doesn't, try appending the extension
					if(!fb.exists())
					{
						nm = nm + ".txt";
						fb = new File(nm);
					}
					
					if(fb.exists())
					{
					
						sc = new Scanner(new FileInputStream(nm));
						int count = 0;
						
						while(sc.hasNextLine())
						{
							hld = hld + " " + sc.nextLine();
							hld = hld.trim();
							count++;
						}
						sc.close();//closes input stream
						ist = new StringTokenizer(hld,";");
						//loops for every token in the file
						while(ist.hasMoreTokens())
						{
							//recreates the token 
							hld = ist.nextToken() + ";";
							
							if(hld.replaceAll("\\s+", "").length() > 1){
								//string replacements for easier handling
								hld =hld.replaceAll("\\(", " ( ");
								hld = hld.replaceAll("\\)", " ) ");
								hld = hld.replaceAll("\\s*;", ";");
								wholeLine = hld;
							
								//tokenizes the new token
								String ihld = hld;
								st = new StringTokenizer(ihld);
								//set the first token to upper case to negate case sensitivity 
								//holds the command value
								hld = st.nextToken();
								
								if(hld.endsWith(";"))
									hld = hld.substring(0, hld.indexOf(";"));
								int inumCom = getCom(hld.toUpperCase());//calls a function to assign the command a value
								//method to do the switch handling
								redirect(ihld, inumCom, false);
								
							}
						}	
				
					}
					else
						printMess("FILE NOT FOUND",14,f);
				}
				else
				{
					File fb = new File(nm);
					
					//if file doesn't exist, try appending the extension
					if(!fb.exists())
					{
						nm = nm + ".txt";
						fb = new File(nm);
					}
					
					if(fb.exists())
					{
						sc = new Scanner(new FileInputStream(nm));
						//reads in the entire file
						while(sc.hasNextLine())
						{
							hld = hld + " "+ sc.nextLine();
							hld = hld.trim();
						}
						sc.close();//closes input stream
						nm = st.nextToken();
						nm = nm.toUpperCase();
						//makes sure it says OUTPUT and not anything else
						if(nm.equals("OUTPUT"))
						{
							 if (!st.hasMoreTokens())
									throw new SyntaxException(wholeLine, ";","FileName",prwtr,f );
								
							nm = st.nextToken();
							//checks to make sure it is the correct identifier
							if(check(nm) || nm.contains(";") && check(nm.substring(0,nm.length()-1)))
							{
								//checks to see if it is a terminal
								if(nm.contains(";") || peekLast(nm).equals(";"))
								{
									nm = remSemiColon(nm);
									
									//global printwriter needs to be placed here
									ist = new StringTokenizer(hld,";");
									prwtr = new PrintWriter(new FileOutputStream(nm));
									while(ist.hasMoreTokens())
									{
										//grabs the first line
										String nm2 = ist.nextToken() + ";";
										
										//grabs the next token
										nm2 = nm2.replaceAll("\\(", " ( ");
										nm2 = nm2.replaceAll("\\)", " ) ");
										nm2 = nm2.replaceAll("\\s*;", ";");
										
										wholeLine = nm2;
										
										st = new StringTokenizer(nm2);
										
										String ihld2 = st.nextToken();
										
										if(ihld2.endsWith(";") && ihld2.length() > 1)
										{
											ihld2 = ihld2.substring(0, ihld2.indexOf(";"));
										}
										int inumCom = getCom(ihld2.toUpperCase());//calls a function to assign the command a value
										//method to do the switch handling	
										redirect(nm2, inumCom, true);
										
										prwtr.flush();
									}
									prwtr.close();
									
									printMess("",13,false);
								}
								//else error is thrown
								else
									printMess(nm,10,f);
							}
							//else error is thrown
							else
								printMess(nm,1,f);
						}
						//else error is thrown
						else
							printMess(nm,11,f);
					}
					//if file is not found
					else
						printMess("FILE NOT FOUND",14,f);
				}
			}
			//else error is thrown
			else
				printMess(nm,1,f);
		}
		else
			printMess(";",14,f);
	}
	
	private static void exit(boolean f)
	{
		//printMess("",13,f);
		System.exit(0);
	}
	
	
	//a method that handles the switch statement for input and main
	private static void redirect(String orig, int methCall, boolean fout) throws IOException
	{
		try{
			if(methCall != 1 && methCall != 12 && methCall != 9 && methCall != 10 && methCall != 13 &&
					currentDB.getAbsolutePath().equals(directory.getAbsolutePath()))
				throw new OtherException("NO DATABASE IN USE",prwtr,fout);
			
			//a simple switch statement that to handle the redirection of cod
			switch (methCall)
			{
			case 1: create(orig, fout);
				break;
			case 2: describe(fout);
				break;
			case 3: select(fout, false);
				break;
			case 4: let(fout);
				break;
			case 5: insert(fout);
				break;
			case 6: update(fout);
				break;
			case 7: delete(fout);
				break;
			case 8: aggregate(fout);
				break;
			case 9: input(fout);
				break;
			case 10: exit(fout);
				break;
			//case 11: rename(orig, fout);
			case 11: rename(fout);
				break;
			case 12: use(fout);
				break;
			case 13: comment(orig, fout);
				break;
			default: error(orig, fout);
				break;
			}
		}
		catch(SyntaxException e)
		{
			if (!fout)
				System.out.println(e.getMessage());
		}
		catch (OtherException e)
		{
			if(!fout)
				System.out.println(e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			System.out.println(e.getMessage());
		}
		catch (FileNotFoundException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	//a method to handle the commands from the user
	private static int getCom(String hld)
	{
		int tmp;
		if(hld.equals("CREATE"))
			tmp = 1;
		else if(hld.equals("DESCRIBE"))
			tmp = 2;
		else if(hld.equals("SELECT"))
			tmp = 3;
		else if(hld.equals("LET"))
			tmp = 4;
		else if(hld.equals("INSERT"))
			tmp = 5;
		else if(hld.equals("UPDATE"))
			tmp = 6;
		else if(hld.equals("DELETE"))
			tmp = 7;
		else if(hld.equals("AGGREGATE"))
			tmp = 8;
		else if(hld.equals("INPUT"))
			tmp = 9;
		else if(hld.equals("EXIT"))
			tmp = 10;
		else if(hld.equals("RENAME"))
			tmp = 11;
		else if(hld.equals("USE"))
			tmp = 12;
		else if(hld.contains("/*"))
			tmp = 13;
		else
			tmp = 14;
		return tmp;
	}
	
	//a method to parse a relation operator statement
	private static String relOpCheck(String hld)
	{
		int numhld;
		boolean chr1 = false;//control variable
		String str1 = "", str2 = "", str3 = "";//holding strings
		//loop through every character of the passes string
		for(int i = 0; i < hld.length(); i++)
		{
			//gets the numeric value of the character
			numhld = (int)hld.charAt(i);
			//checks to see if character is anything other than a relational operator
			if(chr1 == false && ((numhld > 64 && numhld < 90) || (numhld > 96 && numhld < 123)))
				str1 = str1 + hld.charAt(i);
			//if it is a relational operator
			else if(numhld == 33 || (numhld > 59 && numhld < 63))
			{
				str2 = str2 + hld.charAt(i);
				chr1 = true;
			}
			//else it falls after a relational operator in the statement
			else
				str3 = str3 + hld.charAt(i);
		}
		//puts all the above strings together
		str1 = str1 + " " + str2 + " " + str3;
		//returns the above string
		return str1;
		
	}
	
	//a method to grab the inner string of create table statement
	private static String innerString(String tmp)
	{
		String rtrnSt = "";
		int num; //control variable
		boolean ch = false;//control variable
		//loops through the list to isolate the prevelent information
		for(int i = 0; i < tmp.length(); i++)
		{
			//numerical value of the character
			num = (int)tmp.charAt(i);
			if(ch == false)
			{
				if(num == 40)
					ch = true;
			}
			else
				rtrnSt = rtrnSt + tmp.charAt(i);
			
		}
		
		//second loop grab just the most prevelant information
		String tmp2 = "";
		for(int i  = 0; i < rtrnSt.length(); i++)
		{
			if(i < rtrnSt.length()-2)
				tmp2 = tmp2 + rtrnSt.charAt(i);
		}
		
		//returns the new string
		return tmp2;
	}
	//removes the semi colon from a string
	private static String remSemiColon(String hld)
	{
		String rvalue = "";
		//loops through till a semi colon
		for(int i = 0; i < hld.length(); i++)
		{
			if(hld.charAt(i) != 59)
				rvalue = rvalue + hld.charAt(i);
		}
		return rvalue;
	}
	
	//checks for a valid constant and returns the type
	private static String checkConstant(String constant)
	{
		if(constant.isEmpty())
			return "fail";
		
		//text constant
		if (constant.startsWith("\"") && constant.endsWith("\"") && constant.length() > 2)
		{
			String temp = constant.replaceAll("\"", "");
			if (temp.length() <= 100)
				return "text";
			else
				return "fail";
		}
		//number constant
		else if (Character.isDigit(constant.charAt(0)) || constant.charAt(0) == '-' || constant.charAt(0) == '-') 
		{
			//float constant
			if(constant.contains("."))
			{
				String [] validate = constant.split("\\.");

				if (validate.length != 2)
					return "fail";
				
				if (validate[1].contains("-") || validate[1].contains("-"))
					return "fail";
				
				//validate each side of the . as an int
				String int1 = checkConstant(validate[0]);
				String int2 = checkConstant(validate[1]);
				
				if (!int1.equals("int") || !int2.equals("int"))
					return "fail";
				else
					return "float";
			}
			//int constant
			else
			{
				int start = 0;
				
				if (constant.startsWith("-") || constant.startsWith("-"))
					start = 1;
				else
					start = 0;
				
				//validate digits starting after a potential negative sign
				for (int i = start; i<constant.length(); i++)
					if(!Character.isDigit(constant.charAt(i)))
						return "fail";
				
				if(Integer.parseInt(constant) < -2147483648 && Integer.parseInt(constant) > 2147483647)
					return "fail";
				
				return "int";
			}
		}
		else
		{
			return "fail";
		}
	}
	
	
	//checks for a valid identifier
	private static boolean check(String nm)
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
		if(nm.isEmpty())
			ch = false;
		//else it contains something
		else
		{
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
		}
		//returns a boolean true or false based on the above
		return ch;
	}
	
	private static void printMess(String fnd, int exp, boolean f)
	{
		String output = "";
		if(exp == 1)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"identifier\"";
		else if(exp == 2)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"int, float, text or identifier\"";
		else if(exp == 3)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"identifier LIST\"";
		else if(exp == 4)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"FROM\"";
		else if(exp == 5)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"<,>,>=,<=,!=,=\"";
		else if(exp == 6)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"WHERE\"";
		else if(exp == 7)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"AND/OR/INTO/VALUES\"";
		else if(exp == 8)
			output = "ERROR - FOUND \"" + fnd + "\". EXPECTING \"SELECT\"";
		else if(exp == 9)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"KEY\"";
		else if(exp == 10)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"TERMINAL\"";
		else if(exp == 11)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"OUTPUT\"";
		else if(exp == 12)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"DATABASE or TABLE\"";
		else if(exp == 13)
		{	
			if(fnd.equals("") || fnd.equals(" "))
				output = "OK";
			else
				output = fnd;
		}
		else if(exp == 14)
			output = "ERROR - FOUND \"" + fnd + "\", EXPECTING \"STRING\"";
		else
			output = "ERROR - FOUND \"" + fnd +"\", EXPECTING \"COMMENT\"";
		
		if(f == true)
		{
			if(exp != 13)
				prwtr.println(">"+wholeLine);
			prwtr.println(output);
		}
		else
		{
			if(exp != 13)
				System.out.println("> "+wholeLine);
			System.out.println(output);
		}
	}
	
	private static void error(String tmp, boolean f)
	{
		if(f == true)
		{
			//prwtr.println(">"+wholeLine);
			prwtr.println("ERROR - FOUND \"" + tmp + "\", EXPECTING: \"COMMAND\"");
		}
		else
			System.out.println("ERROR - FOUND \"" + tmp + "\", EXPECTING: \"COMMAND\"");
	}
	
	private static String peekLast(String currentToken)
	{
		String [] tokens = wholeLine.substring(wholeLine.lastIndexOf(currentToken)).split("\\s+");
		return tokens[1];
	}
	
	/**
	 * Checks the entire where condition for syntax validity and returns an array list
	 * of integers corresponding to record positions in the file that satisfy all
	 * where clauses.
	 *
	 * @param currentToken The current String token in the global tokenizer.
	 * @param f The eternally present boolean parameter for printing to a file.
	 * @return An array list of record positions that evaluate to true based on all where conditions.
	 * @throws SyntaxException the syntax exception
	 * @throws OtherException the other exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException 
	 */
	private static ArrayList<Integer> checkWhereCondition(String currentToken, boolean f) throws SyntaxException, OtherException, IOException, ClassNotFoundException
	{
		String temp = wholeLine;
		String whereList = temp.substring(temp.toUpperCase().indexOf(" WHERE ") + 7);
		StringTokenizer whereTokens = new StringTokenizer(whereList);
		int control = 0;
		String andOr1 = "", andOr2 = ""; //for storing an and or an or in the where condition
		
		whereConditions = new ArrayList<WhereCondition>();
		
		//build the rest of the where list while placing spaces between any AND or OR
		whereList = "";
		while (whereTokens.hasMoreTokens())
		{
			String s = whereTokens.nextToken();
			
			if (s.toUpperCase().equals("AND") || s.toUpperCase().equals("OR") 
				|| (s.endsWith(";") && (s.toUpperCase().substring(0,s.length()-1).equals("AND")) 
				|| s.toUpperCase().substring(0,s.length()-1).equals("OR")))
				whereList = whereList + " ";
			
			whereList = whereList + s;
			
			if (s.toUpperCase().equals("AND") || s.toUpperCase().equals("OR"))
				whereList = whereList + " ";
		}

		//create a new Tokenizer for the newly formed where condition
		whereTokens = new StringTokenizer(whereList, " \t\n\f\r<>=!;", true);
		String current = whereTokens.nextToken();
		
		//loop through the AND and OR cases while checking the validity of
		//each clause in the condition
		while(whereTokens.hasMoreTokens())
		{
			WhereCondition tempWC = new WhereCondition();
			
			//prevent more than three valid where clauses
			if(control >= 3)
				throw new SyntaxException(wholeLine,current, ";", prwtr, f);
			
			//check the identifier validity
			if(!check(current))
				throw new SyntaxException(wholeLine,current, "identifier", prwtr, f);
			if(!whereTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine,";", "operator", prwtr, f);
			
			tempWC.setIdentifier(current);
			
			//check operator validity with a second token for potentially a two
			//part operator
			current = whereTokens.nextToken();
			if(!whereTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine,";", "operator", prwtr, f);
			String potentialOpDelim = whereTokens.nextToken();
			boolean secondOp = false;
			
			//current token is an operator by itself
			if(opdelimiter(current))
			{
				if(current.equals("<") || current.equals(">"))
				{
					if(opdelimiter(current + potentialOpDelim))
					{
						tempWC.setOperator(current + potentialOpDelim);
						secondOp = true;
					}
					else
						tempWC.setOperator(current);
				}
				else
					tempWC.setOperator(current);
				
				if(!whereTokens.hasMoreTokens())
					throw new SyntaxException(wholeLine,";", "identifier or constant", prwtr, f);
			}
			//otherwise check the validity of the token as an operator together with its
			//potential secondary operator
			else
			{
				potentialOpDelim = current + potentialOpDelim;
				
				if(!opdelimiter(potentialOpDelim))
					throw new SyntaxException(wholeLine,potentialOpDelim, "operator", prwtr, f);
				if(!whereTokens.hasMoreTokens())
					throw new SyntaxException(wholeLine,";", "identifier or constant", prwtr, f);
				
				tempWC.setOperator(potentialOpDelim);
				secondOp = true;
			}

			String constant;
		
			//if there was a secondary part to the operator, get the next token as the constant
			if(secondOp)
			{
				current = whereTokens.nextToken();
				constant = checkConstant(current);
				tempWC.setConstant(current);
			}
			//otherwise use the already grabbed potential operator as the constant token
			else
			{
				current = potentialOpDelim;
				constant = checkConstant(potentialOpDelim);
				tempWC.setConstant(potentialOpDelim);
			}
			
			//check constant/identifier validity
			if(constant.equals("fail") && !check(current))
				throw new SyntaxException(wholeLine,current, "identifier or constant", prwtr, f);
			if(!whereTokens.hasMoreTokens())
				throw new SyntaxException(wholeLine,";", "identifier or constant", prwtr, f);
			
			whereConditions.add(tempWC);
			
			current = whereTokens.nextToken();
			
			//assumes a space will lead to an AND, OR, or ;
			if(current.equals(" "))
			{
				current = whereTokens.nextToken().toUpperCase();
				
				if(control == 0)
					andOr1 = current;
				else if(control == 1)
					andOr2 = current;
				
				//in any valid AND or OR state, continue the loop for next where clause
				if(current.equals("AND") || current.equals("OR") || current.equals(";"))
				{
					current = whereTokens.nextToken();
					
					if(!whereTokens.hasMoreTokens())
						throw new SyntaxException(wholeLine,";", "identifier", prwtr, f);
					current = whereTokens.nextToken();
					control++;
					continue;
				}
				else
					throw new SyntaxException(wholeLine,current, "AND, OR, or semicolon", prwtr, f);
			}
			//if a semicolon is reached, condition is considered valid, so exit the loop
			else if(current.equals(";"))
			{
				break;
			}
			else
				throw new SyntaxException(wholeLine,current, "AND, OR, or semicolon", prwtr, f);
		}
		
		ArrayList<Integer> positions = new ArrayList<Integer>();
		
		control = 0;
		ArrayList<Integer> previousIntArray = new ArrayList<Integer>();
		//ArrayList<Integer> removedElements = new ArrayList<Integer>();
		
		for(WhereCondition wc : whereConditions)
		{
			ArrayList<Integer> tempIntArray = new ArrayList<Integer>();
			tempIntArray = getWherePositions(tableNames.get(0), wc, f);
			
			//boolean removeElements = (control == 1 && andOr1.equals("AND")) || (control == 2 && andOr2.equals("AND"));
			
			if((control == 1 && andOr1.equals("AND")) || (control == 2 && andOr2.equals("AND")))
			{
				if(tempIntArray.isEmpty() && previousIntArray.isEmpty())
				{
					continue;
				}
				else
				{
					int maxArraySize;
					
					if(tempIntArray.isEmpty())
						maxArraySize = Collections.max(previousIntArray);
					else if(previousIntArray.isEmpty())
						maxArraySize = Collections.max(tempIntArray);
					else
						maxArraySize = Math.max(Collections.max(tempIntArray), Collections.max(previousIntArray));
					
					for(int i = 1; i <= maxArraySize;i++)
					{
						if(tempIntArray.contains(i) && !previousIntArray.contains(i))
						{
							tempIntArray.remove((Integer)i);
							positions.remove((Integer)i);
						}
						else if(!tempIntArray.contains(i) && previousIntArray.contains(i))
						{
							positions.remove((Integer)i);
						}
					}
				}
			}
				
			for(int i : tempIntArray)
			{
				
				if(!positions.contains(i))
				{
					positions.add(i);
				}
			}
			previousIntArray = tempIntArray;
			control++;
		}
		
		return positions;
	}

	//public static ArrayList<Integer> getWherePositions(String tableName, ArrayList<WhereCondition> whereConds) throws OtherException, IOException
	private static ArrayList<Integer> getWherePositions(String tableName, WhereCondition whereCond, boolean f) throws OtherException, IOException, ClassNotFoundException
	{
		ArrayList<Integer> positions = new ArrayList<Integer>();
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".dat");
    	File treeFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".itf");
    	File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
    	
    	if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
    	
    	ObjectInputStream treeStream = new ObjectInputStream(new FileInputStream(treeFile));
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
    	RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	BinarySearchTree indexTree = new BinarySearchTree();
    	indexTree = (BinarySearchTree)treeStream.readObject();
    	treeStream.close();
    	
    	//store the size of a record
    	int recordLength = Integer.parseInt(metaInfo.nextLine());
    	metaInfo.close();
    	
    	//store the file length and the number of records in the file
    	long dataFileLength = tableAccess.length();
    	long totalFileRecords = dataFileLength/recordLength;
    	
		boolean found = false;
		boolean checkIdentifier = false;
		String identifierValue = "";
		
		tableAttributes = getTableAttributes(tableName, f);
		
		//set whether the right half of the where clause is an identifier
		if(whereCond.rightHalfIsIdentifier())
		{
			checkIdentifier = true;
		}
		
		//loop through all the file records
		for(int i = 0; i < totalFileRecords; i++)
		{
			found = false;
			tableAccess.seek(i*recordLength);
			
			//read the deletion marker
			char deletionMarker = (char)tableAccess.read();
			
			//if the right half of the where clause was an identifier, grab its value from the table
			if(checkIdentifier)
			{
				identifierValue = getValue(tableName, whereCond.getConstant(), i, f);
			}
			
			for(TableAttribute currentAttribute : tableAttributes)
			{
				int domainLength = currentAttribute.getDomainLength();
				String name = currentAttribute.getName();
				
				//current attribute is an int
				if(domainLength == 4)
				{
					//only update a value if the attribute name equals the entered identifier
					if(name.equals(whereCond.getIdentifier()) && deletionMarker != 'D')
	    			{
						int value = tableAccess.readInt();
						
						if(!checkConstant(whereCond.getConstant()).equals("int") && identifierValue.equals(""))
						{
							tableAccess.close();
							throw new OtherException("Domain type mismatch: "+whereCond.getConstant(), prwtr,f);
						}
						if(identifierValue.equals("") && whereOperation(value, Integer.parseInt(whereCond.getConstant()), whereCond.getOperator()))
							found = true;
						if(!identifierValue.equals("") && whereOperation(value, Integer.parseInt(identifierValue), whereCond.getOperator()))
							found = true;
	    			}
					//otherwise skip over
					else
					{
						tableAccess.readInt();
					}
				}
				//current attribute is a float
				else if(domainLength == 8)
				{
					//only update a value where the attribute name equals the entered identifier
					if(name.equals(whereCond.getIdentifier()) && deletionMarker != 'D')
	    			{
						double value = tableAccess.readDouble();
						
						if(!checkConstant(whereCond.getConstant()).equals("float") && identifierValue.equals(""))
						{
							tableAccess.close();
							throw new OtherException("Domain type mismatch: "+whereCond.getConstant(), prwtr,f);
						}
						if(identifierValue.equals("") && whereOperation(value, Double.parseDouble(whereCond.getConstant()), whereCond.getOperator()))
							found = true;
						if(!identifierValue.equals("") && whereOperation(value, Double.parseDouble(identifierValue), whereCond.getOperator()))
							found = true;
	    			}
					//otherwise skip over
					else
					{
						tableAccess.readDouble();
					}
				}
				//current attribute is text
				else
				{
					//only update a value where the attribute name equals the entered identifier
					if(name.equals(whereCond.getIdentifier()) && deletionMarker != 'D')
	    			{
						String value = "";
						byte [] b = new byte[100];
						tableAccess.read(b);
						
						//String newValue = new String(b);
						
						value = new String(b);
						//remove unnecessary \b characters
						//newValue = newValue.replaceAll("\b", "");
						value = value.replaceAll("\b", "");
    					
    					//value = newValue;
    					
    					if(!checkConstant(whereCond.getConstant()).equals("text") && identifierValue.equals(""))
    					{
    						tableAccess.close();
							throw new OtherException("Domain type mismatch: "+whereCond.getConstant(), prwtr,f);
    					}
    					if(identifierValue.equals("") && whereOperation(value, whereCond.getConstant(), whereCond.getOperator()))
    						found = true;
    					if(!identifierValue.equals("") && whereOperation(value, identifierValue, whereCond.getOperator()))
							found = true;
	    			}
					//otherwise skip over
					else
					{
						byte [] b = new byte[100];
						tableAccess.read(b);
					}
				}
			}
			
			if(found && deletionMarker != 'D')
				positions.add(i+1); //add one for skipping the attribute names
		}
		tableAccess.close();
	    Collections.sort(positions);
		return positions;
	}
	
	//returns an attribute value from a specified table using the given pointer
	private static String getValue(String tableName, String attributeName, long pointer, boolean f) throws IOException, OtherException
	{
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".dat");
		File metaFile = new File(currentDB.getAbsolutePath()+"/"+tableNames.get(0)+".mtd");
		
		if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
		
		RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
    	Scanner metaInfo = new Scanner(new FileInputStream(metaFile));
		int attributePosition = 0;
		String value = "";

		int recordLength = Integer.parseInt(metaInfo.nextLine());
		metaInfo.close();
		
		if(attributePosition == -1)
		{
			tableAccess.close();
			throw new OtherException("Attribute name not in table.", prwtr, f);
		}
		
		tableAccess.seek(pointer*recordLength);
		
		char deletionMarker = (char)tableAccess.read();
		
		for(TableAttribute currentAttribute : tableAttributes)
		{
			int domainLength = currentAttribute.getDomainLength();
			String name = currentAttribute.getName();
			
			//current attribute is an int
			if(domainLength == 4)
			{
				//only update a value the entered identifier
				if(name.equals(attributeName) && deletionMarker != 'D')
    			{
					value = Integer.toString(tableAccess.readInt());
					break;
    			}
				//otherwise skip over
				else
				{
					tableAccess.readInt();
				}
			}
			//current attribute is a float
			else if(domainLength == 8)
			{
				//only update a value the entered identifier
				if(name.equals(attributeName) && deletionMarker != 'D')
    			{
					value = Double.toString(tableAccess.readDouble());
					break;
    			}
				//otherwise skip over
				else
				{
					tableAccess.readDouble();
				}
			}
			//current attribute is text
			else
			{
				//only update a value the entered identifier
				if(name.equals(attributeName) && deletionMarker != 'D')
    			{	
					byte [] b = new byte[100];
					tableAccess.read(b);
					
					String newValue = new String(b);
					
					newValue = newValue.replaceAll("\b", "");
					
					value = newValue;
					break;
    			}
				//otherwise skip over
				else
				{
					byte [] b = new byte[100];
					tableAccess.read(b);
				}
			}
		}
		tableAccess.close();
		
		return value;
	}
	
	private static void setValue(String tableName, String updateAttributeName, String newValue, long pointer, int recordLength, char delete, boolean f) throws IOException, OtherException, ClassNotFoundException
	{
		File dataFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".dat");
		
		if(!dataFile.exists())
    		throw new OtherException("NO SUCH TABLE EXISTS",prwtr,f);
		
		RandomAccessFile tableAccess = new RandomAccessFile(dataFile, "rw");
		File treeFile = new File(currentDB.getAbsolutePath()+"/"+tableName+".itf");
		ObjectInputStream treeStream = new ObjectInputStream(new FileInputStream(treeFile));
    	BinarySearchTree indexTree = new BinarySearchTree();
    	indexTree = (BinarySearchTree)treeStream.readObject();
    	treeStream.close();
    
		String newValueDomain = checkConstant(newValue);
	
		//attributePosition = getAttributePosition(tableName, updateAttributeName);
		tableAttributes = getTableAttributes(tableName, f);
		
		for(TableAttribute testAttribute : tableAttributes)
		{
			if(testAttribute.getName().equals(updateAttributeName))
			{
				if(!newValueDomain.equals(testAttribute.getDomain()) && !testAttribute.getDomain().equals("integer"))
				{
					throw new OtherException("Expecting domain: "+testAttribute.getDomain()+". Found: "+newValueDomain,prwtr,f);
				}
				else if(testAttribute.getDomain().equals("integer") && !newValueDomain.equals("int"))
				{
					throw new OtherException("Expecting domain: "+testAttribute.getDomain()+". Found: "+newValueDomain,prwtr,f);
				}
			}
		}
		
		//if(attributePosition == -1)
		//	throw new OtherException("Attribute name not in table.", prwtr, false);
		
		tableAccess.seek(pointer*recordLength);
		
		char deletionMarker = 'n';
		
		//the delete parameter was set to 'D', write it to the deletion marker byte
		if(delete == 'D')
		{
			tableAccess.writeByte(delete);
		}
		//otherwise read the byte
		else
			deletionMarker = (char)tableAccess.read();

		//loop through the table attributes to get to the attribute to update
		for(TableAttribute currentAttribute : tableAttributes)
		{		
			int domainLength = currentAttribute.getDomainLength();
			String name = currentAttribute.getName();
			
			//current attribute is an int
			if(domainLength == 4)
			{		
				//only update a value the entered identifier
				if(name.equals(updateAttributeName) && deletionMarker != 'D')
    			{
					if(!newValueDomain.equals("int"))
					{
						tableAccess.close();
						throw new OtherException("Expecting domain: integer. Found: "+newValueDomain,prwtr,f);
					}
					tableAccess.writeInt(Integer.parseInt(newValue));
    			}
				else if(currentAttribute.isPrimaryKey() && delete == 'D')
				{
					indexTree.delete(Integer.toString(tableAccess.readInt()));
				}
				//otherwise skip over
				else
				{
					tableAccess.readInt();
				}
			}
			//current attribute is a float
			else if(domainLength == 8)
			{	
				//only update a value the entered identifier
				if(name.equals(updateAttributeName) && deletionMarker != 'D')
    			{
					if(!newValueDomain.equals("float"))
					{
						tableAccess.close();
						throw new OtherException("Expecting domain: float. Found: "+newValueDomain,prwtr,f);
					}
					tableAccess.writeDouble(Double.parseDouble(newValue));
    			}
				else if(currentAttribute.isPrimaryKey() && deletionMarker == 'D')
				{
					indexTree.delete(Double.toString(tableAccess.readDouble()));
				}
				//otherwise skip over
				else
				{
					tableAccess.readDouble();
				}
			}
			//current attribute is text
			else
			{		
				//only update a value the entered identifier
				if(name.equals(updateAttributeName) && deletionMarker != 'D')
    			{		
					if(!newValueDomain.equals("text"))
					{
						tableAccess.close();
						throw new OtherException("Expecting domain: text. Found: "+newValueDomain,prwtr,f);
					}
					
					tableAccess.writeBytes(newValue);
					
					for(int i = 0; i < 100 - newValue.length();i++)
					{
						tableAccess.writeByte('\b');
					}
    			}
				else if(currentAttribute.isPrimaryKey() && deletionMarker == 'D')
				{
					byte [] b = new byte[100];
					tableAccess.read(b);
					String value = new String (b);
					
					indexTree.delete(value);
				}
				//otherwise skip over
				else
				{
					byte [] b = new byte[100];
					tableAccess.read(b);
				}
			}
		}
		tableAccess.close();
		
		ObjectOutputStream treeOutStream = new ObjectOutputStream(new FileOutputStream(treeFile));
		treeOutStream.writeObject(indexTree);
		treeOutStream.close();
	}
	
	public static boolean whereOperation(String value1, String value2, String op) throws OtherException
	{
		if(op.equals("="))
			return value1.equals(value2);
		else if(op.equals("!="))
			return !value1.equals(value2);
		else
			throw new OtherException("Invalid operator error.", prwtr, false);
	}
	
	public static boolean whereOperation(int value1, int value2, String op) throws OtherException
	{
		if(op.equals("="))
			return value1 == value2;
		else if(op.equals("<"))
			return value1 < value2;
		else if(op.equals(">"))
			return value1 > value2;
		else if(op.equals("<="))
			return value1 <= value2;
		else if(op.equals(">="))
			return value1 >= value2;
		else if(op.equals("!="))
			return value1 != value2;
		else
			throw new OtherException("Invalid operator error.", prwtr, false);
	}
	
	public static boolean whereOperation(double value1, double value2, String op) throws OtherException
	{
		if(op.equals("="))
			return value1 == value2;
		else if(op.equals("<"))
			return value1 < value2;
		else if(op.equals(">"))
			return value1 > value2;
		else if(op.equals("<="))
			return value1 <= value2;
		else if(op.equals(">="))
			return value1 >= value2;
		else if(op.equals("!="))
			return value1 != value2;
		else
			throw new OtherException("Invalid operator error.", prwtr, false);
	}

	public static boolean opdelimiter(String s) {
	   return (s.equals(">") || s.equals("<") || s.equals(">=") || s.equals("<=") || s.equals("=") || s.equals("!="));
	}
}
