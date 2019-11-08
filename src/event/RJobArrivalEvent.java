package event;

import debug.Sim;
import job.Job;
import experiment.Experiment;

public final class RJobArrivalEvent extends JobEvent {

	private static final long serialVersionUID = 1L;

//	public RJobArrivalEvent(final double time, final Experiment experiment,
//			final Job job) {
//		super(time, experiment, job);
//	}
	
	public RJobArrivalEvent(final double time, final Experiment experiment,
			final Job job) {
		super(time, experiment, job);
	}

	@Override
	public void process() {
		verbose();
//		Sim.debug(3, "+++" + time + ", [JOB_ARRIVAL], job " + job.getJobId()
//				+ " arrival, job type: " + job.getJobType() + ".");
		job.initialize(time);
		
		/*
		 * put getNextJob next to initialize() so that the task event produced
		 * from initialize would be processed first. Therefore when multiple
		 * jobs arrive at the same time, the status of the server would be
		 * marked by the first arrival
		 */
		// get the next random job
		experiment.getNextJob(time);
	}

}

/*
 * job scheduler network size stress test network power delay task finish event
 * check blocking and nonblocking documents workload generator server side
 * modeling topology generator summary and plan
 */
