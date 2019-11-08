

package event;

import job.HeuristicJob;
import experiment.Experiment;


public abstract class HeuristicJobEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The job related to the event.
     */
    protected HeuristicJob job;

    @Override
    public EVENT_TYPE getEventType(){
    	return EVENT_TYPE.JOB_EVENT;
    }
    
    
    /**
     * Creates a new job event.
     *
     * @param time - the time the event takes place
     * @param experiment - the experiment the event is in
     * @param aJob - the job related to the event
     */
    public HeuristicJobEvent(final double time,
                    final Experiment experiment,
                    final HeuristicJob aJob) {
        super(time, experiment);
        this.job = aJob;
    }

 public void process (){
	 //job.getNextSchedulableTask();
 }

}
