package experiment;

import debug.Sim;
import loadpredictor.FakeLoadPredictor;
import scheduler.MultiServerScheduler;
import scheduler.OnSleep2Scheduler;
import scheduler.OnSleepScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;


/**
 * @author fanyao
 * delay timers experiment with two sleep state, shallow and deep
 * two taus are involved. idle -> s4 (tau1), s4 -> s5 (tau2)
 */
public class DelayDoze2Experiment extends DelayDozeExperiment {
	
	protected double meanDelayTime2;

	public DelayDoze2Experiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		this.sleepStateMode = "delaydoze2";
		this.statsFileName = "delaydoze2_stats";
		// TODO Auto-generated constructor stub
	}

	public double getNextDelayTime2() {
		// use fixed delay time in our paper
		return meanDelayTime2;
		//return delayTimeGen.getNextDouble();
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);

		argParser.addOption("meandeltime2", true, "mean delay time 2");
		
		if(argParser.parse("meandeltime2") == null) {
			Sim.fatalError("DelayDoze2Experiment requires mean delay time 2 param");
		}

		meanDelayTime2 = Double.parseDouble(argParser.parse("meandeltime2"));
		mExpConfig.setMeanDelayTime2(meanDelayTime2);
		//mExpConfig.setPendingThreshold(Integer.parseInt(tokens.popToken()));
		// mExpConfig.setWorkloadThreshold(threshold);
	}
	
	@Override
	protected void formulateResults() {
		super.formulateResults();
		
		resultFieldValueMap.put(String.format("%-15s", "delayoffTime2"), String.format("%-15.5f", mExpConfig.getMeanDelayTime2()));
	}
	
	@Override
	protected void initialServerType() {
		this.serverType = ServerType.DELAY_DOZE_SERVER2;
	}
	
	@Override
	protected void setTaskScheduler() {
		// OnOff2Scheduler is not original delayoff experiment implementation
		/**********************************************
		 * taskScheduler = new OnOff2Scheduler(this);
		 **********************************************/
		taskScheduler = new OnSleep2Scheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
				msScheduler.getQueueHis()));
	}
}
