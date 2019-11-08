package infrastructure;

import debug.Sim;
import experiment.DelayDozeExperiment;
import experiment.DelayOffExperiment;
import experiment.DualDelayDozeExperiment;
import experiment.Experiment;

public class DualDelayOffController extends DelayOffController {
	/*indicates whether this controller is currently using higt tau or low tau
	 * true means using regular tau (high tau), otherwise use low tau.
	 * */
    protected boolean doUseHighTau = true;
   // protected int numOfHighTauServers; 
    protected DualDelayDozeExperiment dualExp;

	public DualDelayOffController(Experiment experiment, OffMode offMode) {
		super(experiment, offMode);
		dualExp = (DualDelayDozeExperiment)experiment;
	}
	
	public boolean doUseHighTau(){
		return doUseHighTau; 
	}
	
	public void setUseHighTau(boolean useHighTau){
		this.doUseHighTau = useHighTau;
	}
	
	@Override
	public double getNextWaitingTime(int currentSS) {
		// TODO Auto-generated method stub
		if (currentSS == 1) {

			if (doUseHighTau) {
				rDelayOffTime = dualExp.getNextDelayTime();
			} else {
				rDelayOffTime = dualExp.getNextLowDelayTime();
			}

			return rDelayOffTime;
		} else {
			Sim.fatalError("DelayoffCore is in invalid sleepstate");
			return 0.0;
		}

	}
	
	

}
