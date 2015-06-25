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

public class HashIndex {
	
	private String database;
	// fixed length : num_columns * (fixed_size - 1) + separator
	private int fixed_size = 255;
	private int big_file_fixed_size = 63;
	
	public HashIndex(String database)
	{
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
				System.out.println("Already put the csv file into database");
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
	
	public HashMap<String, List<Integer>> build(String table, String Index, int fixed_size)
	{
		HashMap<String, List<Integer>> map = new HashMap<String, List<Integer>>();
		File file = new File(database + "_" + table);
		if(!file.exists())
		{
			return map;
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
				return map;
			}
			
			String line = reader.readLine();
			int count = 1;
			while(line != null)
			{
				String column = line.split("\\|")[pos];
				// we already knew that column.length() == fixed_size - 1, but we choose to check again here
				if(column.length() < fixed_size)
				{
					// remove the white spaces
					int last_position_not_empty = 0;
					for(int j = column.length() - 1; j >= 0; j--)
					{
						if(!column.substring(j, j + 1).equals(" "))
						{
							last_position_not_empty = j;
							break;
						}
					}
					column = column.substring(0, last_position_not_empty + 1);
				}
				else
				{
					column = column.substring(0, fixed_size - 1);
				}
				
				// in this particular case, we choose not to handle empty value
				if(column.length() == 0) 
				{
					line = reader.readLine();
					count += 1;
					continue;
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
		
		return map;
	}
	
	public void dump(String table, String Index, HashMap<String, List<Integer>> map)
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
	public HashMap<String, List<Integer>> load(String table, String Index)
	{
		HashMap<String, List<Integer>> map = null;
		try {
			File file = new File(database + "_" + table + "_" + Index);  
			if(!file.exists()) 
			{
				System.out.println("Such Index not built yet");
			}
			else
			{
				FileInputStream f = new FileInputStream(file);  
				ObjectInputStream s = new ObjectInputStream(f);     
				map = (HashMap<String, List<Integer>>)s.readObject();
				s.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	public void join(String table_inner, HashMap<String, List<Integer>> index_inner,
			String table_outer, HashMap<String, List<Integer>> index_outer)
	{
		StringBuffer buffer = new StringBuffer();
		try {
			File file_inner = new File(database + "_" + table_inner);
			File file_outer = new File(database + "_" + table_outer);
			if(file_inner.exists() && file_outer.exists())
			{
				RandomAccessFile raf_1 = new RandomAccessFile(file_inner, "r");
				String line = raf_1.readLine();
				String[] attributes = line.split("\\|");
				int num_column_1 = attributes.length;
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
					buffer.append("|");
				}
				
				RandomAccessFile raf_2 = new RandomAccessFile(file_outer, "r");
				line = raf_2.readLine();
				attributes = line.split("\\|");
				int num_column_2 = attributes.length;
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
				
				System.out.println(buffer.toString());

				int count = 0;
				for(String key : index_outer.keySet())
				{
					if(index_inner.containsKey(key))
					{
						List<Integer> positions_1 = index_inner.get(key);
						List<Integer> positions_2 = index_outer.get(key);
						
						count += positions_1.size() * positions_2.size();
		 				for(int i = 0; i < positions_1.size(); i++)
						{
		 					for(int j = 0; j < positions_2.size(); j++)
		 					{
		 						buffer = new StringBuffer();
//		 						if(table_inner.equals("Payments"))
//		 						{
//		 							output(raf_1, positions_1.get(i), num_column_1, buffer, big_file_fixed_size);
//		 						}
//		 						else
//		 						{
//		 							output(raf_1, positions_1.get(i), num_column_1, buffer, fixed_size);
//		 						}
//		 						buffer.append("|");
//		 						
//		 						if(table_outer.equals("Payments"))
//		 						{
//		 							output(raf_2, positions_2.get(j), num_column_2, buffer, big_file_fixed_size);
//		 						}
//		 						else
//		 						{
//		 							output(raf_2, positions_2.get(j), num_column_2, buffer, fixed_size);
//		 						}
//								buffer.append("\n");
//								buffer.append("\n");
//								System.out.println(new String(buffer));
		 					}
						}
					}
				}
				System.out.println(count);
				raf_1.close();
				raf_2.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void output(RandomAccessFile raf, int num_line, int num_column, StringBuffer buffer, int size)
	{
		try {
			// fixed size
			long position = (long)num_line * size * num_column;
			raf.seek(position);
			
			String current = raf.readLine();
			String[] columns = current.split("\\|");
			
			for(int m = 0; m < columns.length; m++)
			{
				String column = columns[m];
				// if it is an empty column, we use a single white space to represent it
				// instead of printing nothing
				boolean flag = true;
				// remove the white spaces
				int last_position_not_empty = 0;
				for(int k = column.length() - 1; k >= 0; k--)
				{
					if(!column.substring(k, k + 1).equals(" "))
					{
						//column = column.substring(0, k + 1);
						last_position_not_empty = k;
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
					column = column.substring(0, last_position_not_empty + 1);
					buffer.append(column);
				}
				
				if(m < columns.length - 1)
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Hard code the table_name, csv_name and Index to be built
	public void buildIndex()
	{
		// Table : Payments
		// Size : 1.41 GB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		String csv = "OPPR_ALL_DTL_GNRL_12192014.csv";
		String Table = "Payments";
		String Index = "Physician_Last_Name";
		HashMap<String, List<Integer>> map;
		
		System.out.println("Building index for table " + Table);
		readCSV(csv, Table, big_file_fixed_size);
		map = build(Table, Index, big_file_fixed_size);
		dump(Table, Index, map);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		map = build(Table, Index, big_file_fixed_size);
		dump(Table, Index, map);
		System.out.println();
		
		// Table : Physicians
		// Size : 77.5 MB
		// Index : Physician_Profile_Last_name
		csv = "OPPR_SPLMTL_PH_PRFL_12192014.csv";
		Table = "Physicians";
		Index = "Physician_Profile_Last_Name";
		
		System.out.println("Building index for table " + Table);
		readCSV(csv, Table, fixed_size);
		map = build(Table, Index, fixed_size);
		dump(Table, Index, map);
		System.out.println();
		
		// Table : Ownership
		// Size : 1.5 MB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		csv = "OPPR_ALL_DTL_OWNRSHP_12192014.csv";
		Table = "Ownership";
		Index = "Physician_Last_Name";
		
		System.out.println("Building index for table " + Table);
		readCSV(csv, Table, fixed_size);
		map = build(Table, Index, fixed_size);
		dump(Table, Index, map);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		map = build(Table, Index, fixed_size);
		dump(Table, Index, map);
		System.out.println();
		
		// Table : Research
		// Size : 13.7 MB
		// Index : Physician_Last_name, Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name
		csv = "OPPR_ALL_DTL_RSRCH_12192014.csv";
		Table = "Research";
		Index = "Physician_Last_Name";
		
		System.out.println("Building index for table " + Table);
		readCSV(csv, Table, fixed_size);
		map = build(Table, Index, fixed_size);
		dump(Table, Index, map);
		
		Index = "Applicable_Manufacturer_or_Applicable_GPO_Making_Payment_Name";
		map = build(Table, Index, fixed_size);
		dump(Table, Index, map);
		
		System.out.println();
	}
	
	public void execute(String line)
	{
		Pattern p = Pattern.compile("\\w+\\s+.\\s+\\w+\\s+\\w+\\s+.+\\s+\\w+\\s+\\w+\\s+\\w+\\s+.+\\s*=\\s*.+");
		Matcher m = p.matcher(line);
		if(!m.matches())
		{
			System.out.println("Syntax Error");
		}
		else
		{
			String index_1 = new String();
			String index_2 = new String();
			String table_inner = new String();
			String table_outer = new String();
			
			String[] tokens = line.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			
			if(tokens.length == 9)
			{
				String temp = tokens[8];
				int pos = temp.indexOf("=");
				index_1 = temp.substring(2, pos);
				index_2 = temp.substring(pos + 3);
				if(index_2.endsWith(";"))
				{
					int length = index_2.length();
					index_2 = index_2.substring(0, length - 1);
				}
				table_inner = tokens[3];
				table_outer = tokens[5];
			}
			else if(tokens.length == 11)
			{
				index_1 = tokens[8].substring(2);
				index_2 = tokens[10].substring(2);
				if(index_2.endsWith(";"))
				{
					int length = index_2.length();
					index_2 = index_2.substring(0, length - 1);
				}
				table_inner = tokens[3];
				table_outer = tokens[5];
			}
			else return;
			
			File file_inner = new File(database + "_" + table_inner + "_" + index_1);
			File file_outer = new File(database + "_" + table_outer + "_" + index_2);
			if(file_inner.exists() && file_outer.exists())
			{
				HashMap<String, List<Integer>> index_inner = load(table_inner, index_1);
				HashMap<String, List<Integer>> index_outer = load(table_outer, index_2);
				join(table_inner, index_inner, table_outer, index_outer);
			}
			else
			{
				System.out.println(database + "_" + table_inner + "_" + index_1);
				System.out.println(database + "_" + table_outer + "_" + index_2);
				System.out.println("No index available");
			}
			System.out.println("==============================");
		}
	}
	
	public static void main(String[] args)
	{
		if(args.length == 3 && args[0].equals("-init"))
		{
			String database_name = args[2];
			HashIndex hashindex = new HashIndex(database_name);
			hashindex.buildIndex();
			
			try
			{
				File file = new File(args[1]);
				if(!file.exists())
				{
					return;
				}
				FileReader fr = new FileReader(file);
				BufferedReader reader = new BufferedReader(fr);
				String line = reader.readLine();
				while(line != null)
				{
					hashindex.execute(line);
					line = reader.readLine();
				}
				reader.close();
				
			} catch ( IOException e ) {
		    	e.printStackTrace();
		    }
		}
		else if(args.length == 1) 
		{
		
			String database_name = args[0];
			HashIndex hashindex = new HashIndex(database_name);
			hashindex.buildIndex();
			
			// Query part
			System.out.println("You may now start to query :");
			while(true)
			{
				Scanner scan = new Scanner(System.in);
				String line = scan.nextLine();
				hashindex.execute(line);
			}
		}
	}
}