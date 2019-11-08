package infrastructure;

import infrastructure.Core.CorePowerPolicy;
import infrastructure.Core.PowerState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import job.Task;

import constants.Constants;
import debug.Sim;

import processor.CorePState;

import event.StartSleepEvent;
import event.Event;
import event.SocketEnteredParkEvent;
import event.SocketExitedParkEvent;
import experiment.Experiment;
import experiment.MultiServerExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * This class represents a single socket (physical processor chip) in a server.
 * 
 * 
 */
public final class Socket implements IActivityUnit, Serializable {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The server this Socket belongs to.
	 */
	private Server server;

	/**
	 * The number of cores in this socket.
	 */
	private int nCores;
	
	/**
	 * frequency shared across all cores
	 */
	public double speed;

	/**
	 * The experiment the socket is part of.
	 */
	private Experiment experiment;
	
	private double lastTaskFinishTime = 0.0;
	
	private double lastStartServiceTime = 0.0;

	/**
	 * A mapping of Tasks to cores. This allows bookeeping of cores when Tasks
	 * finish.
	 */
	private HashMap<Task, Core> taskToCoreMap;

	/**
	 * Available socket power states.
	 */
	// private enum SocketPowerState {
	// /**
	// * Socket is active and can process Tasks.
	// */
	// ACTIVE,
	//
	// /**
	// * Socket is in the process of transitioning to idle.
	// */
	// TRANSITION_TO_LOW_POWER_IDLE,
	//
	// /**
	// * Socket is in the process of transitioning to active.
	// */
	// TRANSITIONG_TO_ACTIVE,
	//
	// /**
	// * Socket is in a low-power idle state.
	// */
	// LOW_POWER_IDLE
	// };
	
	public enum SocketPowerState {
		/**
		 * Socket is active and can process Tasks.
		 */
		ACTIVE,

		/**
		 * Socket is in the process of transitioning to active.
		 */
		TRANSITIONG_TO_ACTIVE,
		
		PAKRING,
//		SOCKET_HALT,

		/**
		 * Socket is in a low-power idle state.
		 */
		LOW_POWER_SLEEP
	};

	/**
	 * Available power management policies for the socket.
	 */
	public static enum SocketPowerPolicy {
		/**
		 * No power management, socket is always active.
		 */
		NO_MANAGEMENT,

		/**
		 * socket sleep state.
		 */
		SOCKET_PARKING,
		SOCKET_SLEEP, 
		
	};

	/**
	 * The policy used by the socket for power management.
	 */
	public SocketPowerPolicy powerPolicy;

	public SocketPowerPolicy getPowerPolicy() {
		return powerPolicy;
	}


	/**
	 * The power state of the socket.
	 */
	public SocketPowerState powerState;

	/**
	 * Cores are available to process Tasks.
	 */
	private Vector<Core> availableCores;

	/**
	 * Cores that are busy processing a Task.
	 */
	private Vector<Core> busyCores;

	/**
	 * A temporary queue for Tasks while a socket is transitioning.
	 */
	private Vector<Task> transitionQueue;
	

	/** The power consumed by the socket in park (in watts). */
	//private double socketParkPower;

	/** The power consumed by the socket while idle (in watts). */
	//private double socketActiveIdlePower;

	/** The transition time of the socket in and out of park (in seconds) . */
	//private double socketParkTransitionTime;

	/**
	 * The event transitioning the socket. Used to allow cancellation if a Task
	 * arrival interrupts the transition.
	 */
	private Event trasitionEvent;

	//private int taskCompleted;
	
	/**
	 * the SleepController
	 */
	protected AbstractSleepController mSleepController = null;
	

	/**
	 * Instantiate a socket with nCores cores.
	 * 
	 * @param anExperiment
	 *            - the experiment the socket is part of
	 * @param theNCores
	 *            - the number of cores in the socket
	 */
	public Socket(final Experiment anExperiment, final int theNCores) {

		this.experiment = anExperiment;
		//this.server = aServer;
		this.nCores = theNCores;

		this.taskToCoreMap = new HashMap<Task, Core>();
		this.availableCores = new Vector<Core>();
		this.busyCores = new Vector<Core>();
		this.transitionQueue = new Vector<Task>();

		// Create nCores Cores and put them on the free list
		for (int i = 0; i < nCores; i++) {
			Core core = new Core(experiment);
			core.setSocket(this);
			this.availableCores.add(core);
		}

		this.powerPolicy = SocketPowerPolicy.NO_MANAGEMENT;
		this.powerState = SocketPowerState.ACTIVE;
	}
	
	
	public Socket(final Experiment anExperiment, final int theNCores,
			AbstractSleepController abController) {
		this(anExperiment, theNCores);
		this.mSleepController = abController;
		
	}

