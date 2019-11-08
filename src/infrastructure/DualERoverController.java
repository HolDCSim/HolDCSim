package infrastructure;

import debug.Sim;
import experiment.DualDelayDozeExperiment;
import experiment.DualERoverExperiment;
import experiment.Experiment;

public class DualERoverController extends ERoverSleepController {

	protected double tau;
	
	protected boolean doUseHighTau = true;
	   // protected int numOfHighTauServers; 
	    protected DualERoverExperiment dualExp;
	    
	public DualERoverController(Experiment experiment) {
		super(experiment);
		dualExp = (DualERoverExperiment)experiment;
		// TODO Auto-generated constructor stub
	}

	
	public boolean doUseHighTau(){ 
		return doUseHighTau;
	}
	
	public void setUseHighTau(boolean useHighTau){
		this.doUseHighTau = useHighTau;

		if (useHighTau)
			tau = dualExp.getExpConfig().getSingleTao();
		else
			tau = dualExp.getExpConfig().getMeanLowDelayTime();
	}
	
	@Override
	public double getNextWaitingTime(int currentSS){
		// TODO Auto-generated method stub
		//double waitTime = 0.0;
		if (currentSS != ERoverStates.DEEP_SLEEP) {
			Sim.fatalError("MultiStateCore is in unexpected sleep state "
					+ currentSS);
		}
		
		return tau;
//		return sleepStateWaitings[ERoverStates.DEEPEST_SLEEP - 1]
//				- sleepStateWaitings[currentSS - 1];
	}
	
	@Override
	public void accumulateSSTimes(int sleepState, double time) {
		
		this.updateWakeupSplits(sleepState);
		// TODO Auto-generated method stub
		/*
		 * for MultiStateCore we know whether it is using multiple state or not
		 * by looking at the current sleeps state. we do not
		 * useMultipleSleepState here before it might be changed by
		 * EnergyAwareScheduler
		 */
		if (sleepState != 1) {

			C6S0Count++;
			if (sleepState > ERoverStates.DEEP_SLEEP) {
				C6S3Count++;
				C6S0Time += tau;
				C6S3Time += time - tau - AbstractSleepController.sleepStateWakeups[sleepState - 1];
			} else {
				/*
				 * C6S0Time += time - Core.sleepStateWaitings[sleepState - 1] -
				 * Core.sleepStateWakeups[sleepState - 1];
				 */
				C6S0Time += time - AbstractSleepController.sleepStateWakeups[sleepState - 1];
				return;
			}
		}

		// fixed sleepstate = 1
		else {

			double duration = time - AbstractSleepController.sleepStateWakeups[sleepState - 1];

			C0S0Count++;
			C0S0Time += duration;
		}

	}
}
