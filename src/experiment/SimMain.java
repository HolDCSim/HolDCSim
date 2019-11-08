package experiment;

import constants.Constants.TimeWeightedStatName;
import stat.StatisticsCollection;

import debug.Sim;

public class SimMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//Create ArgumentParser
		ArgumentParser argParser = new ArgumentParser(args);
		
		//Create option
		argParser.addOption("path", true, "file path of simconfig");
		
		//Get file path of configuration file (sim.config)
		String configFile = argParser.parse("path");
		
		ExperimentRunner expRunner = new ExperimentRunner(argParser);
		// if (args[args.length - 1].equals("random")) {
		// expRunner = new RandomSSRuner(args);
		// } else if (args[args.length - 1].equals("ss")) {
		// expRunner = new SleepScaleRunner(args);
		// }

		
		Experiment theExp = null;

		ExperimentConfig expConfig = new ExperimentConfig(configFile);
		theExp = expRunner.createExperiment(expConfig);
		
		if (theExp != null) {
			// run the experiment
			theExp.initialize();
			theExp.run();
			
			if(expConfig.isPrintFullHis()){
				theExp.printFullHistory();
			}
			
			if(expConfig.isPrintAggStats()){
				theExp.dumpAggStatistics();
			}
			theExp.dumpStatistics();

			System.out.println("Now flushing out statistics to file if necessary...");
			if (StatisticsCollection.SAVE_TO_FILE) {
				theExp.flushStatistics();
			}

			System.out.println("Simulation finished");

		} else {
			// System.err.println("experiment initializing failed");
			Sim.fatalError("experiment initializing failed");
		}
		// }
	}

}