	// fanyao added
	/**
	 * use this constructor to set core externally
	 * 
	 * @param experiment
	 * @param theCores
	 */
	public Socket(final Experiment experiment, Vector<Core> theCores) {
		if (theCores == null || theCores.size() == 0) {
			Sim.fatalError("core vectore are empty");
		}

		for(Core aCore : theCores){
			aCore.setSocket(this);
		}
		this.experiment = experiment;
		//this.server = aServer;
		this.nCores = theCores.size();

		this.taskToCoreMap = new HashMap<Task, Core>();
		this.availableCores = new Vector<Core>();
		this.busyCores = new Vector<Core>();
		this.transitionQueue = new Vector<Task>();

		this.availableCores.addAll(theCores);
		this.powerPolicy = SocketPowerPolicy.NO_MANAGEMENT;
		this.powerState = SocketPowerState.ACTIVE;
	}
	
	public void setServer(Server theServer){
		this.server = theServer;
	}
	
	public void prepareStats(){
		mSleepController.prepareStats();
	}
	

	public SocketPowerState getPowerState(){
		return powerState;
	}
	
	public void setPowerState(Socket.SocketPowerState powerState){
		this.powerState = powerState;
	}
	
	@Override
	public void initialLowPowerState(){
			this.powerState = SocketPowerState.LOW_POWER_SLEEP;
	}
	
	/**
	 * Start a Task for the first time on the socket. It will be assigned to a
	 * random core.
	 * 
	 * @param time
	 *            - the time the Task is inserted
	 * @param Task
	 *            - the Task being inserted
	 */
	public void insertTask(final double time, final Task Task) {
		if (this.powerState == SocketPowerState.ACTIVE) {
			/**
			 * update lastService time if this is the first job current socket
			 * received before waking up
			 */
			if(busyCores.size() == 0){
				lastTaskFinishTime = -1.0;
				lastStartServiceTime = time;
			}
			
			// Pick the first core off the available cores
			Core core = this.availableCores.remove(0);
			core.insertTask(time, Task);
			this.busyCores.add(core);

			// Save the core the Task is on so we can remove it later
			this.taskToCoreMap.put(Task, core);
		} 
		// else if (this.powerState ==
		// SocketPowerState.TRANSITION_TO_LOW_POWER_IDLE) {
		// this.transitionQueue.add(Task);
		// this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;
		//
		// if (this.trasitionEvent != null) {
		// this.experiment.cancelEvent(this.trasitionEvent);
		// }
		//
		// double exitParkTime = time + Constants.SOCKET_PARK_TRANSITION_TIME;
		// SocketExitedParkEvent socketExitedParkEvent = new
		// SocketExitedParkEvent(
		// exitParkTime, this.experiment, this);
		// this.experiment.addEvent(socketExitedParkEvent);
		// }
		
	
		/*********************************************************************
		else if (this.powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
			this.transitionQueue.add(Task);
		} else if (this.powerState == SocketPowerState.LOW_POWER_SLEEP) {
			this.transitionQueue.add(Task);
			this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;
			double exitParkTime = time + Constants.SOCKET_PARK_TRANSITION_TIME;
			SocketExitedParkEvent socketExitedParkEvent = new SocketExitedParkEvent(
					exitParkTime, this.experiment, this);
			this.experiment.addEvent(socketExitedParkEvent);
		}
		*********************************************************************/
		else {
			if (mSleepController != null) {
				//need to store the task somewhere 
				this.transitionQueue.add(Task);
				
				/**
				 * add task to transitionQueue before preparewakeup modification
				 * needed when direct call of wakeup event process due to 0
				 * wakeup penalty
				 */
				mSleepController.prepareControllerWakeup(time);
				
			}
			/**
			 * when sleepcontroller not used, we use the original mechanism
			 * 
			 */
			else {
				if (this.powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
					this.transitionQueue.add(Task);
				} else if (this.powerState == SocketPowerState.LOW_POWER_SLEEP) {
					this.transitionQueue.add(Task);
					this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;
					double exitParkTime = time
							+ Constants.SOCKET_PARK_TRANSITION_TIME;
					SocketExitedParkEvent socketExitedParkEvent = new SocketExitedParkEvent(
							exitParkTime, this.experiment, this);
					this.experiment.addEvent(socketExitedParkEvent);
				}
			}
		}

	}

