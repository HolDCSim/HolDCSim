package infrastructure;

import constants.Constants;
import experiment.ExperimentConfig;
import utility.ServerPowerMeter;
import job.Task;
import debug.Sim;
import experiment.Experiment;
import experiment.OnOffExperiment;

/**
 * use OnOffServer for delayoff and onoff experiment which transition
 * interruption allowed, prototype in CMU paper experiments
 * @author fanyao 
 */
public class OnOffServer extends UniprocessorServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	public OnOffServer(Experiment anExperiment, DataCenter dataCenter,
//			Socket[] theSockets) {
//		super(anExperiment, dataCenter, theSockets);
//	}
	public OnOffServer(Experiment anExperiment, DataCenter dataCenter,
	Socket[] theSockets, AbstractSleepController abController, Constants.SleepUnit sleepUnit) {
		super(anExperiment, dataCenter, theSockets, abController, sleepUnit);
	}

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

			/*
			 * Task has left the systems succesufully put the server to sleep,
			 * clean the MSTransToSleepEvent
			 */
		}
		/* There is now a spot for a Task, see if there's one waiting */
		else if (!globalQueueEmpty) {
			/*
			 * for experiments with global queue fetch one task from the global
			 * queue this task is retrieved from global queue so the mServer has
			 * not been set yet
			 */
			dequeuedTask = experiment.getGlobalQueue().poll(time);
			Sim.debug(3,"mmm Time: " + time + " task " + dequeuedTask.getTaskId()
							+ " continously service by server: "
							+ this.getNodeId());

			OnOffServer tranServer = (OnOffServer) dequeuedTask.getServer();
			
			/*
			 * When busy server becomes idle, it first checks unmapped jobs
			 * if there is no unmapped jobs, the idle server would pick up
			 * one mapped job and shut down the related server in transitiion state
			 */
			if (tranServer != null) {
				Task unmappedTask = ((OnOffExperiment) experiment)
						.getUnassignedJobInQueue();
				if (unmappedTask == null) {
					// no unassigned task in the queue
					// turn the previous server off
					((OnOffExperiment) experiment).shutoffTranServer(null,
							time);
				} else {
					// give the server to the unmapped task
					unmappedTask.setServer(tranServer);
				}
			}

			dequeuedTask.setServer(this);
			this.startTaskService(time, dequeuedTask);
			this.TasksInServerInvariant++;

		}

		// Task has left the systems
		this.TasksInServerInvariant--;
		this.checkInvariants();

	}
	
	@Override
	public double generateEnergyAndPerfStats() {
		sleepable.prepareStats();

		OnOffController ofController = (OnOffController) sleepable;
		ExperimentConfig expConfig = experiment.getExpConfig();
		double speed = expConfig.getSpeed();
//		wakeupEnergy = this.getWakeupTime()
//				* ServerPowerMeter.calculateActivePower(speed);
		wakeupEnergy = ofController.C0S0Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 1)
			     + ofController.C6S3Wakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 5)
			     + ofController.offStateWakeup * ServerPowerMeter.calculateWakeupPower(expConfig, speed, 6);
		idleEnergy = sleepable.C0S0Time * ServerPowerMeter.calculateLowPower(expConfig, speed, 1)
				+ ServerPowerMeter.calculateLowPower(expConfig, speed, 6)
				* ((OnOffController)sleepable).timeInOffState;
		productiveEnergy = (sleepable.activeTime - this.getWakeupTime())
				* ServerPowerMeter.calculateActivePower(expConfig, speed);
		energy = productiveEnergy + idleEnergy + wakeupEnergy;
		return energy;
		
	}

}
