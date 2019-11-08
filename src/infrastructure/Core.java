package infrastructure;

import java.io.Serializable;
import java.util.*;

import job.Task;

import constants.Constants;
import controller.WindowedSSEnforcer;
import debug.Sim;

import event.AbstractEvent;
import event.ComputationFinishEvent;
import event.StartSleepEvent;
import event.WakedupEvent;
import event.TaskFinishEvent;
import experiment.Experiment;
import experiment.MultiServerExperiment;
import experiment.WindowedSSExperiment;
import experiment.SingletonJobExperiment.IdleRecord;
import processor.*;
import utility.FakeRecordVector;
import utility.ServerPowerMeter;

/**
 * @author fanyao
 * 
 */
public class Core  implements IActivityUnit, Serializable {

	/**
	 * The serializaiton id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The Task running on the core. Is null if there is no Task.
	 */
	protected Task task;
	
	/**
	 * the SleepController
	 */
	protected AbstractSleepController mSleepController = null;

	/**
	 * core p state
	 */
	protected CorePState corePState;
	protected CoreCState coreCState;


	/**
	 * num of tasks completed by this core
	 */
	protected int taskCompleted;

	// public static double[] sleepStateWakeups = { 0.0, 1e-6, 1e-5, 1e-4, 1e-3,
	// 1.0 };


	/****************************************************
	 * // tasks that are scheduled on this core protected Vector<Task>
	 * assignedTasks;
	 ****************************************************/

	/**
	 * The possible power states the core can be in.
	 */
	// public static enum PowerState {
	//
	// /** The core is actively processing. */
	// ACTIVE,
	//
	// /** Transitioning to park. */
	// TRANSITIONINGG_TO_LOW_POWER_IDLE,
	//
	// /** Transitioning to active from park. */
	// TRANSITIONING_TO_ACTIVE,
	//
	// /** The core is in the "halt" mode (idle). */
	// HALT,
	//
	// /** The core is not currently busy, has the same state with the socket*/
	// IDLE,
	//
	// /** The core is in park mode. */
	// LOW_POWER_SLEEP,
	//
	// };
	
	/**
	 * The possible power states the core can be in.
	 */
	public static enum PowerState {

		/** The core is actively processing. */
		ACTIVE,

		/** Transitioning to active from park. */
		TRANSITIONING_TO_ACTIVE,

		/** The core is in the "halt" mode (idle). */
		HALT,

		/** The core is in park mode. */
		LOW_POWER_SLEEP,
		
		/** core power state doesn't matter for our multi-core model*/
		BUSY,
		
		IDLE

	};

	/**
	 * The possible core power management policies.
	 */
	public static enum CorePowerPolicy {
		/** No power management. Simply go to halt when idle. */
		NO_MANAGEMENT,
		/** Transition to core parking when the core is idle. */
		CORE_SLEEP,
		
		/** power policy for multi-core processor, core always has the same
		 *  power state as the socket*/
		SYNC_TO_SOCKET
	};

	/** The current core power state. */
	protected PowerState powerState;

	/** The current power management policy. */
	protected CorePowerPolicy powerPolicy;

	/**
	 * The speed at which the core is running. The relative (1.0 is no slowdown)
	 * speed the core is operating at (determines Task completion times)
	 */
	protected double speed;

	/**
	 * The experiment the core is part of.
	 */
	protected Experiment experiment;

	/**
	 * The socket the core is part of.
	 */
	protected Socket socket;

	/**
	 * The dynamic power consumption of the core.
	 */
	// protected double dynamicPower;

	/**
	 * The power of the core while parked.
	 */
	// protected double parkPower;

	/**
	 * The idle power of the core.
	 */
	// protected double idlePower;

	/**
	 * An event representing the core transitioning, if it is happening. null
	 * otherwise.
	 */
	protected AbstractEvent transitionEvent;

	/**
	 * If the core is paused.
	 */
	protected boolean paused;

	public double power;

	public void setCorePState(CorePState corePState) {
		this.corePState = corePState;
		float frequency = corePState.getFrequency();
		float peakFrequency = corePState.getPeakFrequency();

		float voltage = corePState.getVoltage();
		float peakVoltage = corePState.getPeakVoltage();

		this.speed = frequency / peakFrequency;
		// this.dynamicPower = this.dynamicPower * (speed)
		// * (voltage / peakVoltage) * (voltage / peakVoltage);
	}

	public void setCoreCState(CoreCState coreCState) {
		this.coreCState = coreCState;
	}
	
	public void setCorePowerState(PowerState powerState) {
		this.powerState = powerState;
	}
	
//	@Override
//	public int generateNextSleepState(int currentState) {
//
//		if (currentState == deepiestState) {
//			Sim.fatalError("it is already the deepest sleep state");
//		}
//		return ++currentState;
//
//	}