	/**
	 * Removes a Task from the socket due to completion.
	 * 
	 * @param time
	 *            - the time the Task is removed
	 * @param Task
	 *            - the Task being removed
	 * @param taskWaiting
	 *            - If there is a Task waiting for this to be removed
	 */
	public void removeTask(final double time, final Task Task,
			final boolean taskWaiting) {

		// Find out which socket this Task was running on
		Core core = this.taskToCoreMap.remove(Task);

		// Error check we got a real socket
		if (core == null) {
			Sim.fatalError("Couldn't resolve which core this Task belonged to");
		}

		core.removeTask(time, Task, taskWaiting);

		// Mark that the Task is no longer busy
		boolean found = this.busyCores.remove(core);

		// Error check the socket was considered busy
		if (!found) {
			Sim.fatalError("Could take core off the busy list");
		}

		// Core is now available
		this.availableCores.add(core);

		if (this.busyCores.size() == 0 && !taskWaiting) {
			
			if (this.powerPolicy == SocketPowerPolicy.SOCKET_PARKING) {
				//this.powerState = SocketPowerState.TRANSITION_TO_LOW_POWER_IDLE;
				double enterParkTime = time
						+ Constants.SOCKET_PARK_TRANSITION_TIME;
				SocketEnteredParkEvent socketEnteredParkEvent = new SocketEnteredParkEvent(
						enterParkTime, this.experiment, this);
				this.experiment.addEvent(socketEnteredParkEvent);
				this.trasitionEvent = socketEnteredParkEvent;
			}
			else if (this.powerPolicy == SocketPowerPolicy.SOCKET_SLEEP) {
				if (mSleepController != null)
					mSleepController.prepareForSleep(time);
				else
					Sim.fatalError("mSleepController is null when SOCKET_SLEEP is used");
				}
			
//			} else {
//				this.powerState = SocketPowerState.SOCKET_HALT;
//			}
			// Otherwise the socket stays active
		}
	}

	/**
	 * Gets the number of cores that have slots for Tasks.
	 * 
	 * @return the number of cores that are available for Tasks
	 */
	public int getRemainingCapacity() {
		//System.out.println(this.availableCores.size());
		//System.out.println(this.transitionQueue.size());
		return this.availableCores.size() - this.transitionQueue.size();
	}

	/**
	 * Gets the number of Tasks this socket can ever support.
	 * 
	 * @return the total number of cores/Tasks the socket can support.
	 */
	public int getTotalCapacity() {
		return this.nCores;
	}

	/**
	 * Gets the instant utilization of the socket (busy cores/ total cores).
	 * 
	 * @return the instant utilization of the core
	 */
	public double getInstantUtilization() {
		return ((double) this.busyCores.size() + this.transitionQueue.size())
				/ this.nCores;
	}
	
	public int getNumOfBusyCores(){
		return this.busyCores.size() + this.transitionQueue.size();
	}

	/**
	 * Gets an Vector of cores on this socket.
	 * 
	 * @return a vector of the cores on the socket
	 */
	public Vector<Core> getCores() {
		Vector<Core> combined = new Vector<Core>();
		combined.addAll(this.availableCores);
		combined.addAll(this.busyCores);

		return combined;
	}

    @Override
	public Server getServer() {
		return this.server;
	}

	/**
	 * Get the number of Tasks being serviced.
	 * 
	 * @return The number of Tasks being serviced
	 */
	public int getTasksInService() {
		return this.busyCores.size();
	}

	/**
	 * Set the power management policy of the cores in the socket.
	 * 
	 * @param corePowerPolicy
	 *            - the power management policy to use on the cores
	 */
	public void setCorePolicy(final CorePowerPolicy corePowerPolicy) {
		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while (iter.hasNext()) {
			Core core = iter.next();
			core.setPowerPolicy(corePowerPolicy);
		}
	}

