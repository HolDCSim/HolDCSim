package event;

import job.Task;
import scheduler.LinecardNetworkScheduler;
import infrastructure.DCNode;
import infrastructure.Schedulable;
import experiment.Experiment;

/**
 * Task will be scheduled on server to satisfy QoS requirements, regardless of whether server
 * workload threshold has been reached
 */
public final class TaskTimerEvent extends TaskEvent {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The server at which the Task arrives.
	 */
	private DCNode node;
	private Task task;

	/**
	 * Constructs a Task arriving at a server.
	 * 
	 * @param time
	 *            - the time the Task arrives
	 * @param experiment
	 *            - the experiment the event happens in
	 * @param Task
	 *            = the Task that arrives
	 * @param aServer
	 *            - the server the Task arrives at
	 */
	public TaskTimerEvent(final double time, final Experiment experiment,
			final Task _task) {
		super(time, experiment, _task);
		this.task = _task;
	}

	@Override
	public void printEventInfo() {
		
	}

	/**
	 * Wake up server and schedule task
	 */
	@Override
	public void process() {
		verbose();
		
		LinecardNetworkScheduler taskScheduler = (LinecardNetworkScheduler)(experiment.getTaskScheduler());
		taskScheduler.wakeServerForJobQoS(experiment.getCurrentTime());
	}

}
