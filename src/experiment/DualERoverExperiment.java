package experiment;

import loadpredictor.AbstractLoadPredictor;
import loadpredictor.AllAveragePredictor;
import loadpredictor.InstantPredictor;
import loadpredictor.MixedInstantPredictor;
import loadpredictor.WindowedAveragePredictor;
import debug.Sim;
import scheduler.DualERoverScheduler;
import scheduler.ERoverScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;

public class DualERoverExperiment extends ERoverExperiment {
	
	private static final long serialVersionUID = 1L;
	
	//private ExponentialRandom lowDelayGen;
	
	protected static double meanLowDelayTime; 
	
	protected int lowDelayServers;
	

	public DualERoverExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		// TODO Auto-generated constructor stub
		
		this.statsFileName = "dualerover_stats";
		this.sleepStateMode = "dualerover";
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);

		argParser.addOption("meanldeltime", true, "mean low delay time");
		argParser.addOption("hdelservnum", true, "high delay server num");
		
		if(argParser.parse("meanldeltime") == null || argParser.parse("hdelservnum") == null) {
			Sim.fatalError("DualERoverExperiment requires mean low delay time and high delay server num params");
		}

		meanLowDelayTime = Double.parseDouble(argParser.parse("meanldeltime"));
		mExpConfig.setMeanLowDelayTime(meanLowDelayTime);
		mExpConfig.setHighDelayServerNum(Integer.parseInt(argParser.parse("hdelservnum")));
		// mExpConfig.setWorkloadThreshold(threshold);
	}
	
	public double getNextLowDelayTime() {
		// use fixed delay time in our paper
		 return meanLowDelayTime;
		
		//return lowDelayGen.getNextDouble();
	}

	@Override
	public void initialServerType(){
		this.serverType = ServerType.DUAL_EROVER_SERVER;
	}
	
	@Override
	protected void formulateResults() {
		
		super.formulateResults();

		resultFieldValueMap.put(String.format("%-15s", "highTau"), String.format("%-15s",mExpConfig.getMeanDelayTime()));
		resultFieldValueMap.put(String.format("%-15s", "LowTau"), String.format("%-15.5f", mExpConfig.getMeanLowDelayTime()));
		resultFieldValueMap.put(String.format("%-15s", "activeServers"), String.format("%-15d", mExpConfig.getHighDelayServerNum()));
	}

	public double getNextDelayTime() {
		// TODO Auto-generated method stub
		return mExpConfig.getSingleTao();
	}
	
	@Override
	protected void setTaskScheduler() {
		ERoverScheduler eeScheduler = new DualERoverScheduler(this);

		double predictorType = mExpConfig.getPredictorType();

		AbstractLoadPredictor predictor = null;

		
		// if it's mixed workload, use mixedinstant predictor
		if (predictorType == -2) {
			predictor = new MixedInstantPredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), this);
		}
		
		else if (predictorType == -1) {
			predictor = new InstantPredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else if (predictorType == 0) {
			predictor = new AllAveragePredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else {
			predictor = new WindowedAveragePredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), predictorType);
		}

		taskScheduler = eeScheduler;
		eeScheduler.setQueuePredictor(predictor);
	}

}