	/**
	 * Set the power management policy of the socket.
	 * 
	 * @param policy
	 *            - the power management policy to set the socket to
	 */
	public void setPowerPolicy(final SocketPowerPolicy policy) {
		this.powerPolicy = policy;

		if (powerPolicy == SocketPowerPolicy.NO_MANAGEMENT) {
			this.powerState = SocketPowerState.ACTIVE;
		} else if (powerPolicy == SocketPowerPolicy.SOCKET_SLEEP) {
			// /////////////////////////////////////////////////////////////
			// parameter initializations for sleep state
			this.powerState = SocketPowerState.LOW_POWER_SLEEP;
			// this.useMultipleState = false;
			// this.nextSleepState = -1;
			// this.lastEnterSleepState = 0.0;
			// this.initializeSleepStateMaps();
			if (mSleepController != null)
				mSleepController.initialController();
			else{
				Sim.fatalError("null sleepcontroller for socket with SOCKET_SLEEP mode");
			}
		}
	}
	/**
	 * Put the socket into park.
	 * 
	 * @param time
	 *            - the time the socket is put into park
	 */
	public void enterPark(final double time) {
		if (this.busyCores.size() != 0) {
			Sim.fatalError("Socket tried to enter park when it shouldn't have");
		}

		this.powerState = SocketPowerState.LOW_POWER_SLEEP;
	}

	/**
	 * Take the socket out of park.
	 * 
	 * @param time
	 *            - the time the socket comes out of park
	 */
	public void exitPark(final double time) {
		this.powerState = SocketPowerState.ACTIVE;
		Iterator<Task> iter = this.transitionQueue.iterator();
		while (iter.hasNext()) {
			Task Task = iter.next();
			// fanyao commented: first wakeup socket, then wakeup core
			this.insertTask(time, Task);
		}
		this.transitionQueue.clear();
	}

	/**
	 * Gets the number of Tasks waiting for the socket to transition.
	 * 
	 * @return the number of Tasks waiting for the socket to transition
	 */
	public int getNTasksWaitingForTransistion() {
		return this.transitionQueue.size();
	}

	/**
	 * Pause processing at the socket.
	 * 
	 * @param time
	 *            - the time the socket processing is paused
	 */
	public void pauseProcessing(final double time) {
		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while (iter.hasNext()) {
			Core core = iter.next();
			core.pauseProcessing(time);
		}
	}

	/**
	 * Resume processing at the socket.
	 * 
	 * @param time
	 *            - the time the socket resumes processing
	 */
	public void resumeProcessing(final double time) {
		Vector<Core> cores = this.getCores();
		Iterator<Core> iter = cores.iterator();
		while (iter.hasNext()) {
			Core core = iter.next();
			core.resumeProcessing(time);
		}
	}

	/**
	 * Set the socket's active power.
	 * 
	 * @param socketActivePower
	 *            - the power consumed by the socket while active
	 */
	// public void setSocketActivePower(final double socketActivePower) {
	// this.socketActiveIdlePower = socketActivePower;
	// }

	/**
	 * Set the socket's power while parked.
	 * 
	 * @param theSocketParkPower
	 *            - the power of the socket while parked
	 */
	// public void setSocketParkPower(final double theSocketParkPower) {
	// this.socketParkPower = theSocketParkPower;
	// }

	/**
	 * Set the idle power of the socket's cores (in watts).
	 * 
	 * @param coreHaltPower
	 *            - the idle power of the socket's cores (in watts)
	 */
	// public void setCoreIdlePower(final double coreHaltPower) {
	// Iterator<Core> iter = this.getCores().iterator();
	// while (iter.hasNext()) {
	// Core core = iter.next();
	// core.setIdlePower(coreHaltPower);
	// }
	// }

	/**
	 * Set the park power of the socket's core.
	 * 
	 * @param coreParkPower
	 *            - the power of the socket's cores while parked
	 */
	// public void setCoreParkPower(final double coreParkPower) {
	// Iterator<Core> iter = this.getCores().iterator();
	// while (iter.hasNext()) {
	// Core core = iter.next();
	// core.setParkPower(coreParkPower);
	// }
	// }

	/**
	 * Sets the active power of the socket's cores (in watts).
	 * 
	 * @param coreActivePower
	 *            - the active power of the socket's cores (in watts)
	 */
	// public void setCoreActivePower(final double coreActivePower) {
	// Iterator<Core> iter = this.getCores().iterator();
	// while (iter.hasNext()) {
	// Core core = iter.next();
	// core.setActivePower(coreActivePower);
	// }
	// }

	/**
	 * Sets the DVFS speed for all the socket's cores.
	 * 
	 * @param time
	 *            - the time at which the speed is set
	 * @param speed
	 *            - the speed to set the cores to (relative to 1.0)
	 */
	public void setDvfsSpeed(final double time, final double speed) {
		Iterator<Core> iter = this.getCores().iterator();
		while (iter.hasNext()) {
			iter.next().setDvfsSpeed(time, speed);
		}
	}
	
