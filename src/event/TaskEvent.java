

package event;

import job.Task;
import experiment.Experiment;

/**
 * An abstract TaskEvent class to extend for various kinds of events
 * relating to Tasks in servers.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public abstract class TaskEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The Task related to the event.
     */
    private Task Task;

    /**
     * Creates a new Task event.
     *
     * @param time - the time the event takes place
     * @param experiment - the experiment the event is in
     * @param aTask - the Task related to the event
     */
    public TaskEvent(final double time,
                    final Experiment experiment,
                    final Task aTask) {
        super(time, experiment);
        this.Task = aTask;
    }

    /**
     * Get the Task of the event.
     *
     * @return the event's Task
     */
    public final Task getTask() {
        return this.Task;
    }
    
    @Override
    public EVENT_TYPE getEventType(){
    	return EVENT_TYPE.TASK_EVENT;
    }

}