	/**
	 * Constructs a new Core.
	 * 
	 * @param anExperiment
	 *            - the experiment the core is part of
	 */
	public Core(final Experiment anExperiment) {
		this.experiment = anExperiment;
		// Core starts without a Task
		this.task = null;
		//this.socket = aSocket;
		// No slowdown or speedup
		this.speed = 1.0;

		// this.useDelayOff = false;
	}
	
	public Core(final Experiment anExperiment, AbstractSleepController abSleepController) {
		this(anExperiment);
		this.mSleepController = abSleepController;

		// this.useDelayOff = false;
	}
	
	public Core(final Experiment anExperiment, AbstractSleepController abSleepController, double speed) {
		this(anExperiment, abSleepController);
		// No slowdown or speedup
		this.speed = speed;

	}

	public void setSleepController(AbstractSleepController abController){
		this.mSleepController = abController;
	}

	public void setSocket(Socket theSocket) {
		this.socket = theSocket;
	}

	/**
	 * Sets the power management currently used by the core.
	 * 
	 * @param policy
	 *            - the power management policy used by the core
	 */
	public void setPowerPolicy(final CorePowerPolicy policy) {
		this.powerPolicy = policy;
		if (powerPolicy == CorePowerPolicy.SYNC_TO_SOCKET) {
			this.powerState = PowerState.IDLE;
		} else if (powerPolicy == CorePowerPolicy.CORE_SLEEP) {
			// /////////////////////////////////////////////////////////////
			// parameter initializations for sleep state
			this.powerState = PowerState.LOW_POWER_SLEEP;
			// this.useMultipleState = false;
			// this.nextSleepState = -1;
			// this.lastEnterSleepState = 0.0;
			// this.initializeSleepStateMaps();
			if(mSleepController != null)
				mSleepController.initialController();
			else
				Sim.fatalError("null sleep controller for core with CORE_SLEEP policy");

			// fanyao commented: bighouse power mode not used
			// dynamicPower = 40.0 * (4.0 / 5.0) / 2;
			// parkPower = 0;
			// idlePower = 0;
			/*******************************************************/
			/* transitionToParkTime = 100e-6; */
			// transitionToParkTime = Constants.CORE_TRANSITION_TO_PAKR;
			this.paused = false;

			// when first initialized, set the core to be acitive
			//this.setCurrentSleepState(1);

			// assignedTasks = new Vector<Task>();

			// create idle distribution and then give it the first record
			// assume that all servers are intialized as idle;
			
			// ///////////////////////////////////////////////////////////
		}
	}

	/**
	 * Gets the power management currently used by the core.
	 * 
	 * @return the power management policy used by the core
	 */
	public CorePowerPolicy getPowerPolicy() {
		return this.powerPolicy;
	}

