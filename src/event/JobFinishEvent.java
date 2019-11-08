

package event;

import job.Job;
import job.Job.JobState;
import debug.Sim;
import experiment.Experiment;


public final class JobFinishEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;
    private Job job;

   
    /**
     * Creates a new JobFinishEvent.
     *
     * @param time - the time the job finishes
     * @param experiment - the experiment the event is in
     * @param job - the finishing job
     * @param aServer - the server the job finished on
     * @param theFinishTimeSet - double check this
     * @param theFinishSpeed - the normalized speed at which the job finishes
     */
    public JobFinishEvent(final double time,
                          final Experiment experiment,
                          final Job job) {
        super(time, experiment);
        this.job = job;
        
    }
    
    @Override
	public void printEventInfo() {
		Sim.debug(5,"Time: " + this.getTime() + ", Job "
				+ job.getJobId()
				+ " Finish event.");
		//System.out.println("The job execution time is " + (this.time-job.getStartTime()));

	}

    //@Override
    public void process() {
       this.job.changeJobState(JobState.JOBFINISHED);
       boolean allJobFinished = true;
       for(Job j:experiment.getJobs()){
    	   if(j.queryState() != JobState.JOBFINISHED ){
    		   allJobFinished = false;
    		   break;
    	   }
       }
       if(allJobFinished == true){
    	   //add AllJobsFinishEvent
    	   AllJobsFinishEvent finishEvent = new AllJobsFinishEvent(time, experiment);
			experiment.addEvent(finishEvent);
       }
       
    }
    
    @Override
    public EVENT_TYPE getEventType() {
    	// TODO Auto-generated method stub
    	return EVENT_TYPE.JOB_EVENT;
    }

}
