package event;

import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.util.Vector;

import controller.WindowedSSEnforcer;
import beans.SSWindowRecord;
import experiment.Experiment;
import experiment.MultiServerExperiment;
import experiment.WindowedSSExperiment;

public class AllJobsFinishEvent extends AbstractEvent {

	private Experiment experiment;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AllJobsFinishEvent(final double time, final Experiment experiment) {
		super(time, experiment);
		// this.job = job;
		this.experiment = experiment;
	}

	@Override
	public void process() {
		experiment.setExperimentFinish(time);

		// fanyao commented for RandomSSExperiment
		// SleepScaleExperiment ssExp = (SleepScaleExperiment)experiment;
		if (experiment instanceof WindowedSSExperiment) {
			WindowedSSEnforcer ssEnforcer = ((WindowedSSExperiment) experiment)
					.getSSEnforcer();
			SSWindowRecord record = ssEnforcer.getWindowedRecords()
					.lastElement();
			record.setWindowSize(time - record.getWindowStateTime());
			record.setActiveTimePeriod(record.getWindowSize()
					- record.getIdleTimePeriod());
		}

		// wrap up sleep states statistics for all servers
		// Vector<Job> jobs = experiment.getJobs();
		if (experiment instanceof MultiServerExperiment) {
			int serverToSch = experiment.getExpConfig().getServersToSchedule();
			Vector<Server> servers = experiment.getDataCenter().getServers();
			for (int i = 0; i < serverToSch; i++) {
				UniprocessorServer server = (UniprocessorServer) servers.get(i);
				// if(server.getLastIdleTime() != time){
				server.udpateFinalStats(time);
//				if(server.isTransitionToActive()){
//					System.out.printf("transitioning");
//				}
				// }
			}
		}
	}

	@Override
	public EVENT_TYPE getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE.JOB_EVENT;
	}
}
