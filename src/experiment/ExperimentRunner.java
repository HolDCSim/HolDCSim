package experiment;

import infrastructure.DataCenter;
import debug.Sim;

//import generator.MTRandom;
import event.ExperimentInput;
import event.ExperimentOutput;

public class ExperimentRunner {

	public enum ExperimentType {
		COMMUNICATION_EXP, // basic communication experiment
		SS_EXP, // sleep scale experiment
		RANDOM_EXP, // random sleep state experiment
		MS_EXP, // multiple server experiment (always on)
		SD_EXP, // energy efficient data center experiment
		ONSLEEP_EXP, // on off experiment with our power profile and state
					// transitions
		DELAYDOZE_EXP, // delay off experiment with our power profile and state
						// transitions
		ONOFF_EXP, // original on off experiment
		DELAYOFF_EXP, // original delay off experiment
		DUALDELAY_EXP,
		EROVER_EXP, // energy efficient experiment with multiple states support
		DUALEROVER_EXP,
        DELAYDOZE2_EXP,
        COMBINE_EXP, //combination of server and network experiment
        LCNETWORK_EXP // combination of server and network experiment and switch sleep states
	}

	private String expName;
	private ExperimentType expType;

	private ArgumentParser mArgParser;
	

	public ExperimentRunner(ArgumentParser argParser) {
		mArgParser = argParser;
		
		argParser.addOption("exptype", true, "experiment type");
		expName = argParser.parse("exptype");

		if (expName.equals("sleepscale")) {
			expType = ExperimentType.SS_EXP;
		} else if (expName.equals("random")) {
			expType = ExperimentType.RANDOM_EXP;
		} else if (expName.equals("ms_exp")) {
			expType = ExperimentType.MS_EXP;
		} else if (expName.equals("sd_exp")) {
			expType = ExperimentType.SD_EXP;
		} else if (expName.equals("network_exp")) {
			expType = ExperimentType.COMMUNICATION_EXP;
		} else if (expName.equals("onsleep_exp")) {
			expType = ExperimentType.ONSLEEP_EXP;
		} else if (expName.equals("onoff_exp")) {
			expType = ExperimentType.ONOFF_EXP;
		} else if (expName.equals("delaydoze_exp")) {
			expType = ExperimentType.DELAYDOZE_EXP;
		} else if (expName.equals("delaydoze2_exp")) {
			expType = ExperimentType.DELAYDOZE2_EXP;
		} else if (expName.equals("delayoff_exp")) {
			expType = ExperimentType.DELAYOFF_EXP;
		} else if (expName.equals("dualdelay_exp")) {
			expType = ExperimentType.DUALDELAY_EXP;
		} else if (expName.equals("erover_exp")) {
			expType = ExperimentType.EROVER_EXP;
		} else if(expName.equals("dualerover_exp")) {
			expType = ExperimentType.DUALEROVER_EXP;
		} else if(expName.equals("comb_exp")) {
			expType = ExperimentType.COMBINE_EXP;
		} else if(expName.equals("lcnetwork_exp")) {
			expType = ExperimentType.LCNETWORK_EXP;
		}
		else {
			Sim.fatalError("unknown experiment type");
		}

	}

	public Experiment createExperiment(ExperimentConfig expConfig) {

		// ///////////////////////////////////////////////////////////////////
		/** original bighouse stuff, will be discarded */
		// setup experiment
		// double targetRho = .5;
		ExperimentInput experimentInput = new ExperimentInput();
		// MTRandom rand = new MTRandom(1);

		ExperimentOutput experimentOutput = new ExperimentOutput();

		// FIXME: statistics output should be initialized by each experiment
		/******************************************************************
		 * experimentOutput.addOutput(StatName.SOJOURN_TIME, .05, .95, .05,
		 * 5000); experimentOutput.addOutput(StatName.GENERATED_SERVICE_TIME,
		 * .05, .95, .05, 5000); experimentOutput.addTimeWeightedOutput(
		 * TimeWeightedStatName.SERVER_POWER, .01, .5, .01, 50000, .001);
		 ********************************************************************/
		
		Experiment experiment = null;

		switch (expType) {
		case COMMUNICATION_EXP:
			//System.out.println("in experiment runner, create network experiment");
			experiment = new LinecardNetworkExperiment(expName, null, experimentInput,
					experimentOutput, expConfig, mArgParser);
			break;
		case COMBINE_EXP:
			experiment = new CombineExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case SS_EXP:
			experiment = new SleepScaleExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case RANDOM_EXP:
			experiment = new WindowedSSExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case MS_EXP:
			experiment = new MultiServerExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case SD_EXP:
			experiment = new ShallowDeepExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case EROVER_EXP:
			experiment = new ERoverExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
			
		case DUALEROVER_EXP:
			experiment = new DualERoverExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case ONOFF_EXP:
			experiment = new OnOffExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case DELAYOFF_EXP:
			experiment = new DelayOffExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;

		case ONSLEEP_EXP:
			experiment = new OnSleepExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case DELAYDOZE_EXP:
			experiment = new DelayDozeExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case DELAYDOZE2_EXP:
			experiment = new DelayDoze2Experiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case DUALDELAY_EXP:
			experiment = new DualDelayDozeExperiment(expName, null,
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		case LCNETWORK_EXP:
			experiment = new LinecardNetworkExperiment(expName, null, 
					experimentInput, experimentOutput, expConfig, mArgParser);
			break;
		default:
			experiment = null;
		}

		// ////////////////////////////////////////////////////////////////////
		// setup datacenter
		DataCenter dataCenter = new DataCenter(expConfig);
		dataCenter.initialize(experiment);
		
		if (experiment != null) {
			experiment.setDataCenter(dataCenter);
			dataCenter.mTopo.setmDataCenter(dataCenter);
			dataCenter.mTopo.initialize(expConfig.preloadPath());
			expConfig.setExperiment(experiment);
		}
		experimentInput.setDataCenter(dataCenter);
		return experiment;

	}

	public void dumpStatistics(Experiment experiment) {
		if (experiment.getExperimentConfig().isPrintFullHis()) {
			experiment.printFullHistory();
		}

		// check whether the log is asynchornously written to persistent state
		// if (experiment.getExpConfig().isAsynLogging()) {
		// experiment.flushAsynedLogs();
		// } else {
		experiment.dumpStatistics();
		// }
	}

}
