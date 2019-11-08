package infrastructure;

import constants.Constants;
import queue.MSGlobalQueue;
import debug.Sim;
import job.Task;
import event.StartSleepEvent;
import event.MSTransToActiveEvent;
import experiment.Experiment;
import infrastructure.Socket.SocketPowerPolicy;

/**
 * Server used in simple version of ERover only have s5 when server is in low
 * power mode
 * @author fanyao
 * 
 */
public class ShallowDeepServer extends UniprocessorServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private int shallowSleepState;
	// private int deepSleepState;

	/**should be obsolete since there is no need to cancel such event*/
	private MSTransToActiveEvent toActiveEvent = null;
	private boolean toSleepEvent = false;
	private boolean assignedJob;

	protected boolean isScheduledActive;

//	public ShallowDeepServer(Experiment anExperiment, DataCenter dataCenter) {
//		super(anExperiment, dataCenter);
//		this.assignedJob = false;
//		this.isScheduledActive = true;
//		// TODO Auto-generated constructor stub
//	}

	public ShallowDeepServer(Experiment anExperiment, DataCenter dataCenter,
			Socket[] theSockets, AbstractSleepController abController, Constants.SleepUnit sleepUnit) {
		super(anExperiment, dataCenter, theSockets, abController, sleepUnit);
		this.assignedJob = false;
		this.isScheduledActive = true;
	}

//	public void fetchTaskToExecute(Task aTask, double time) {
//		activityUnit.fetchTaskToExecute(aTask, time);
//	}

//	@Override
//	public void insertTask(final double time, final Task task) {
//		// Check if the Task should be serviced now or put in the queue
//		if (this.getRemainingCapacity() == 0) {
//			// There was no room in the server, put it in the queue
//			Sim.fatalError("fatal error: tried to insert a job to a busy EnergyAwareServer!");
//		} else {
//			super.insertTask(time, task);
//		}
//	}

	@Override
	public void removeTask(final double time, final Task task) {
		if (experiment.useGlobalQueue()) {
			// Remove the Task from the socket it is running on
			Socket socket = this.TaskToSocketMap.remove(task);

			// Error check we could resolve which socket the Task was on
			if (socket == null) {
				Sim.fatalError("Task to Socket mapping failed");
			}

			boolean globalQueueEmpty = true;
			if (experiment.useGlobalQueue()) {
				if (experiment.getGlobalQueueSize() != 0)
					globalQueueEmpty = false;
			}

			// could further fetch job only when
			// 1. there are jobs in the global queue
			// 2. the server is currently in active mode (instead of scheduled deep
			// sleep)
			// boolean taskWaiting = !globalQueueEmpty &&
			// (mCore.getFixedSleepState() != deepSleepState);
			boolean taskWaiting = !globalQueueEmpty && this.isScheduledActive;
			// Remove the Task from the socket (which will remove it from the core)
			socket.removeTask(time, task, taskWaiting);

			if (!this.isScheduledActive) {
				// if (mCore.getFixedSleepState() == deepSleepState) {
				// Task has left the systems
				// succesufully put the server to sleep, clean the
				// MSTransToSleepEvent
				this.setToSleepEvent(false);

				/*
				 * better return immediately here, no need to updateThreshold as the
				 * jobs in the queue didn't change
				 */
				this.TasksInServerInvariant--;
				this.checkInvariants();
				return;
			}
			// There is now a spot for a Task, see if there's one waiting
			if (taskWaiting) {
				// for experiments with global queue
				// fetch one task from the global queue
				// this task is retrieved from global queue so the mServer has
				// not been set yet
				Task dequeuedTask = experiment.getGlobalQueue().poll(time);
				Sim.debug(
						3,
						"mmm Time: " + time + " task " + dequeuedTask.getTaskId()
								+ " continously service by server : "
								+ this.getNodeId());
				dequeuedTask.setServer(this);
				this.startTaskService(time, dequeuedTask);
				this.TasksInServerInvariant++;

			} else {
				// needs to udpateThresholds since there is no more jobs in the
				// queue to be retrieved
				MSGlobalQueue msGlobalQueue = (MSGlobalQueue) experiment
						.getGlobalQueue();
				msGlobalQueue.updateThreadholds(time);
			}
			// Task has left the systems
			this.TasksInServerInvariant--;
			this.checkInvariants();
		} else {
			super.removeTask(time, task);
		}
	}

	// public void setShallowSleepState(int shallowState) {
	// this.shallowSleepState = shallowState;
	// }
	//
	// public void setDeepSleepState(int deepState) {
	// this.deepSleepState = deepState;
	// }

	
	public MSTransToActiveEvent getToActiveEvent() {
		return this.toActiveEvent;
	}

	
	/**
	 * should be obsolete since we do not want to cancel MSTransToActiveEvent
	 * @param event
	 */
	public void setToActiveEvent(MSTransToActiveEvent event) {
		this.toActiveEvent = event;
	}

	
	/**
	 * indicate whether the server should be put back to low power mode
	 * @return
	 */
	public boolean getToSleepEvent() {
		return this.toSleepEvent;
	}

	/**
	 * flag function to indicate whether a current transition_to_active
	 * server should be put to low power mode or active mode. This is used
	 * since the same server may be transit from two states during the transiting
	 * time. This functionality could also be achieved by querying whether the
	 * server is currently scheduled active
	 * @param sleep
	 */
	public void setToSleepEvent(boolean sleep) {
		this.toSleepEvent = sleep;
	}

	public void setJobAssigned(boolean assigned) {
		this.assignedJob = assigned;
	}

	public boolean isJobAssigned() {
		return assignedJob;
	}

	public StartSleepEvent getSingelSleepEvent() {
		return sleepable.getSingleSleepEvent();
	}

	public StartSleepEvent generateSleepEvent(double time,
			Experiment experiment, int theSleepState) {
		return sleepable.generateSleepEvent(time, experiment, theSleepState);
	}

	// public boolean isScheduledActive() {
	// return isScheduledActive;
	// }
	//
	// public void setScheduledActive(boolean isActive) {
	// this.isScheduledActive = isActive;
	// }

	public int getInitialSleepState() {
		if (isScheduledActive) {
			/* shallowest sleep state */
			return ShallowDeepStates.SHALLOW;
		}

		else {
			/* deepest sleep state */
			return ShallowDeepStates.DEEP;
		}

	}

	public int getDeepestSleepState() {
		// TODO Auto-generated method stub
		// we know it is gonna be the deepest sleep state
		return sleepable.getDeepestSleepState();
	}

	public void setScheduledActive() {
		// TODO Auto-generated method stub
		this.isScheduledActive = true;
		sleepable.setNextFixedSleepState(ShallowDeepStates.SHALLOW);
		// mCore.setUseMultipleSleepState(false);
	}
	
	public boolean isScheduledActive(){
		return isScheduledActive;
	}

	public void unsetScheduledActive() {
		this.isScheduledActive = false;
		sleepable.setNextFixedSleepState(ShallowDeepStates.DEEP);
	}

	public StartSleepEvent getNextSleepEvent() {
		return sleepable.getNextSleepEvent();
	}
	
	public void initialPowerState(){
		//mCore.setPowerState(Core.PowerState.LOW_POWER_SLEEP);
		if (activityUnit.isSocket()) {
			Socket socket = (Socket)activityUnit;
			if (socket.powerPolicy != SocketPowerPolicy.NO_MANAGEMENT) {
				activityUnit.initialLowPowerState();
			}
		} else {
			activityUnit.initialLowPowerState();
		}
	}

}
