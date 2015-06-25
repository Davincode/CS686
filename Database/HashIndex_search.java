import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Build the Hash Index, with the search key as its key, 
// and the positions of matching values in the data file as its value

public class HashIndex {
	
	private HashMap<String, List<Integer>> map;
	private String database;
	
	public HashIndex(String database)
	{
		this.map = new HashMap<String, List<Integer>>();
		this.database = database;
	}
	
	public void readCSV(String csv, String table, int fixed_size)
	{
		try {
			File csv_file = new File(csv);
			if(!csv_file.exists()) 
			{
				return;
			}

			File file = new File(database+ "_" + table);
			if(file.exists())
			{
				System.out.println("Already load the csv file into database");
				return;
			}
			
			Scanner scanner = new Scanner(csv_file);
			scanner.useDelimiter(",");
			FileWriter fw = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(fw);
			
			String firstLine = scanner.nextLine();
			String[] columns = firstLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			int num_columns = columns.length;
			
			for(int i = 0; i < columns.length; i++)
			{
				String current = columns[i];

				// remove the double quotes if necessary
				int first_quote = current.indexOf("\"");
				int second_quote = current.indexOf("\"", first_quote + 1);
				
				if(first_quote != -1 && second_quote != -1)
				{
					current = current.substring(first_quote + 1, second_quote);
				}
				
				if(current.length() < fixed_size)
				{
					writer.write(current);
					for(int j = 0; j < fixed_size - 1 - current.length(); j++)
					{
						// add the white spaces to make the size fixed
						writer.write(" ");
					}
				}
				else
				{
					writer.write(current.substring(0, fixed_size - 1));
				}
				
				if(i < columns.length - 1)
				{
					writer.write("|");
				}
			}
			writer.write("\n");
			
			
			while (scanner.hasNext()) {
				String[] row = scanner.nextLine().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				for(int i = 0; i < row.length; i++)
				{
					String current = row[i];
					
					// remove the double quotes if necessary
					int first_quote = current.indexOf("\"");
					int second_quote = current.indexOf("\"", first_quote + 1);
					
					if(first_quote != -1 && second_quote != -1)
					{
						current = current.substring(first_quote + 1, second_quote);
					}
					
					if(current.length() < fixed_size)
					{
						writer.write(current);
						for(int j = 0; j < fixed_size - 1 - current.length(); j++)
						{
							// add the white spaces to make the size fixed
							writer.write(" ");
						}
					}
					else
					{
						writer.write(current.substring(0, fixed_size - 1));
					}
					
					if(i < row.length - 1)
					{
						writer.write("|");
					}
				}
				
				// in case the columns in the tail are empty
				// in that way, there is no comma to separate them
				// but we still need to make this row fixed-sized
				if(row.length < num_columns)
				{
					for(int i = 0; i < num_columns - row.length; i++)
					{
						for(int j = 0; j < fixed_size - 1; j++)
						{
							// add the white spaces to make the size fixed
							writer.write(" ");
						}
						
						if(i < num_columns - row.length)
						{
							writer.write("|");
						}
					}
				}
				writer.write("\n");
			}
			writer.close();
			scanner.close();
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void build(String table, String Index, int fixed_size)
	{
		File file = new File(database + "_" + table);
		if(!file.exists())
		{
			return;
		}
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader reader = new BufferedReader(fr);
			String[] names = reader.readLine().split("\\|");
			int pos = -1;
			for(int i = 0; i < names.length; i++)
			{
				String name = names[i];
				if(name.length() < fixed_size)
				{
					// remove the white spaces
					for(int j = name.length() - 1; j >= 0; j--)
					{
						if(!name.substring(j, j + 1).equals(" "))
						{
							name = name.substring(0, j + 1);
							break;
						}
					}
				}
				else
				{
					name = name.substring(0, fixed_size - 1);
				}
				
				if(name.equals(Index))
				{
					pos = i; 
					break;
				}
			}
			// no such key, cannot build the index
			if(pos == -1) 
			{
				System.out.println("No such key : " + Index);
				reader.close();
				return;
			}
			
			// clear the map before creating a new one
			map.clear();
			String line = reader.readLine();
			int count = 1;
			while(line != null)
			{
				String column = line.split("\\|")[pos];
				if(column.length() < fixed_size)
				{
					// remove the white spaces
					for(int j = column.length() - 1; j >= 0; j--)
					{
						if(!column.substring(j, j + 1).equals(" "))
						{
							column = column.substring(0, j + 1);
							break;
						}
					}
				}
				else
				{
					column = column.substring(0, fixed_size - 1);
				}
				
				if(map.containsKey(column))
				{
					List<Integer> temp = map.get(column);
					temp.add(count);
					map.put(column, temp);
				}
				else
				{
					List<Integer> temp = new ArrayList<Integer>();
					temp.add(count);
					map.put(column, temp);
				}
				line = reader.readLine();
				count += 1;
			}
			reader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dump(String table, String Index)
	{
		try {
			File file = new File(database + "_" + table + "_" + Index);  
			if(file.exists()) 
			{
				System.out.println("Already Built the index");
				return;
			}
			FileOutputStream f = new FileOutputStream(file);  
			ObjectOutputStream s = new ObjectOutputStream(f);     
			s.writeObject(map);
			s.flush();
			s.close();
			
			System.out.println("Built index for key " + Index);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void load(String table, String Index)
	{
		map.clear();
		try {
			File file = new File(database + "_" + table + "_" + Index);  
			if(!file.exists()) 
			{
				System.out.println("Such Index not built yet");
				return;
			}
			FileInputStream f = new FileInputStream(file);  
			ObjectInputStream s = new ObjectInputStream(f);     
			map = (HashMap<String, List<Integer>>)s.readObject();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public String search(String key, String table, int fixed_size)
	{
		StringBuffer buffer = new StringBuffer();
		try {
			File file = new File(database + "_" + table);
			if(map.containsKey(key) && file.exists())
			{
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				String line = raf.readLine();
				String[] attributes = line.split("\\|");
				int num_column = attributes.length;
				for(int i = 0; i < attributes.length; i++)
				{
					String attribute = attributes[i];
					for(int j = attribute.length() - 1; j >= 0; j--)
					{
						if(!attribute.substring(j, j + 1).equals(" "))
						{
							attribute = attribute.substring(0, j + 1);
							break;
						}
					}
					buffer.append(attribute);
					if(i < attributes.length - 1)
					{
						buffer.append("|");
					}
				}
				buffer.append("\n");
				buffer.append("\n");
				
				List<Integer> index = map.get(key);
				for(int i = 0; i < index.size(); i++)
				{
					// fixed size
					long position = (long)index.get(i) * fixed_size * num_column;
					raf.seek(position);
					String current = raf.readLine();
					String[] columns = current.split("\\|");
					
					for(int j = 0; j < columns.length; j++)
					{
						String column = columns[j];
						// if it is an empty column, we use a single white space to represent it
						// instead of printing nothing
						boolean flag = true;
						// remove the white spaces
						for(int k = column.length() - 1; k >= 0; k--)
						{
							if(!column.substring(k, k + 1).equals(" "))
							{
								column = column.substring(0, k + 1);
								flag = false;
								break;
							}
						}
						
						if(flag) 
						{
							buffer.append(" ");
						}
						else 
						{
							buffer.append(column);
						}
						
						if(j < columns.length - 1)
						{
							buffer.append("|");
						}
					}
					
					// since there is no | in the end 
					// the last column could be ignored if it is empty
					if(columns.length < num_column)
					{
						buffer.append("|");
					}
					
					buffer.append("\n");
					buffer.append("\n");
				}
				raf.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(buffer);
	}
	
	public static void main(String[] args)
	{
		String database_name = new String();
		if(args.length == 1) 
		{
			database_name = args[0];
		}
		else if(args.length == 3 && args[0].equals("-init"))
		{
			database_name = args[2];
		}
		else
		{
			return;
		}
		
		HashIndex hashindex = new HashIndex(database_name);
		
		int fixed_size = 255;
		int big_file_fixed_size = 63;
		
		// Hard code the table_name, csv_name and Index to be built
		// Table : Payments
		// Size : 1.41 GB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		String csv = "OPPR_ALL_DTL_GNRL_12192014.csv";
		String Table = "Payments";
		String Index = "Physician_Last_Name";
		
		System.out.println("Building index for table " + Table);
		hashindex.readCSV(csv, Table, big_file_fixed_size);
		hashindex.build(Table, Index, big_file_fixed_size);
		hashindex.dump(Table, Index);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		hashindex.build(Table, Index, big_file_fixed_size);
		hashindex.dump(Table, Index);
		System.out.println();
		
		// Table : Physicians
		// Size : 77.5 MB
		// Index : Physician_Profile_Last_name
		csv = "OPPR_SPLMTL_PH_PRFL_12192014.csv";
		Table = "Physicians";
		Index = "Physician_Profile_Last_Name";
		
		System.out.println("Building index for table " + Table);
		hashindex.readCSV(csv, Table, fixed_size);
		hashindex.build(Table, Index, fixed_size);
		hashindex.dump(Table, Index);
		System.out.println();
		
		// Table : Ownership
		// Size : 1.5 MB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		csv = "OPPR_ALL_DTL_OWNRSHP_12192014.csv";
		Table = "Ownership";
		Index = "Physician_Last_Name";
		
		System.out.println("Building index for table " + Table);
		hashindex.readCSV(csv, Table, fixed_size);
		hashindex.build(Table, Index, fixed_size);
		hashindex.dump(Table, Index);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		hashindex.build(Table, Index, fixed_size);
		hashindex.dump(Table, Index);
		System.out.println();
		
		// Table : Research
		// Size : 13.7 MB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		csv = "OPPR_ALL_DTL_RSRCH_12192014.csv";
		Table = "Research";
		Index = "Physician_Last_Name";
		
		System.out.println("Building index for table " + Table);
		hashindex.readCSV(csv, Table, fixed_size);
		hashindex.build(Table, Index, fixed_size);
		hashindex.dump(Table, Index);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		hashindex.build(Table, Index, fixed_size);
		hashindex.dump(Table, Index);
		System.out.println();
		
		// Query part
		if(args.length == 1)
		{
			System.out.println("You may now start to query :");
			while(true)
			{
				Scanner scan = new Scanner(System.in);
				String line = scan.nextLine();
				
				Pattern p = Pattern.compile("\\w+\\s+.\\s+\\w+\\s+\\w+\\s+\\w+\\s+\\w+\\s*=\\s*\"*.+\"*");
				Matcher m = p.matcher(line);
				if(!m.matches())
				{
					System.out.println("Syntax Error");
				}
				else
				{
					String index = new String();
					String key = new String();
					String table = new String();
					
					String[] tokens = line.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					
					if(tokens.length == 6)
					{
						String temp = tokens[5];
						int pos = temp.indexOf("=");
						index = temp.substring(0, pos).replaceAll(" ", "");
						key = temp.substring(pos + 1);
						table = tokens[3];
					}
					else if(tokens.length == 8)
					{
						index = tokens[5];
						key = tokens[7];
						table = tokens[3];
					}
					
					int first_quote = key.indexOf("\"");
					int second_quote = key.indexOf("\"", first_quote + 1);
					
					if(first_quote != -1 && second_quote != -1)
					{
						key = key.substring(first_quote + 1, second_quote);
					}
					
					
					File file = new File(database_name + "_" + table + "_" + index);
					if(file.exists())
					{
						hashindex.load(table, index);
						if(table.equals("Payments"))
						{
							System.out.print(hashindex.search(key, table, big_file_fixed_size));
						}
						else
						{
							System.out.print(hashindex.search(key, table, fixed_size));
						}
					}
					System.out.println("==============================");
				}
			}
		}
		else if(args.length == 3 && args[0].equals("-init"))
		{
			try
			{
				File input_file = new File(args[1]);
				if(!input_file.exists())
				{
					return;
				}
				FileReader fr = new FileReader(input_file);
				BufferedReader reader = new BufferedReader(fr);
				String line = reader.readLine();
				while(line != null)
				{
					Pattern p = Pattern.compile("\\w+\\s+.\\s+\\w+\\s+\\w+\\s+\\w+\\s+\\w+\\s*=\\s*\"*.+\"*");
					Matcher m = p.matcher(line);
					if(!m.matches())
					{
						System.out.println("Syntax Error");
					}
					else
					{
						String index = new String();
						String key = new String();
						String table = new String();
						
						String[] tokens = line.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
						
						if(tokens.length == 6)
						{
							String temp = tokens[5];
							int pos = temp.indexOf("=");
							index = temp.substring(0, pos).replaceAll(" ", "");
							key = temp.substring(pos + 1);
							table = tokens[3];
						}
						else if(tokens.length == 8)
						{
							index = tokens[5];
							key = tokens[7];
							table = tokens[3];
						}
						
						int first_quote = key.indexOf("\"");
						int second_quote = key.indexOf("\"", first_quote + 1);
						
						if(first_quote != -1 && second_quote != -1)
						{
							key = key.substring(first_quote + 1, second_quote);
						}
						
						
						File file = new File(database_name + "_" + table + "_" + index);
						if(file.exists())
						{
							hashindex.load(table, index);
							if(table.equals("Payments"))
							{
								System.out.print(hashindex.search(key, table, big_file_fixed_size));
							}
							else
							{
								System.out.print(hashindex.search(key, table, fixed_size));
							}
						}
						System.out.println("==============================");
					}
					line = reader.readLine();
				}
				reader.close();
				
			} catch ( IOException e ) {
		    	e.printStackTrace();
		    }
		}
	}
}