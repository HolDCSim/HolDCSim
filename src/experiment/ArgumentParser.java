package experiment;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.*;

import debug.Sim;

/**
 * Utilizes Apache Commons CLI library to parse arguments using options
 */

public class ArgumentParser {
	Options options;
	String[] args;
	CommandLineParser parser;
	CommandLine cmd;
	
	public ArgumentParser(String args[]) {
		options = new Options();
		this.args = args;
		parser = new DefaultParser();
		cmd = null;
	}
	
	public void addOption(String opt, boolean hasArg, String description) {
		options.addOption(opt, hasArg, description);
	}
	
	public String parse(String opt) {
		List<String> argsList = Arrays.asList(args);
		int optIndex = argsList.indexOf("-" + opt);
		
		//Option not found
		if(optIndex == -1) {
			Sim.fatalError("-" + opt + " option not found");
		}
		
		//Using limited range array allows options to be parsed out of order
		String[] subarray = Arrays.copyOfRange(args, optIndex, optIndex + 2);
		
		try {
			cmd = parser.parse(options, subarray);
		} catch (ParseException e) {
			Sim.fatalError("Error parsing arguments");
		}
		return cmd.getOptionValue(opt);
	}
}