	/**
	 * Puts a Task on the core for the first time.
	 * 
	 * @param time
	 *            - the time the Task is inserted
	 * @param aTask
	 *            - the Task to add to the core
	 */
	public void insertTask(final double time, final Task aTask) {

		// test if the status is consistent with job dispatching mechanism
		// if(this.powerState == PowerState.ACTIVE && this.task == null){
		// Sim.fatalError("tried to schedule task to a busy core");
		// }
		// Error check that we never try to put two Tasks on one core
		if (this.task != null) {
			Sim.fatalError("Tried to insert a Task into a core"
					+ " that was already busy");
		}

		// Assign Task to core
		this.task = aTask;

		// put task to assigned tasks vector
		// assignedTasks.add(aTask);

		// fanyao added for RandomSSRunner
		/*****************************************************************/
		if (experiment instanceof WindowedSSExperiment) {
			if (this.powerState == PowerState.LOW_POWER_SLEEP) {
				double duration;
				WindowedSSEnforcer enforcer = ((WindowedSSExperiment) experiment)
						.getSSEnforcer();

				if (Double.compare(mSleepController.lastIdleTime, time) > 0)
					duration = 0.0;
				else if (Double.compare(mSleepController.lastIdleTime,
						enforcer.getCurrentWindowStartTime()) <= 0) {
					duration = time - enforcer.getCurrentWindowStartTime();

				}

				else {
					duration = time - mSleepController.lastIdleTime;
				}
				enforcer.incrementCurIdleTime(duration);
			}
		}
		/*****************************************************************/

		// fanyao comments: no entering delay
		/***************************************************************************
		 * if (this.powerState == PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) {
		 * // We need to interrupt transitioning to low power idle if
		 * (this.transitionEvent.getClass() != CoreEnteredParkEvent.class) {
		 * Sim.fatalError("Tried to cancel the wrong type of event"); }
		 * this.experiment.cancelEvent(this.transitionEvent); }
		 * 
		 * 
		 * if (this.powerState == PowerState.LOW_POWER_IDLE || this.powerState
		 * == PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) { // We need to
		 * transition out of low power double exitTime = time +
		 * this.transitionToParkTime; CoreExitedParkEvent coreExitedParkEvent =
		 * new CoreExitedParkEvent( exitTime, this.experiment, this);
		 * this.experiment.addEvent(coreExitedParkEvent); }
		 *****************************************************************************/

		/**
		 * power state could be transition_to_active (e.g. in
		 * EnergyAwareSchdueler, this is used to mark that the server has been
		 * assigned to a job)
		 */
		/**
		 * if use socket as the activity unit, then insertTask for core means the core is currently
		 * available, we do not need to bother wait it up with WakeupEvent
		 */
//		if (this.powerState != PowerState.ACTIVE && this.powerPolicy != CorePowerPolicy.SYNC_TO_SOCKET
//				&& this.powerPolicy != CorePowerPolicy.NO_MANAGEMENT ) {
		if(this.powerPolicy == CorePowerPolicy.CORE_SLEEP){
			if(this.powerState != PowerState.ACTIVE ){
				mSleepController.prepareControllerWakeup(time);
				return;
			}
		}
		if(this.powerPolicy == CorePowerPolicy.SYNC_TO_SOCKET){
			if(this.powerState == PowerState.IDLE)
				this.powerState = PowerState.BUSY;
		}
			
			// We need to transition out of low power
//			double exitTime = 0.0;
//			// consider On/off experiment
//			// if (currentSleepState != 6)
//			exitTime = time + mSleepController.transitionToActiveTime;
//			// else
//			// exitTime = time + this.rSetupTime;
//			WakedupEvent coreWakedupEvent = new WakedupEvent(exitTime,
//					this.experiment, mSleepController, mSleepController.getCurrentSleepState());
//			this.experiment.addEvent(coreWakedupEvent);
//
//			/************************************************************************/
//			// fanyao added: accumulate idle distrubtion statistics, windowed
//			// SleepScaleExperiment ssExp = (SleepScaleExperiment) experiment;
//			if (idleDistribution.size() == 0) {
//				IdleRecord firstRecord = new IdleRecord();
//				firstRecord.startIdleTime = 0.0;
//				idleDistribution.add(firstRecord);
//			}
//			IdleRecord lastRecord = idleDistribution.lastElement();
//			lastRecord.duration = time - lastRecord.startIdleTime;
//			// if we have multiple sleep states, each record will remember the
//			// deepest sleepstate
//			lastRecord.sleepstate = currentSleepState;
			/************************************************************************/

			// fanyao added: cancel next sleepState event
			// if (useMultipleState) {
			// if (currentSleepState != getLastSleepState()
			// && lastEnterSleepState != 0.0) {
			// StartSleepEvent event = getNextSleepEvent();
			// if (event == null) {
			// Sim.fatalError("fatal error, using multiple states but no valid next state found");
			// }
			// this.experiment.cancelEvent(event);
			// }
			// }
//			StartSleepEvent event = mSleepController.getNextSleepEventToCancel();
//			this.experiment.cancelEvent(event);
//			// when core is in transitioning, treate the status as active
//			// this.powerState = PowerState.TRANSITIONINGG_TO_ACTIVE;
//			// this.currentSleepState = 0;
//
//			/* before set the new state, record last sleep state before wakeup */
//			this.lastStateBeforeWakeup = currentSleepState;
//			this.setPowerState(PowerState.TRANSITIONING_TO_ACTIVE);
//			// fanyao added:
//			collectedIdleTime += time - this.getLastEnterSleepStateTime();

//		else {
			// fanyao comments: let speed changes linearly with frequency
			/* double alpha = .9; */

			double alpha = 1.0;
			double slowdown = (1 - alpha) + alpha / this.speed;
			double finishTime = time + this.task.getSize() * slowdown;
			Server server = this.socket.getServer();

			/********************************************************
			 * the event is now modified to computation finish event
			 */
			// TaskFinishEvent finishEvent = new TaskFinishEvent(finishTime,
			// experiment, aTask, server, time, this.speed);
			// aTask.setLastResumeTime(time);
			// this.experiment.addEvent(finishEvent);

			/*********************************************************/
			aTask.markBeginExecute(time);
			// Tasks that are dependent on current task
			Vector<Task> ChildTasks = this.task.getaJob().getChildTasks(this.task);
		Vector<Task> parentTasks = this.task.getaJob().getChildTasks(this.task);


				ComputationFinishEvent cfEvent = new ComputationFinishEvent(
						finishTime, experiment, aTask, server, time, this.speed);
				this.experiment.addEvent(cfEvent);
			aTask.setLastResumeTime(time);

			// Core now goes into full power state
			// this.powerState = PowerState.ACTIVE;
//		}
	}

