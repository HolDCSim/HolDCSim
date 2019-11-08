package scheduler;

import infrastructure.DualDelayOffController;
import infrastructure.DualERoverController;
import infrastructure.EnergyServer;
import experiment.DualDelayDozeExperiment;
import experiment.DualERoverExperiment;
//import experiment.ShallowDeepExperiment;

public class DualERoverScheduler extends ERoverScheduler {

	protected DualERoverExperiment dualExp;
	protected int numOfHighTauServers; 
	public DualERoverScheduler(DualERoverExperiment experiment) {
		super(experiment);
		// TODO Auto-generated constructor stub
		numOfHighTauServers = experiment.getExpConfig().getHighDelayServerNum();
		for(int i=0; i< experiment.getExpConfig().getServersToSchedule(); i++){
			EnergyServer server = (EnergyServer)experiment.getDataCenter().getServers().get(i);
			DualERoverController dualController = (DualERoverController)(server.getSleepController());
			
			if(i < numOfHighTauServers){
				dualController.setUseHighTau(true);
			}
			else{
				dualController.setUseHighTau(false);
			}
			// TODO Auto-generated constructor stub
		}
	}

}
