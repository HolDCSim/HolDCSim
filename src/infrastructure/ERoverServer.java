package infrastructure;

import java.util.LinkedList;

//import org.omg.DynamicAny._DynEnumStub;

import constants.Constants;
import debug.Sim;
import experiment.Experiment;
import job.Task;
import queue.MSGlobalQueue;
import utility.Pair;

/**
 * ERover server uses MultiStateCore When scheduled active, it uses single sleep
 * state (shallow) When scheduled deep sleep, it uses multiple sleep state (s4
 * and s5)
 * 
 * @author fanyao
 * 
 */
public class ERoverServer extends ShallowDeepServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinkedList<Task>  waitingTasksList;

	public LinkedList<Task> getWaitingTasksList() {
		return waitingTasksList;
	}


	public ERoverServer(Experiment anExperiment, DataCenter dataCenter,
			Socket[] theSockets, AbstractSleepController abController, Constants.SleepUnit sleepUnit) {
		super(anExperiment, dataCenter, theSockets, abController, sleepUnit);
		// TODO Auto-generated constructor stub
		waitingTasksList= new LinkedList<Task>();
	}


	@Override
	public void setScheduledActive() {
		// TODO Auto-generated method stub
		this.isScheduledActive = true;
		//NOTE: idle state is always set to 1
		sleepable.setNextFixedSleepState(1);
		this.setUseMultipleSleepStates(false);
	}

	@Override
	public void unsetScheduledActive() {
		this.isScheduledActive = false;
		this.setUseMultipleSleepStates(true);
	}

	@Override
	public int getInitialSleepState() {
		if (isScheduledActive) {
			/* shallowest sleep state */
			return 1;
		}

		else {
			return sleepable.getInitialSleepState();
		}

	}
	
	@Override
	public void removeTask(final double time, final Task task) {

		// Remove the Task from the socket it is running on
		Socket socket = this.TaskToSocketMap.remove(task);

		// Error check we could resolve which socket the Task was on
		if (socket == null) {
			Sim.fatalError("Task to Socket mapping failed");
		}
		
		if (experiment.useGlobalQueue()) {
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
			boolean taskWaiting = this.isScheduledActive && !this.getWaitingTasksList().isEmpty();
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
				Task dequeuedTask,t;
				
				if (getWaitingTasksList().size() > 0 ) {
					
					 t = ((ERoverServer)this).getWaitingTasksList().poll();
					 dequeuedTask = experiment.getGlobalQueue().polltask(time, t);
					 experiment.cancelEvent(dequeuedTask.getTaskTimerEvent());
						dequeuedTask.setTaskTimerEvent(null);
						Sim.debug(1,"--Dequeuend task at server" + this.getNodeId()+" Job "+ dequeuedTask.getJobId()+ " " + dequeuedTask.getTaskId() + " dequeed task " + experiment.printStateofServers());
						
						Sim.debug(
								1,
								"mmm Time: " + time + " task " + dequeuedTask.getTaskId()
										+ " continously service by server : "
										+ this.getNodeId());
						dequeuedTask.setServer(this);
						this.startTaskService(time, dequeuedTask);
						this.TasksInServerInvariant++;
			
				} 
//				else {
//					dequeuedTask = experiment.getGlobalQueue().poll(time);
//				}
				
				

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
			// See if we're going to schedule another job or if it can go to sleep
	        boolean taskWaiting = !this.queue.isEmpty();

			// Remove the Task from the socket (which will remove it from the core)
			socket.removeTask(time, task, taskWaiting);

			// There is now a spot for a Task, see if there's one waiting
			if (taskWaiting) {
				// first get task from local queue if possible

				Task dequeuedTask = this.queue.poll();
				this.startTaskService(time, dequeuedTask);

				// fanyao commented: collect statistics for queued jobs stats
				// SleepScaleExperiment ssExp = (SleepScaleExperiment)
				// experiment;
				curJobsInQueue -= 1;
				Pair<Double, Integer> pair = new Pair<Double, Integer>();
				pair.setFirst(time);
				pair.setSecond(curJobsInQueue);
				queuedJobsStats.add(pair);

			}

			// Task has left the systems
			this.TasksInServerInvariant--;
			this.checkInvariants();
		}
	}


	public void addToWaitingTaskList(Task _task) {
		// TODO Auto-generated method stub
		this.waitingTasksList.add(_task);
	}
	
}