	/**
	 * Removes a Task from the core because of Task completion.
	 * 
	 * @param time
	 *            - the time the Task is removed
	 * @param aTask
	 *            - the Task that is being removed
	 * @param taskWaiting
	 *            - if there is a Task waiting after this is removed
	 */
	public void removeTask(final double time, final Task aTask,
			final boolean taskWaiting) {
		// Error check we're not trying to remove a Task from an empty core
		if (this.task == null) {
			Sim.fatalError("Tried to remove a Task from a core"
					+ " when there wasn't one");
		}

		// Error check we're removing the correct Task
		if (!this.task.equals(aTask)) {
			Sim.fatalError("Tried to remove a Task,"
					+ "but it didn't match the Task on the core");
		}

		// Null signifies the core is idle
		this.task = null;

		// If no Task is waiting, we can begin transitioning to a low power
		// state
		if (!taskWaiting) {

			// fanyao commented: no entering delay
			/*****************************************************************
			 * if (this.powerPolicy == CorePowerPolicy.CORE_PARKING) {
			 * this.powerState = PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE;
			 * double enteredLowPowerTime = time + this.transitionToParkTime;
			 * CoreEnteredParkEvent coreEnteredParkEvent = new
			 * CoreEnteredParkEvent(enteredLowPowerTime, this.experiment, this);
			 * this.transitionEvent = coreEnteredParkEvent;
			 * this.experiment.addEvent(coreEnteredParkEvent);
			 * 
			 * }
			 *****************************************************************/
			//lastIdleTime = time;
			if (this.powerPolicy == CorePowerPolicy.CORE_SLEEP) {
//				// fanyao commented: let CoreStartSleepEvent set its power state
//				// at this time we still considered it is active
//				// this is used to distinguish servers that stay in low power
//				// idle
//				// for a period of time, otherwise in UpdateThreshold() there
//				// would
//				// be an inconsistency sleepstate = 0 but power state is low
//				// power idle
//				/* this.powerState = PowerState.LOW_POWER_IDLE; */
//
//				StartSleepEvent coreStartSleepEvent = null;
//				/*****************************************************************/
//				// fanyao commented: use multiple sleepstates
//				if (useMultipleState) {
//					coreStartSleepEvent = generateSleepEvent(time,
//							this.experiment, getInitialSleepState());
//					// enterC0S0 = coreStartSleepEvent;
//					stateToEventsMap.put(getInitialSleepState(),
//							coreStartSleepEvent);
//				}
//
//				// otherwise, use one predefined fixed low power state
//				else {
//
//					coreStartSleepEvent = generateSleepEvent(time,
//							this.experiment, nextSleepState);
//					singleStateSleep = coreStartSleepEvent;
//
//					// // if is is OnOff experiment, we need to generate random
//					// // setup times
//					// if (nextSleepState == 6) {
//					// rSetupTime = ((OnOffExperiment) this.experiment)
//					// .getNextSetupTime();
//					// }
//				}
//				/*****************************************************************/
//				this.experiment.addEvent(coreStartSleepEvent);
				if(mSleepController != null)
					mSleepController.prepareForSleep(time);
				else
					Sim.fatalError("sleepcontroller is null when CORE_SLEEP is used");

			}
			else if(this.powerPolicy == CorePowerPolicy.SYNC_TO_SOCKET){
				this.powerState = PowerState.IDLE;
			}
			else {
				this.powerState = PowerState.HALT;
			}
		}
	}



