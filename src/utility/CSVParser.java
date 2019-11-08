package utility;

import java.io.*;
import java.util.Vector;

public class CSVParser {
	
    
	public static Vector<Vector<Integer>> parseCSV(String fileName){

	    
		 
		    if(fileName == null || fileName.isEmpty()){
				  
				fileName = "/home/fan/dependency.csv";	    	
		
		    }
		    
		    Vector<Vector<Integer>> dependencyGraph = new Vector<Vector<Integer>>();

			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
		 
			try {
		 
				br = new BufferedReader(new FileReader(fileName));
				while ((line = br.readLine()) != null) {
		 
				        // use comma as separator
					String[] rowString = line.split(cvsSplitBy);
		            Vector<Integer> rowDependency = new Vector<Integer>();
		    
		            for(String aString : rowString){
		            	rowDependency.add(Integer.parseInt(aString));
		            dependencyGraph.add(rowDependency);
		            }
				}
		 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		 
			System.out.println(dependencyGraph);
			return dependencyGraph;
		 
	}
}