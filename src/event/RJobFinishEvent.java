package event;

import job.HeuristicJob;
import job.Job;
import job.Job.JobState;
import experiment.Experiment;
import experiment.SingletonJobExperiment;

public final class RJobFinishEvent extends AbstractEvent {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;
	private Job job;
	private HeuristicJob heuristicJob;

	/**
	 * Creates a new JobFinishEvent.
	 * 
	 * @param time
	 *            - the time the job finishes
	 * @param experiment
	 *            - the experiment the event is in
	 * @param job
	 *            - the finishing job
	 * @param aServer
	 *            - the server the job finished on
	 * @param theFinishTimeSet
	 *            - double check this
	 * @param theFinishSpeed
	 *            - the normalized speed at which the job finishes
	 */
	public RJobFinishEvent(final double time, final Experiment experiment,
			final Job job) {
		super(time, experiment);
		this.job = job;

	}
	
	/**
	 * Job finish event for heuristic job
	 */
	public RJobFinishEvent(final double time, final Experiment experiment,
			final HeuristicJob heuristicJob) {
		super(time, experiment);
		this.heuristicJob = heuristicJob;
	}

	@Override
	public void printEventInfo() {
//		System.out.println("Time: " + this.getTime() + ", Job "
//				+ job.getJobId()
//				+ " Finish event.");
		//System.out.println("The job execution time is " + (this.time-job.getStartTime()));

	}

	// @Override
	public void process() {
		this.job.changeJobState(JobState.JOBFINISHED);
        experiment.jobFinished(job, time);
        //System.out.println("The job execution time is " + (this.time-job.getStartTime()));
        //System.out.println("The job execution time is " +job.getFinishTime() + "hhhhhh"+ job.getStartTime());
        //System.out.println(this.job.getFlowCompletionTime() + "     " + (job.getFinishTime()-job.getStartTime()));
        //System.out.println(this.job.getFlowCompletionTime());
        //System.out.println(job.getFinishTime()-job.getStartTime()+job.getTaskDelay()*0.001);
//        System.out.println(job.getFinishTime()-job.getStartTime());
	}
	
//	public void process() {
//		this.job.changeJobState(JobState.JOBFINISHED);
//        experiment.jobFinished(job, time);
//	}

	@Override
	public EVENT_TYPE getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE.JOB_EVENT;
	}

}