	// fanyao commented: this function is confusing and not used
	/**
	 * Gets the number of Tasks this core can currently support. 1 if idle, 0 if
	 * busy.
	 * 
	 * @return the number of Tasks the core can currently support
	 */
	public int getRemainingCapacity() {
		if (this.powerState == PowerState.HALT) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Gets the number of Tasks this core can ever support. Returns 1.
	 * 
	 * @return the number of Tasks this core can support at once (is 1)
	 */
	public int getTotalCapacity() {
		return 1;
	}

	/**
	 * Gets the instant utilization of the core.
	 * 
	 * @return the instant utilization of the core
	 */
	public double getInstantUtilization() {
		if (this.task == null) {
			return 0.0d;
		} else {
			return 1.0d;
		}
	}

	/**
	 * Get the Task running on the core. Is null if there is no Task.
	 * 
	 * @return the Task running on the core or null
	 */
	public Task getTask() {
		return this.task;
	}


	public void startSleep(final double time, int sleepState) {
		if (task != null) {
			Sim.fatalError("fatal error: trying to put job-processing server to sleep");
		}

		/************************************************************************/
		// fanyao added for accululating number of active server history
		if (powerState == PowerState.ACTIVE) {
			if (experiment instanceof MultiServerExperiment) {
				MultiServerExperiment msExp = (MultiServerExperiment) experiment;
				msExp.updateServerHis(time, -1);
			}
		}

		/************************************************************************/
		this.powerState = PowerState.LOW_POWER_SLEEP;
		// this is not necessary
		/* this.setNextSleepState(sleepState); */
		
		mSleepController.startSleep(time, sleepState);

	}


	public void wakedupFromSleep(final double time) {
		if (this.task == null) {
			Sim.fatalError("Task is null when trying to go to active");
		}

//		// fanyao added: set all the sleepstate events to null
//		// this should also reset the reference in stateEventMap
//		/***********************************************************/
//		enterC0S0 = null;
//		enterC1S0 = null;
//		enterC3S0 = null;
//		enterC6S0 = null;
//		enterC6S3 = null;
//
//		singleStateSleep = null;
//
//		/***********************************************************/
//
//		/************************************************************************/
//		// fanyao added for accumulating number of active server history
//
//		if (experiment instanceof MultiServerExperiment) {
//			MultiServerExperiment msExp = (MultiServerExperiment) experiment;
//			msExp.updateServerHis(time, 1);
//		}
//
//		/************************************************************************/

		this.task.markBeginExecute(time);

		double alpha = 1.0;
		double slowdown = (1 - alpha) + alpha / this.speed;
		double finishTime = time + this.task.getSize() * slowdown;

		// double finishTime = time + this.Task.getSize();
		Server server = this.socket.getServer();

		// Tasks that are dependent on current task


		Vector<Task> ChildTasks = this.task.getaJob().getChildTasks(this.task);
		Vector<Task> parentTasks = this.task.getaJob().getChildTasks(this.task);

			ComputationFinishEvent finishEvent = new ComputationFinishEvent(
					finishTime, experiment, task, server, time, this.speed);
			this.experiment.addEvent(finishEvent);
		task.setLastResumeTime(time);
		// this.powerState = PowerState.ACTIVE;
		
		if(powerPolicy == CorePowerPolicy.CORE_SLEEP)
			this.setPowerState(PowerState.ACTIVE);
		else{
			Sim.fatalError("wakeupFromSleep called in unexptected core power policy : " + powerPolicy.name());
		}
		
		// sleepstate 0 means core is currently active
//		this.setCurrentSleepState(0);
		if(mSleepController != null){
			mSleepController.wakedupFromSleep(time);
		}
	}

	/*
	 * unused
	 */
//	public void fetchTaskToExecute(Task aTask, double time) {
//		this.task = aTask;
//		// the behavior is same as when the core exit parked state
//		wakedupFromSleep(time);
//	}

	/**
	 * Sets the DVFS speed of the core.
	 * 
	 * @param time
	 *            - the time the speed is changed
	 * @param theSpeed
	 *            - the speed to change the core (relative to 1.0)
	 */
	public void setDvfsSpeed(final double time, final double theSpeed) {
		this.speed = theSpeed;
		// Figure out it's new completion time
		if (this.task != null && this.powerState == PowerState.ACTIVE) {

			ComputationFinishEvent comFinishEvent = this.task
					.getComputationFinishEvent();
			this.experiment.cancelEvent(comFinishEvent);

			Task theTask = comFinishEvent.getTask();
			double compSpeed = comFinishEvent.getComputationSpeed();
			double compStartTime = comFinishEvent.getCompTimeSet();
			double duration = time - compStartTime;
			// TODO Fix this magic number

			// fanyao commented: change alpha to 1.
			/* double alpha = 0.9; */
			double alpha = 1.0;

			double previousSlowdown = (1 - alpha) + alpha / compSpeed;
			double workCompleted = duration / previousSlowdown;

			theTask.setAmountCompleted(theTask.getAmountCompleted()
					+ workCompleted);

			// fanyao added:
			// System.out.println("Time: " + time + " task id: "
			// + theTask.getTaskId() + " finished "
			// + theTask.getAmountCompleted());

			double slowdown = (1 - alpha) + alpha / theSpeed;

			// be careful here
			double finishTime = time
					+ (theTask.getSize() - theTask.getAmountCompleted())
					* slowdown;
			Vector<Task> parentTasks = this.task.getaJob().getChildTasks(this.task);


			ComputationFinishEvent newFinishEvent = new ComputationFinishEvent(
					finishTime, this.experiment, theTask,
					this.socket.getServer(), time, this.speed);
			this.experiment.addEvent(newFinishEvent);
		}
	}

	/**
	 * Pauses processing at the core.
	 * 
	 * @param time
	 *            - the time the processing is paused
	 */
	public void pauseProcessing(final double time) {
		if (this.paused) {
			Sim.fatalError("Core paused when it was already paused");
		}

		this.paused = true;

		if (this.task != null) {
			double totalCompleted = this.task.getAmountCompleted()
					+ (time - this.task.getLastResumeTime()) / this.speed;

			// TODO fix this fudge factor
			if (totalCompleted > this.task.getSize() + 1e-5) {
				System.out.println("time " + time + " Task "
						+ this.task.getTaskId() + " Task size "
						+ task.getSize() + " totalCompleted " + totalCompleted
						+ " lastresume " + this.task.getLastResumeTime()
						+ " previously completed "
						+ this.task.getAmountCompleted());
				Sim.fatalError("totalCompleted can't be"
						+ "more than the Task size");
			}

			if (totalCompleted < 0) {
				Sim.fatalError("totalCompleted can't be less than 0");
			}

			if (this.task.getAmountCompleted() < 0) {
				Sim.fatalError("amountCompleted can't be less than 0");
			}
			this.task.setAmountCompleted(totalCompleted);
			TaskFinishEvent finishEvent = this.task.getTaskFinishEvent();
			this.experiment.cancelEvent(finishEvent);
		}
	}

	/**
	 * Resumes processing at the core.
	 * 
	 * @param time
	 *            - the time the processing resumes
	 */
	public void resumeProcessing(final double time) {
		if (!this.paused) {
			Sim.fatalError("Core resumed when it was already running");
		}

		this.paused = false;
		if (this.task != null) {
			double timeLeft = (this.task.getSize() - this.task
					.getAmountCompleted()) / this.speed;

			double finishTime = time + timeLeft;
			Server server = this.socket.getServer();

			if (this.task.getAmountCompleted() < 0) {
				System.out
						.println("At time "
								+ time
								+ " Task "
								+ this.task.getTaskId()
								+ " resume is creating a finish event, timeLeft is "
								+ timeLeft + " Task size "
								+ this.task.getSize() + " amount completed "
								+ this.task.getAmountCompleted());
				Sim.fatalError("amountCompleted can't be less than 0");
			}

			// TODO this is FISHY
			if (timeLeft > this.task.getSize() + 1e-6 || timeLeft < -1e6) {
				System.out
						.println("At time "
								+ time
								+ " Task "
								+ this.task.getTaskId()
								+ " resume is creating a finish event, timeLeft is "
								+ timeLeft + " Task size "
								+ this.task.getSize() + " amount completed "
								+ this.task.getAmountCompleted());
				Sim.fatalError("time left has been miscalculated");
			}

			TaskFinishEvent finishEvent = new TaskFinishEvent(finishTime,
					experiment, task, server, time, this.speed);
			task.setLastResumeTime(time);
			this.experiment.addEvent(finishEvent);
		}
	}

	/**
	 * Sets the power of the core while idle (in watts).
	 * 
	 * @param coreIdlePower
	 *            - the power of the core while idle (in watts)
	 */
	// public void setIdlePower(final double coreIdlePower) {
	// this.idlePower = coreIdlePower;
	// }

	/**
	 * Sets the park power of the core (in watts).
	 * 
	 * @param coreParkPower
	 *            - the park power of the core (in watts)
	 */
	// public void setParkPower(final double coreParkPower) {
	// this.parkPower = coreParkPower;
	// }

	/**
	 * Sets the dynamic power consumption of the core (in watts).
	 * 
	 * @param coreDynamicPower
	 *            - the dynamic power consumption of the core (in watts)
	 */
	// public void setActivePower(final double coreDynamicPower) {
	// this.dynamicPower = coreDynamicPower;
	// }

	/**
	 * Gets the total instantaneous power consumption of the core (in watts).
	 * 
	 * @return the total instantaneous power consumption of the core (in watts)
	 */
	// public double getPower() {
	// // fanyao modified power model for cores
	// return (double) ServerPowerReader.getCorePower(this.speed,
	// this.currentSleepState) / (double) socket.getNumOfCores();
	// /** return this.getDynamicPower() + this.getIdlePower(); */
	// }

	/**
	 * Gets the instantaneous dynamic power component of the core (in watts).
	 * 
	 * @return the instantaneous dynamic power component of the core (in watts).
	 */
	// public double getDynamicPower() {
	// if (this.powerState == PowerState.ACTIVE) {
	// return this.dynamicPower - this.idlePower;
	// } else {
	// return 0.0d;
	// }
	// }

	/**
	 * Gets the instantaneous idle power component of the core (leakage) (in
	 * watts).
	 * 
	 * @return the instantaneous idle power component of the core (leakage) (in
	 *         watts).
	 */
	// public double getIdlePower() {
	// if (this.powerState == PowerState.ACTIVE) {
	// return this.idlePower;
	// } else if (this.powerState == PowerState.LOW_POWER_SLEEP) {
	// return this.parkPower;
	// } else if (this.powerState == PowerState.TRANSITIONING_TO_ACTIVE) {
	// // No power is saved during transitions
	// return this.dynamicPower;
	// // fanyao commented: this state is not used
	// } else if (this.powerState ==
	// PowerState.TRANSITIONINGG_TO_LOW_POWER_IDLE) {
	// // No power is saved during transitions
	// return this.dynamicPower;
	// } else if (this.powerState == PowerState.HALT) {
	// return this.idlePower;
	// } else {
	// Sim.fatalError("Unknown power setting");
	// return 0;
	// }
	// }

	public PowerState getCurrentState() {
		return this.powerState;
	}

	
	public double getTransitionToActiveTime(int lastSleepState) {
		if (this.powerState == PowerState.ACTIVE) {
			Sim.fatalError("core is currently acitve, should't query for transition to active time");
		}

		return mSleepController.getTransitionToActiveTime();
	}

	//FIXME: could be removed by automatically do this after some point when simulation finished
	public void prepareStats() {
		mSleepController.prepareStats();
		// collect performance and power related stats
//		this.activeTime = this.serviceTime + this.wakeupTime;
//		this.idleTime = experiment.getSimulationTime() - this.activeTime;
//		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
//				+ C6S3Time;
//		this.jobProcessingTime = activeTime - getWakeupTime();
	}

//	public double generateEnergyAndPerfStats() {
//		// similar to sleepscale experiment statistics
//		// now for single core
//		power = 0.0;
//		energy = 0.0;
//
//		/** statistics for nonRamdom experiment **/
//		// calculate energy and power
//		wakeupEnergy = 0.0;
//		idleEnergy = 0.0;
//		productiveEnergy = 0.0;
//
//		/********************************************************
//		 * //update service time through TaskFinishEvent // double
//		 * tempServiceTime = 0.0; for (Task aTask : assignedTasks) { //
//		 * tempServiceTime += task.getSize() // /
//		 * dataCenter.getExpConfig().getSpeed(); this.serviceTime +=
//		 * aTask.getFinishTime() - aTask.getStartExecutionTime(); }
//		 *********************************************************/
//
//		// collect performance and power related stats
//		this.activeTime = this.serviceTime + this.getWakeupTime();
//		this.idleTime = experiment.getSimulationTime() - this.activeTime;
//		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
//				+ C6S3Time;
//		this.jobProcessingTime = activeTime - getWakeupTime();
//
//		/***************************************************************
//		 * // check timing if (experiment.getExpConfig().doTimingCheck()) { if
//		 * (!timingCheck(activeTime, idleTime, collectedIdleTime)) {
//		 * 
//		 * } }
//		 ***************************************************************/
//
//		// /*******************************************/
//		// //temporary code for calculate alwayson experiment power
//		// wakeupEnergy = this.getWakeupTime() * 200;
//		// idleEnergy = C0S0Time * 140;
//		// productiveEnergy = (this.activeTime - this.getWakeupTime()) * 200;
//		// /*******************************************/
//
//		wakeupEnergy = this.getWakeupTime()
//				* ServerPowerReader.calculateActivePower(speed);
//		idleEnergy = C0S0Time * ServerPowerReader.calculateLowPower(speed, 1)
//				+ C1S0Time * ServerPowerReader.calculateLowPower(speed, 2)
//				+ C3S0Time * ServerPowerReader.calculateLowPower(speed, 3)
//				+ C6S0Time * ServerPowerReader.calculateLowPower(speed, 4)
//				+ C6S3Time * ServerPowerReader.calculateLowPower(speed, 5);
//
//		productiveEnergy = (this.activeTime - this.getWakeupTime())
//				* ServerPowerReader.calculateActivePower(speed);
//		energy = productiveEnergy + idleEnergy + wakeupEnergy;
//
//		return energy;
//	}


	public PowerState getPowerState() {
		return powerState;
	}

	@Override
	public void initialLowPowerState(){
		this.powerState = PowerState.LOW_POWER_SLEEP;
	}

	public void setPowerState(PowerState powerState) {
		// TODO Auto-generated method stub
		this.powerState = powerState;

		// fanyao added:
		// don't invoke setCurrentSleepstate as it will also update
		// transition_to_active time, the old transition_to_active time
		// would be used in CoreWakeupEvent
		/*
		 * the following operations are not necessary since when all job
		 * finished we use isTransitToActive to query the server state instead
		 * of sleepstate
		 */
		if (mSleepController != null) {
			if (powerState == PowerState.TRANSITIONING_TO_ACTIVE)
				mSleepController.currentSleepState = 0;
			if (powerState == PowerState.ACTIVE)
				mSleepController.setCurrentSleepState(0);
		}
	}

	public Socket getSocket() {
		return this.socket;
	}

	public int getFinishedTasks() {
		return taskCompleted;
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
			if (this.powerState != PowerState.TRANSITIONING_TO_ACTIVE) {
				Sim.fatalError("fatal error: "
						+ this.socket.getServer().getNodeId()
						+ " is still active when all job finished");
			}

		}
		
		mSleepController.adjustSSTimes(currentSleepState, time);

		// query idle distribution to get last time the server is tried to
		// wakeup
		// double delta = time - (time - lastRecord.startIdleTime +
		// lastRecord.duration);
//		double delta = (time - lastRecord.startIdleTime - lastRecord.duration);
//
//		this.setWakeupTime(delta + wakeupTime);
//
//		// need to craft time in order to call accumulateSSTimes
//		// accumulateSSTimes(lastRecord.sleepstate, time
//		// - lastRecord.startIdleTime
//		// + sleepStateWakeups[lastRecord.sleepstate - 1]);
//
//		accumulateSSTimes(lastRecord.sleepstate, lastRecord.duration
//				+ sleepStateWakeups[lastRecord.sleepstate - 1]);
//		/*
//		 * switch (lastRecord.sleepstate) { // when all job finished, the
//		 * current sleep state of this server is // 0 // means it is in
//		 * transition state
//		 * 
//		 * case 1: C0S0Time += duration; C0S0Count++; break; case 2: C1S0Time +=
//		 * duration; C1S0Count++; break; case 3: C3S0Time += duration;
//		 * C3S0Count++; break; case 4: C6S0Time += duration; C6S0Count++; break;
//		 * case 5: C6S3Time += duration; C6S3Count++; break;
//		 * 
//		 * default: break;
//		 * 
//		 * }
//		 */

	}



