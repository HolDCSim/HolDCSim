
package debug;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Sim {
	
    public static final boolean RANDOM_SEEDING = false; 

    /**
     * The default debug level of the simulator.
     */
    private static final int DEFAULT_DEBUG_LEVEL = 1;

    /**
     * The current debug level of the simulator.
     */
    public static int debugLevel = DEFAULT_DEBUG_LEVEL;

    /**
     * 
     * Private constructor which prevents instantiation.
     */
    private Sim() {
        throw new UnsupportedOperationException();
    }

    /**
     * Prints the banner for the simulator.
     */
    public static void printBanner() {
        String banner =
    "fanyao: basic network testing\n";
    

        System.out.println(banner);
    }

    /**
     * Set the level of debugging.
     * Higher levels of debugging prints more information.
     * @param level - The level of debugging to set the simulator to
     */
    public static void setDebugLevel(final int level) {
        Sim.debugLevel = level;
    }

    /**
     * Get the current debug level.
     * @return the current debug level
     */
    public static int getDebugLevel() {
        return Sim.debugLevel;
    }
    
    public static void warning(String message){
    	System.out.println(message);
    }

    /**
     * Prints a debug message if the debug level is at
     * or above the provided threshold.
     * @param levelThreshold - the level the debug level must be
     * at to print this message
     * @param message - the message to print
     */
    public static void debug(final int levelThreshold, final String message) {
        if (levelThreshold <= Sim.debugLevel) {
            System.out.println(message);
        }
    }

    /**
     * Prints a debug message if the debug level is at or above
     * the provided threshold and annotates the time the message occurred.
     * @param levelThreshold - the level the debug level must be
     * at to print this message
     * @param time - the time (in simulation time) the debug is printed
     * @param message - the message to print
     */
    public static void debug(final int levelThreshold,
                             final double time,
                             final String message) {
        if (levelThreshold <= Sim.debugLevel) {
            debug(levelThreshold, "[" + time + "] " + message);
        }
    }

    /**
     * Prints a fatal error and ends the simulation.
     * @param message - the fatal error to print
     */
    public static void fatalError(final String message) {
        System.out.println(message);
        throw new RuntimeException();
    }
    
    public static void log(final String fileName, final String message) {
   
    	try {
   
    		String directory = System.getProperty("user.dir")+File.separator+"log"+File.separator;
    		String path = directory + fileName;
    	
    		File file = new File(path);
    		File directoryFile = new File(directory);
    		
    		if(!directoryFile.exists())
    		{
    			directoryFile.mkdir();
    		}
    		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
    		
    		writer.append(message);
    		writer.append('\n');
    		writer.close();
    	} catch(IOException e) {
    		throw new RuntimeException();
    	}
    }
    public static void log_no_n(final String fileName, final String message) {
   
    	try {
   
    		String directory = System.getProperty("user.dir")+File.separator+"log"+File.separator;
    		String path = directory + fileName;
    	
    		File file = new File(path);
    		File directoryFile = new File(directory);
    		
    		if(!directoryFile.exists())
    		{
    			directoryFile.mkdir();
    		}
    		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
    		
    		writer.append(message);
    		writer.close();
    	} catch(IOException e) {
    		throw new RuntimeException();
    	}
    }

}