	/* (non-Javadoc)
	 * returns the total number of task processed by all cores within current socket
	 * @see infrastructure.IActivityUnit#getFinishedTasks()
	 */
	public int getFinishedTasks(){
		Vector<Core> allCores = getCores(); 
		int totalTaskFinished = 0;
		for(IActivityUnit aUnit : allCores){
			totalTaskFinished += aUnit.getFinishedTasks();
		}
		
		return totalTaskFinished;
	}

	/**
	 * Get the current power consumption of the socket (dynamic + leakage).
	 * 
	 * @return the current power consumption of the socket
	 */
	// public double getPower() {
	// // fanyao modified power model for socket
	// double idlePower = this.getIdlePower();
	// double corePower = 0;
	// for (Core aCore : getCores()) {
	// corePower += aCore.getPower();
	// }
	//
	// return corePower + idlePower;
	// // return this.getDynamicPower() + this.getIdlePower();
	// }

	/**
	 * Get the current idle power of the socket and its cores.
	 * 
	 * @return the current idle power of the socket and its cores
	 */
	// fanyao modified: get idle power returns only socket idle power (core
	// power not involved)
	// public double getIdlePower() {
	//
	// double idlePower = 0.0d;
	// if (this.powerState == SocketPowerState.ACTIVE) {
	//
	// // Iterator<Core> coreIter = this.getCores().iterator();
	// // while (coreIter.hasNext()) {
	// // Core core = coreIter.next();
	// // double corePower = core.getIdlePower();
	// // idlePower += corePower;
	// // }
	// idlePower += Constants.SOCKET_IDLE_POWER;
	//
	// } else if (this.powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE) {
	//
	// idlePower = Constants.SOCKET_IDLE_POWER;
	//
	// } else if (this.powerState ==
	// SocketPowerState.TRANSITION_TO_LOW_POWER_IDLE) {
	//
	// idlePower = Constants.SOCKET_IDLE_POWER;
	//
	// } else if (this.powerState == SocketPowerState.LOW_POWER_IDLE) {
	//
	// idlePower = Constants.SOCKET_PARK_POWER;
	//
	// }
	//
	// return idlePower;
	// }

	/**
	 * Get the current dynamic power consumption of the socket. Modeled as the
	 * sum of the core dynamic power. Uncore power is all leakage.
	 * 
	 * @return the current dynamic power consumption of the socket
	 */
	// public double getDynamicPower() {
	//
	// double dynamicPower = 0.0d;
	//
	// Iterator<Core> coreIter = this.getCores().iterator();
	// while (coreIter.hasNext()) {
	// Core core = coreIter.next();
	// double corePower = core.getDynamicPower();
	// dynamicPower += corePower;
	// }
	//
	// return dynamicPower;
	// }

	public void setCorePState(CorePState corePState) {
		// TODO Auto-generated method stub
		Iterator<Core> coreIter = this.getCores().iterator();
		while (coreIter.hasNext()) {
			Core core = coreIter.next();
			core.setCorePState(corePState);
		}

	}

	public void setNextSleepState(int sleepState) {
		// TODO Auto-generated method stub
		Iterator<Core> coreIter = this.getCores().iterator();
		while (coreIter.hasNext()) {
			Core core = coreIter.next();
			
			if(core.hasSleepController()){
				core.getSLeepController().setNextFixedSleepState(sleepState);		
			}	
		}

	}

	public int getNumOfCores() {
		return this.nCores;
	}

//	@Override
//	public void fetchTaskToExecute(Task aTask, double time) {
//		Sim.fatalError("this is a placeholder method for socket, should not be called");
//	}

	/* (non-Javadoc)
	 * since socket can have multiple cores, single adding up the service
	 * time is incorrect. We need to eliminate the overlap. 
	 * @see infrastructure.IActivityUnit#updateTaskStats(job.Task)
	 */
	public void updateTaskStats(Task task) {
		// TODO Auto-generated method stub
		
		// this.serviceTime += task.getFinishTime() -
		// task.getStartExecutionTime();
		// if(mSleepController != null)
		// mSleepController.updateServiceTime(task.getFinishTime() -
		// task.getStartExecutionTime());
		// this.taskCompleted++;
		
		if(mSleepController != null){
			//FIXME: need to remove the timing overlap
			//this functionality should be finally shipped to SleepController
			double actualDelta  = 0.0;
			if(lastStartServiceTime - task.getStartExecutionTime() > Constants.TIMING_RRECISION){
				Sim.fatalError("fatal error: laststartservice time later than task startExecutionTime");
			}
			else{
				if(lastTaskFinishTime < 0)
					actualDelta = task.getFinishTime() - lastStartServiceTime;
				else
					actualDelta = task.getFinishTime() - lastTaskFinishTime;
			}
			
			mSleepController.updateServiceTime(actualDelta);
			
			//update last task finish time
			lastTaskFinishTime = task.getFinishTime();
			
		}
		
		IActivityUnit theUnit = taskToCoreMap.get(task);
		if(theUnit == null){
			Sim.fatalError("could not find the core that processed current task : " 
					+ task.getTaskId());
		}
		theUnit.updateTaskStats(task);
	}