	// public void updateTempIdleTime(double delta) {
	// // TODO Auto-generated method stub
	// this.tempIdleTime += delta;
	//
	// }

	public boolean timingCheck() {
    	return mSleepController.timingCheck();

//		if (Math.abs(collectedIdleTime - idleTime) > Constants.TIMING_RRECISION) {
//			Sim.warning("timing check failed, expected idle != collected Idle for server : "
//					+ this.getSocket().getServer().getNodeId());
//			return false;
//		}
//
//		if (useMultipleState) {
//			Sim.warning("no distribution check in for cores with multiple states");
//			return true;
//		}
//
//		// might be a FakeRecordVector, ignore distribution check
//		if (idleDistribution.size() <= 1) {
//			Sim.debug(3, "FakeRecordVector used, ignore distribution check");
//			return true;
//		}
//
//		// distribution check
//		double collectedC0S0Time = 0.0;
//		double collectedC1S0Time = 0.0;
//		double collectedC3S0Time = 0.0;
//		double collectedC6S0Time = 0.0;
//		double collectedC6S3Time = 0.0;
//
//		for (IdleRecord record : idleDistribution) {
//			switch (record.sleepstate) {
//			case 1:
//				collectedC0S0Time += record.duration;
//				break;
//			case 2:
//				collectedC1S0Time += record.duration;
//				break;
//			case 3:
//				collectedC3S0Time += record.duration;
//				break;
//			case 4:
//				collectedC6S0Time += record.duration;
//				break;
//			case 5:
//				collectedC6S3Time += record.duration;
//				break;
//			default:
//				Sim.fatalError("unknown sleep state when performing timing check");
//			}
//		}
//
//		if (Math.abs(collectedC0S0Time - C0S0Time) > Constants.TIMING_RRECISION
//				|| Math.abs(collectedC1S0Time - C1S0Time) > Constants.TIMING_RRECISION
//				|| Math.abs(collectedC3S0Time - C3S0Time) > Constants.TIMING_RRECISION
//				|| Math.abs(collectedC6S0Time - C6S0Time) > Constants.TIMING_RRECISION
//				|| Math.abs(collectedC6S3Time - C6S3Time) > Constants.TIMING_RRECISION) {
//			Sim.warning("collected sleep state distribution incorrect");
//			return false;
//		}
//
//		else
//			return true;

	}

//	public int getLastSleepState() {
//		// TODO Auto-generated method stub
//		return sleepStateWakeups.length;
//	}


	public void updateTaskStats(Task task) {
		// TODO Auto-generated method stub
		
//		this.serviceTime += task.getFinishTime() - task.getStartExecutionTime();
		if(mSleepController != null)
			mSleepController.updateServiceTime(task.getFinishTime() - task.getStartExecutionTime());
		this.taskCompleted++;

	}
	
	public int getUnitId(){
		return this.getSocket().getServer().getNodeId();
	}
	
	public Server getServer(){
		return getSocket().getServer();
	}

	public void setActive() {
		// TODO Auto-generated method stub
		this.powerState = PowerState.ACTIVE;
		
	}

	public boolean isActive() {
		// TODO Auto-generated method stub
		return powerState == PowerState.ACTIVE;
	}

	public boolean isTransitionToActive() {
		// TODO Auto-generated method stub
		return powerState == PowerState.TRANSITIONING_TO_ACTIVE;
	}

	public void setTransitionToActive() {
		// TODO Auto-generated method stub
		this.powerState = PowerState.TRANSITIONING_TO_ACTIVE;
	}
	
	public boolean hasSleepController(){
		return mSleepController != null;
	}
	
	public AbstractSleepController getSLeepController(){
		return mSleepController;
	}

	@Override
	public boolean isSocket() {
		return false;
	}
}
