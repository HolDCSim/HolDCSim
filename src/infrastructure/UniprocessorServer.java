package infrastructure;

//import java.util.HashMap;
//import java.util.Map;
import infrastructure.Core.PowerState;
import infrastructure.Socket.SocketPowerState;

import java.util.Map;
import java.util.Vector;

import constants.Constants;

import utility.ServerPowerMeter;

import job.Task;

import debug.Sim;
import experiment.Experiment;
import experiment.SingletonJobExperiment;
import experiment.ExperimentConfig;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * UniprocessorServer has a single SleepController and is able to fetch job from either local
 * queue or global queue SleepScale Experiment or further experiment class that
 * subclassed SleepScale experiment may use this class
 * 
 * @author fanyao
 */
public class UniprocessorServer extends EnergyServer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private Map<Double, Integer> serverStatusHis;

//	public UniprocessorServer(Experiment anExperiment, DataCenter dataCenter) {
//		super(1, 1, anExperiment, dataCenter);
//
//		// serverStatusHis = new HashMap<Double, Integer>();
//		// TODO Auto-generated constructor stub
//	}
	
	public double C0S0IdleEnergy;
	public double C1S0IdleEnergy;
	public double C3S0IdleEnergy;
	public double C6S0IdleEnergy;
	public double C6S3IdleEnergy;
	
	public double C0S0WakeupEnergy;
	public double C1S0WakeupEnergy;
	public double C3S0WakeupEnergy;
	public double C6S0WakeupEnergy;
	public double C6S3WakeupEnergy;

	public UniprocessorServer(Experiment anExperiment, DataCenter dataCenter,
			Socket[] theSockets, AbstractSleepController abController, Constants.SleepUnit sleepUnit) {
		super(anExperiment, dataCenter, theSockets, abController, sleepUnit);
	}

	// the energy is calculated by the core
	@Override
	public double generateEnergyAndPerfStats() {
		sleepable.prepareStats();
		
		// collect performance and power related stats
		/***************************************************************
		 * // check timing if (experiment.getExpConfig().doTimingCheck()) { if
		 * (!timingCheck(activeTime, idleTime, collectedIdleTime)) {
		 * 
		 * } }
		 ***************************************************************/

		// /*******************************************/
		// //temporary code for calculate alwayson experiment power
		// wakeupEnergy = this.getWakeupTime() * 200;
		// idleEnergy = C0S0Time * 140;
		// productiveEnergy = (this.activeTime - this.getWakeupTime()) * 200;
		// /*******************************************/

		ExperimentConfig expConfig = experiment.getExpConfig();
		double speed = expConfig.getSpeed();
		C0S0WakeupEnergy = sleepable.C0S0Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 1);
		C1S0WakeupEnergy = sleepable.C1S0Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 2);
		C3S0WakeupEnergy = sleepable.C3S0Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 3);
		C6S0WakeupEnergy = sleepable.C6S0Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 4);
		C6S3WakeupEnergy = sleepable.C6S3Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 5);;
		
		wakeupEnergy = C0S0WakeupEnergy + C1S0WakeupEnergy + C3S0WakeupEnergy + C6S0WakeupEnergy + C6S3WakeupEnergy;
		//wakeupEnergy = this.getWakeupTime() * ServerPowerMeter.calculateActivePower(speed);
		C0S0IdleEnergy      = sleepable.C0S0Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 1);
		C1S0IdleEnergy		= sleepable.C1S0Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 2);
		C3S0IdleEnergy		= sleepable.C3S0Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 3);
		C6S0IdleEnergy		= sleepable.C6S0Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 4);
		C6S3IdleEnergy		= sleepable.C6S3Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 5);
		
		idleEnergy = C0S0IdleEnergy + C1S0IdleEnergy + C3S0IdleEnergy + C6S0IdleEnergy + C6S3IdleEnergy;
		
		if ((this.experiment instanceof SingletonJobExperiment) &&  
			    (((SingletonJobExperiment) this.experiment).sleepUnit == Constants.SleepUnit.NO_SLEEP)) {
			idleEnergy+= sleepable.idleTime * ServerPowerMeter.calculateActivePower(mDataCenter.getExpConfig(), speed);
		}
	

		productiveEnergy = (sleepable.activeTime - this.getWakeupTime())
				* ServerPowerMeter.calculateActivePower(expConfig, speed);
		energy = productiveEnergy + idleEnergy + wakeupEnergy;

		return energy;
		
	
	}

	//modified to support multi-core scenario
	/**
	 * if all cores are busy or the server is in transitioning
	 * @return
	 */
	public boolean isServerAllBusy() {
//		if (activityUnit.isActive()
//				|| activityUnit.isTransitionToActive()) {
//			return true;
//		} else
//			return false;
		if(activityUnit.isActive()){
			if(this.getRemainingCapacity() > 0){
				return false;
			}
			else return true;
		}
		
		else if(activityUnit.isTransitionToActive()){
			return true;
		}
		
		else
			return false;
		

	}
	
	/**
	 * if any cores are busy or server just finished the last task but the 
	 * corresponding sleep event is not processed or the server is in 
	 * transitioning state
	 * @return
	 */
	public boolean isServerBusy() {
//		if (activityUnit.isActive()
//				|| activityUnit.isTransitionToActive()) {
//			return true;
//		} else
//			return false;
		
		return this.isActive() || this.isTransitionToActive();
		

	}
	

//	public void setPowerState(Core.PowerState powerState) {
//		mCore.setPowerState(powerState);
//	}
//
//	public PowerState getPowerState() {
//		return mCore.getPowerState();
//	}

	@Override
	public void removeTask(final double time, final Task task) {

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
		boolean localTaskWaiting = !this.queue.isEmpty();
		// Remove the Task from the socket (which will remove it from the core)
		socket.removeTask(time, task, localTaskWaiting || !globalQueueEmpty);

		Task dequeuedTask = null;
		if (localTaskWaiting) {
			dequeuedTask = this.queue.poll();
			this.startTaskService(time, dequeuedTask);

			// Task has left the systems
			// succesufully put the server to sleep, clean the
			// MSTransToSleepEvent
		}
		// There is now a spot for a Task, see if there's one waiting
		else if (!globalQueueEmpty) {
			// for experiments with global queue
			// fetch one task from the global queue
			// this task is retrieved from global queue so the mServer has
			// not been set yet
			dequeuedTask = experiment.getGlobalQueue().poll(time);
			Sim.debug(
					3,
					"mmm Time: " + time + " task " + dequeuedTask.getTaskId()
							+ " continously service by server: "
							+ this.getNodeId());
			dequeuedTask.setServer(this);
			this.startTaskService(time, dequeuedTask);
			this.TasksInServerInvariant++;

		}

		// Task has left the systems
		this.TasksInServerInvariant--;
		this.checkInvariants();

	}

	public int getFinishedTasks() {
		return activityUnit.getFinishedTasks();
	}

	// public void setSleepState(int sleepState) {
	// mCore.setFixedSleepState(sleepState);
	// }


	// public void updateTempIdleTime(double delta){
	// mCore.updateTempIdleTime(delta);
	// }

	public void updateTaskStats(Task task) {
		activityUnit.updateTaskStats(task);
	}
	
	@Override
	public void setActive(){
		activityUnit.setActive();
	}
	
	@Override
	public boolean isTransitionToActive(){
		return activityUnit.isTransitionToActive(); 
	}
	
	
		
	@Override
	public boolean isActive(){
		return activityUnit.isActive();
	}
	
	@Override
	public void setTransitionToActive(){
		activityUnit.setTransitionToActive();
	}

}
