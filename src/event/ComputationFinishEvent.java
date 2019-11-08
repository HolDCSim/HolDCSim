package event;

import infrastructure.DCNode;
import infrastructure.Server;

import java.util.ArrayList;
import java.util.Vector;

//import com.sun.corba.se.impl.orbutil.DenseIntMapImpl;
//import com.sun.xml.internal.ws.wsdl.parser.MexEntityResolver;

import communication.Packet;
import job.Job;
import job.Task;
import constants.Constants;
import debug.Sim;
import experiment.Experiment;

/**
 * Represents a Task finishing on a server.
 * 
 * 
 */
public final class ComputationFinishEvent extends TaskEvent {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The speed at which the Task finish time was calculated.
	 */
	private double computationSpeed;

	private Task task;

	// TODO (meisner@umich.edu) Figure out exactly how this works
	/**
	 * ...
	 */
	private double compTimeSet;

	/**
	 * The server on which the Task finished.
	 */
	private Server server;
	
	private Experiment mExp;

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.SERVER_EVENT;
	}

	// TODO

	/**
	 * Creates a new TaskFinishEvent.
	 * 
	 * @param time
	 *            - the time the Task finishes
	 * @param experiment
	 *            - the experiment the event is in
	 * @param Task
	 *            - the finishing Task
	 * @param aServer
	 *            - the server the Task finished on
	 * @param theCompTimeSet
	 *            - double check this
	 * @param theCompSpeed
	 *            - the normalized speed at which the Task finishes
	 */
	public ComputationFinishEvent(final double time,
			final Experiment experiment, final Task Task, final Server aServer,
			final double theCompTimeSet, final double theCompSpeed) {
		super(time, experiment, Task);
		this.server = aServer;
		Task.setComputationFinishEvent(this);
		this.compTimeSet = theCompTimeSet;
		this.computationSpeed = theCompSpeed;
		this.task = Task;
		this.mExp = experiment;
	}
	
	@Override
	public void printEventInfo(){
		/*
		 
				+ task.getJobId() + " Task " + task.getTaskId()
				+ " Computation finish event." + " Task " + task.getTaskId()
				+ " size: " + task.getSize());
				*/
	}

	@Override
	public void process() {
		
		verbose();
		  

		// FIXME:need to rename this method as task computation finish
		Job job = task.getaJob();

		// Tasks that are dependent on current task
		Vector<Task> childTasks = job.getChildTasks(this.task);
		
		// Tasks that current task is dependent on
		Vector<Task> parentTasks = job.getParentTasks(this.task);

		// No dependencies. Finish immediately
		if ((parentTasks == null || parentTasks.size() == 0) && (childTasks == null || childTasks.size() == 0)) {
			if (!task.getDoneExecuting()) {
				task.markDoneExecuting();
			}
			this.getTask().markComputationFinish(this.getTime());
			TaskFinishEvent finishEvent = new TaskFinishEvent(time, experiment, task, task.getServer(), time, 0);
			this.experiment.addEvent(finishEvent);
			Sim.debug(5,"Computation finished" + task.getTaskId());
		}
		// Current task done executing
		else if (task.getDoneExecuting()) {
			// Wait for communication to finish
			rescheduleComputationFinishEvent();
		}
		// Current task not done executing
		else {
			// Has parent tasks
			if (parentTasks != null && parentTasks.size() > 0) {
				// Parent tasks done executing, build communications
				 Sim.debug(7,"Task finished  " + task.getTaskId());
				if (getDependentTasksDoneExecuting(parentTasks)) {
					experiment.buildCommunications(task, parentTasks, time);
					task.markDoneExecuting();
				}
			}
			// Has child tasks
			else {
				task.markDoneExecuting();
			}
			rescheduleComputationFinishEvent();
		}
	}

	public double getComputationSpeed() {
		// TODO Auto-generated method stub
		return this.computationSpeed;
	}

	public double getCompTimeSet() {
		// TODO Auto-generated method stub
		return this.compTimeSet;
	}
	
	/**
	 * Return true if all tasks in vector are done executing, false otherwise
	 */
	public boolean getDependentTasksDoneExecuting(Vector<Task> dependentTasks) {
		for(int i = 0; i < dependentTasks.size(); i++) {
			if(dependentTasks.get(i).getServer() == null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Task is not ready to finish. Delay finish time by task size
	*/
	public void rescheduleComputationFinishEvent() {
		
		task.setAmountDelayed(task.getAmountDelayed() + task.getSize());
		ComputationFinishEvent cfEvent = new ComputationFinishEvent(this.time + task.getSize(),
				this.experiment, task, server, compTimeSet, computationSpeed);
		this.experiment.addEvent(cfEvent);
	}
	
}