	public void setSleepController(AbstractSleepController abController){
		this.mSleepController = abController;
	}
	
	public boolean timingCheck() {
		return mSleepController.timingCheck();
	}

	public void adjustSSTimes(int currentSleepState, double time) {
		

		// TODO Auto-generated method stub
		// this function assumes using fixed sleepstate
		// if (useMultipleState) {
		// Sim.fatalError("Fatal error: adjustSSTimes should only be invoked when single sleep state is used");
		// }

		// double duration = 0.0;

		// FIXME: this is a fragile design, time spent in each sleep state
		// depends on the correct record of IdleRecord
//		IdleRecord lastRecord = idleDistribution.lastElement();
		if (currentSleepState == 0) {
			if (this.powerState != SocketPowerState.TRANSITIONG_TO_ACTIVE) {
				Sim.fatalError("fatal error: "
						+ this.getServer().getNodeId()
						+ " is still active when all job finished");
			}

		}
		
		mSleepController.adjustSSTimes(currentSleepState, time);
	}
	
	@Override
	public void setActive() {
		// TODO Auto-generated method stub
		this.powerState = SocketPowerState.ACTIVE;
		if(mSleepController != null)
			mSleepController.currentSleepState = 0;
		
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return powerState == SocketPowerState.ACTIVE;
	}

	@Override
	public boolean isTransitionToActive() {
		// TODO Auto-generated method stub
		return powerState == SocketPowerState.TRANSITIONG_TO_ACTIVE;
	}

	@Override
	public void setTransitionToActive() {
		// TODO Auto-generated method stub
		this.powerState = SocketPowerState.TRANSITIONG_TO_ACTIVE;
	}


	/* (non-Javadoc)
	 * method called when socket is used as Activity Unit. 
	 * @see infrastructure.IActivityUnit#wakedupFromSleep(double)
	 */
	@Override
	public void wakedupFromSleep(double time) {
		// TODO Auto-generated method stub
		if(powerPolicy == SocketPowerPolicy.SOCKET_SLEEP)
			this.setPowerState(SocketPowerState.ACTIVE);
		else{
			Sim.fatalError("wakeupFromSleep called in unexptected core power policy : " + powerPolicy.name());
		}
		//this.powerState = SocketPowerState.ACTIVE;
		/**
		 * should only be one single task in transition queue 
		 * since the wakeup waits for no time in current implementation
		 * as this function is called only in idle state
		 */
		if(transitionQueue.size() > 1){
			Sim.fatalError("num of jobs in transitionQueue > 1");
		}
		Iterator<Task> iter = this.transitionQueue.iterator();
		while (iter.hasNext()) {
			Task Task = iter.next();
			// fanyao commented: first wakeup socket, then wakeup core
			this.insertTask(time, Task);
		}
		this.transitionQueue.clear();
		
		if(mSleepController != null){
			mSleepController.wakedupFromSleep(time);
		}

	}


	@Override
	public void startSleep(final double time, int sleepState) {
		if (busyCores.size() != 0) {
			Sim.fatalError("fatal error: socket tried to sleep when some cores are still processing jobs");
		}

		/************************************************************************/
		// fanyao added for accululating number of active server history
		if (powerState == SocketPowerState.ACTIVE) {
			if (experiment instanceof MultiServerExperiment) {
				MultiServerExperiment msExp = (MultiServerExperiment) experiment;
				msExp.updateServerHis(time, -1);
			}
		}

		
		/************************************************************************/
		this.powerState = SocketPowerState.LOW_POWER_SLEEP;
		// this is not necessary
		/* this.setNextSleepState(sleepState); */
		
		mSleepController.startSleep(time, sleepState);
	}


	@Override
	public boolean isSocket() {
		// TODO Auto-generated method stub
		return true;
	}
}
