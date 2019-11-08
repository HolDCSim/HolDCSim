package event;

import job.Task;
import infrastructure.DCNode;
import infrastructure.Schedulable;
import debug.Sim;
import experiment.Experiment;

/**
 * Represents a Task arriving at a server.
 * 
 * @author David Meisner (meisner@umich.edu)
 */
public final class TaskArrivalEvent extends TaskEvent {

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
	public TaskArrivalEvent(final double time, final Experiment experiment,
			final Task _task, final DCNode aServer) {
		super(time, experiment, _task);
		this.node = aServer;
		this.task = _task;
	}

	@Override
	public void printEventInfo() {
//		System.out.println("Time: " + this.getTime() + ", Job "
//				+ task.getJobId() + " Task " + task.getTaskId()
//				+ " arrvial event in server: " + node.getNodeId()
//				+ ". Task size :" + task.getSize() + ".");

	}

	/**
	 * Has the Task arrive at a server.
	 */
	@Override
	public void process() {
//		System.out.println("TAsk " + task.getTaskId() + " arrived");
		
		verbose();
		// this.server.createNewArrival(this.getTime());
		//System.out.println("llllllllll222"+this.getTask());
		try {
			Schedulable sdNode = (Schedulable) node;
			//System.out.println("llllllllll"+this.getTask());
			sdNode.scheduleTask(this.getTime(), this.getTask());
			this.getTask().markArrival(this.getTime());
			Sim.debug(5,"000 : task arrived at server " + this.getTask().getServer().getNodeId()+ this.getTask().getJobId() + " Task "+ getTask().getTaskId() + this.getExperiment().printStateofServers());
		} catch (ClassCastException e) {
			System.err.println("Current node: " + node.getNodeId()
					+ " is not schedulable");
			// TODO: maybe tolerate this error?

		}

	}

}
