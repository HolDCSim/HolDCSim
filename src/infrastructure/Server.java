package infrastructure;

import infrastructure.Core.CorePowerPolicy;
import infrastructure.Port.PortPowerState;
import infrastructure.Socket.SocketPowerPolicy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import stat.TimeWeightedStatistic;
import debug.Sim;
import experiment.Experiment;
import utility.*;

import job.Task;

import constants.Constants;
import constants.Constants.NodeState;
import processor.CorePState;

/**
 * This class represents a physical server in a data center. It behaves much
 * like a queuing theory queue with servers equal to the number of cores. A
 * server has a set of sockets which are the physical chips, each with a number
 * of cores.
 * 
 * 
 */
public class Server extends DCNode implements Schedulable,
		Serializable {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The scheduling algorithm for assigning Tasks to sockets.
	 */
	public enum Scheduler {

		/**
		 * Bin packing means trying to fill a socket with Tasks before assigning
		 * Tasks to any other.
		 */
		BIN_PACK,

		/**
		 * Load balancing assigns Tasks to all sockets equally.
		 */
		LOAD_BALANCE
	};

	// platform power based on sleep states from 1 ~ 5
	public static double[] platformPower = { 120.0, 60.5, 60.5, 60.5, 13.1 };

	public enum serverState {
		ACTIVE, OFF
	};
	
	/**
	 * local task scheduler for current server
	 */
	protected Scheduler scheduler;
	
	public static serverState powerState;

	/**
	 * The server's sockets.
	 */
	protected Socket[] sockets;

	/**
	 * Map that saves which socket has a Task to avoid searching.
	 */
	protected HashMap<Task, Socket> TaskToSocketMap;

	/**
	 * The experiment the server is running in.
	 */
	public Experiment experiment;

	/**
	 * Flag for if the server is paused.
	 */
	protected boolean paused;

	/**
	 * Queue to put Tasks in when cores are busy.
	 */
	protected LinkedList<Task> queue;

	/**
	 * A variable to track the number of Tasks in the system and double check
	 * the rest of the logic doesn't add/drop Tasks.
	 */
	public int TasksInServerInvariant;

	/**
	 * global state, used to calculate system platform power
	 * 
	 */
	protected int globalState;

	protected int curJobsInQueue;
	protected Vector<Pair<Double, Integer>> queuedJobsStats;
	
	protected double activeStartTime;
	protected double activeEndTime;
	protected double offStartTime;
	protected double offEndTime;
	protected double activeTime;
	protected double offTime;

	/**
	 * Creates a new server.
	 * 
	 * @param theNumberOfSockets
	 *            - the number of sockets in the server
	 * @param theCoresPerSocket
	 *            - the number of cores per socket
	 * @param anExperiment
	 *            - the experiment the core is part of
	 * @param anArrivalGenerator
	 *            - the interarrival time generator
	 * @param aServiceGenerator
	 *            - the service time generator
	 */
	public Server(final int theNumberOfSockets, final int theCoresPerSocket,
			final Experiment anExperiment, final DataCenter dataCenter) {
		super(dataCenter);
		this.experiment = anExperiment;
		this.queue = new LinkedList<Task>();
		this.sockets = new Socket[theNumberOfSockets];
		for (int i = 0; i < theNumberOfSockets; i++) {
			this.sockets[i] = new Socket(experiment, theCoresPerSocket);
			//fanyao added: container refernece gets set when container is constructed
			this.sockets[i].setServer(this);
		}
		this.TaskToSocketMap = new HashMap<Task, Socket>();
		this.scheduler = Scheduler.LOAD_BALANCE;
		this.TasksInServerInvariant = 0;
		this.paused = false;
		this.queuedJobsStats = new Vector<Pair<Double, Integer>>();

		// default primary core sleep state
		globalState = 1;
		
		this.activeStartTime = 0.0;
		this.activeEndTime = 0.0;
		this.offStartTime = 0.0;
		this.offEndTime = 0.0;
		this.activeTime = 0.0;
		this.offTime = 0.0;
		
		this.powerState = serverState.OFF;
		
	}
	
	
	
	/**
	 * construct server by passing the sockets with custom core settings
	 * @param anExperiment
	 * @param dataCenter
	 * @param theSockets
	 */
	public Server(Experiment anExperiment, DataCenter dataCenter,
			Socket[] theSockets) {
		// TODO Auto-generated constructor stub
		super(dataCenter);
		
		if(theSockets == null){
			Sim.fatalError("assigned sockets is null");
		}
		
		this.experiment = anExperiment;
		this.queue = new LinkedList<Task>();
		this.sockets = theSockets;

		this.TaskToSocketMap = new HashMap<Task, Socket>();
		this.scheduler = Scheduler.LOAD_BALANCE;
		this.TasksInServerInvariant = 0;
		this.paused = false;
		this.queuedJobsStats = new Vector<Pair<Double, Integer>>();


		// default primary core sleep state
		globalState = 1;
		
		this.activeStartTime = 0.0;
		this.activeEndTime = 0.0;
		this.offStartTime = 0.0;
		this.offEndTime = 0.0;
		this.activeTime = 0.0;
		this.offTime = 0.0;
		
		this.powerState = serverState.OFF;
	}
	
	public void setPowerState(serverState state) {
		this.powerState = state;
	}
	
	public serverState getPowerState() {
		return this.powerState;
	}
	
	public void updateActiveTime (double time) {
		this.activeTime = (time - activeStartTime) + activeTime;
	}
	
	public void setActiveStartTime (double time) {
		this.activeStartTime = time;
	}
	
	public void setActiveEndTime (double time) {
		this.activeEndTime = time;
	}
	
	public void setOffStartTime (double time) {
		this.offStartTime = time;
	}
	
	public void setOffEndTime (double time) {
		this.offEndTime = time;
	}
	
	public double getOffEndTime () {
		return this.offEndTime;
	}
	
	public void updateOffTime (double time) {
		this.offTime = (time - offStartTime) + offTime;
	}
	
	public double getActiveEndTime () {
		return this.activeEndTime;
	}
	
	public double getActiveTime () {
		return this.activeTime;
	}
	
	public double getOffTime () {
		return this.offTime;
	}


	/**
	 * Pauses the server. No processing occurs.
	 */
	public final void pause() {
		this.paused = true;
	}

	/**
	 * Unpauses the server. Processing resumes.
	 */
	public final void unpause() {
		this.paused = false;
	}

	/**
	 * Returns if the server is paused of not.
	 * 
	 * @return if the server is paused
	 */
	public final boolean isPaused() {
		return this.paused;
	}

	/**
	 * Creates a new arrival for the server.
	 * 
	 * @param time
	 *            - the time the new arrival is created
	 */
	public final void createNewArrival(final double time) {
		/*
		 * double interarrivalTime = this.arrivalGenerator.next(); double
		 * arrivalTime = time + interarrivalTime; double serviceTime =
		 * this.serviceGenerator.next(); Statistic arrivalStat =
		 * this.experiment.getStats().getStat( StatName.GENERATED_ARRIVAL_TIME);
		 * arrivalStat.addSample(interarrivalTime); Statistic serviceStat =
		 * this.experiment.getStats().getStat( StatName.GENERATED_SERVICE_TIME);
		 * serviceStat.addSample(serviceTime);
		 * 
		 * Task task = new Task(serviceTime); TaskArrivalEvent TaskArrivalEvent
		 * = new TaskArrivalEvent(arrivalTime, experiment, task, this);
		 * this.experiment.addEvent(TaskArrivalEvent);
		 */
	}

	// /**
	// * Assigns a Task to
	// * @param time
	// * @param socket
	// * @param Task
	// */
	// public void setSocketTaskMapping(double time, Socket socket, Task Task) {
	// this.TaskToSocketMap.put(Task, socket);
	// }

	/**
	 * Inserts a Task into the server. This method is called when a Task FIRST
	 * arrives at a server (not started processing of resumed etc)
	 * 
	 * @param time
	 *            - the time the Task is inserted
	 * @param Task
	 *            - the Task that is inserted
	 */
	public void insertTask(final double time, final Task task) {
		// Check if the Task should be serviced now or put in the queue
		if (this.getRemainingCapacity() == 0) {
			// There was no room in the server, put it in the queue
			this.queue.add(task);

			// fanyao commented: collect statistics for queued jobs stats
			// SleepScaleExperiment ssExp = (SleepScaleExperiment) experiment;
			curJobsInQueue += 1;
			Pair<Double, Integer> pair = new Pair<Double, Integer>();
			pair.setFirst(time);
			pair.setSecond(curJobsInQueue);
			queuedJobsStats.add(pair);

		} else {
			// The Task can start service immediately
			this.startTaskService(time, task);
		}
		//System.out.println("qqqqqqqqqqqqqqqqqq"+this.queue.size());
		// Task has entered the system
		this.TasksInServerInvariant++;
		checkInvariants();
	}

	/**
	 * Gets the number of Tasks in the server.
	 * 
	 * @return the number of Tasks in the server
	 */
	public final int getTasksInSystem() {
		// Tasks that need to be counted for socket parking
		int transTasks = 0;
		for (int i = 0; i < this.sockets.length; i++) {
			transTasks += this.sockets[i].getNTasksWaitingForTransistion();
		}

		int TasksInSystem = this.getQueueLength() + this.getTasksInService()
				+ transTasks;
		return TasksInSystem;
	}

	/**
	 * Runs sanity check to make sure we didn't lose a Task.
	 */
	public final void checkInvariants() {
		int TasksInSystem = this.getTasksInSystem();
		if (TasksInSystem != this.TasksInServerInvariant) {
			Sim.fatalError("From insert: Task balance is off.");
		}
	}

	/**
	 * Update the statistics monitoring the server.
	 * 
	 * @param time
	 *            - the time the update occurs
	 */
	public void updateStatistics(final double time) {
		TimeWeightedStatistic serverPowerStat = this.experiment.getStats()
				.getTimeWeightedStat(
						Constants.TimeWeightedStatName.SERVER_POWER);
		serverPowerStat.setRowSize(this.mDataCenter.getNumOfServers());

		// fan yao, calculate lag spacing has some bugs
		// double temp = this.getPower();
		//serverPowerStat.addSample(this.getPower(), time);

		TimeWeightedStatistic serverthroughput = this.experiment.getStats()
				.getTimeWeightedStat(Constants.TimeWeightedStatName.THROUGHPUT);
		serverthroughput.setRowSize(this.mDataCenter.getNumOfServers());
		serverthroughput.addSample(this.getThroughput(), time);

		TimeWeightedStatistic serverUtilStat = this.experiment.getStats()
				.getTimeWeightedStat(
						Constants.TimeWeightedStatName.SERVER_UTILIZATION);
		serverUtilStat.setRowSize(this.mDataCenter.getNumOfServers());
		serverUtilStat.addSample(this.getInstantUtilization(), time);

		// if (LogWriter.checkBufferedLog(
		// Constants.TimeWeightedStatName.THROUGHPUT,
		// serverthroughput.getFullHistory())) {
		// serverthroughput.setFullHistory();
		// }
		// if (LogWriter.checkBufferedLog(
		// Constants.TimeWeightedStatName.SERVER_UTILIZATION,
		// serverUtilStat.getFullHistory())) {
		// serverUtilStat.setFullHistory();
		// }
		// if (LogWriter.checkBufferedLog(
		// Constants.TimeWeightedStatName.SERVER_POWER,
		// serverPowerStat.getFullHistory())) {
		// serverPowerStat.setFullHistory();
		// }

		double idleness = 1.0;
		if (this.isIdle()) {
			idleness = 0.0;
		}

		TimeWeightedStatistic serverIdleStat = this.experiment.getStats()
				.getTimeWeightedStat(
						Constants.TimeWeightedStatName.SERVER_IDLE_FRACTION);
		serverIdleStat.addSample(idleness, time);
	}

	// TODO what if its paused?
	/**
	 * Check if the server is idle (no Tasks).
	 * 
	 * @return if the server is idle
	 */
	public final boolean isIdle() {
		return this.getTasksInSystem() > 0;
	}

	/**
	 * Get the length of the server queue.
	 * 
	 * @return the length of the server queue
	 */
	public final int getQueueLength() {
		return this.queue.size();
	}

	/**
	 * Get the remaining capacity of the server (in Tasks).
	 * 
	 * @return the remaining capacity of the server (in Tasks)
	 */
	public final int getRemainingCapacity() {
		int capacity = 0;
		for (int i = 0; i < this.sockets.length; i++) {
			capacity += this.sockets[i].getRemainingCapacity();
		}
		return capacity;
	}

	/**
	 * Gets the number of Tasks this server can ever support.
	 * 
	 * @return the number of Tasks this server can ever support
	 */
	public final int getTotalCapacity() {
		int nTasks = 0;
		for (int i = 0; i < this.sockets.length; i++) {
			nTasks += this.sockets[i].getTotalCapacity();
		}
		return nTasks;
	}
	
	public boolean isServerAllIdle(){
		return getNumOfBusyCores() == 0; 
	}
	protected int getNumOfBusyCores(){
		int busyCores = 0;
		for (int i = 0; i < this.sockets.length; i++) {
			busyCores += this.sockets[i].getNumOfBusyCores();
		}
		return busyCores;
	}

	/**
	 * Gets the number of Tasks currently being processed.
	 * 
	 * @return - the number of Tasks currently being processed
	 */
	public int getTasksInService() {
		int nInService = 0;
		for (int i = 0; i < this.sockets.length; i++) {
			nInService += this.sockets[i].getTasksInService();
		}

		return nInService;
	}

	/**
	 * This method is called when the Task first starts service. (When it first
	 * arrives in and there's spare capacity or when it is taken out of the
	 * queue for the first time).
	 * 
	 * @param time
	 *            - the time the Task is started
	 * @param Task
	 *            - the Task that starts
	 */
	public void startTaskService(final double time, final Task Task) {
		Socket targetSocket = null;
		Socket mostUtilizedSocket = null;
		double highestUtilization = Double.MIN_VALUE;
		Socket leastUtilizedSocket = null;
		double lowestUtilization = Double.MAX_VALUE;

		for (int i = 0; i < this.sockets.length; i++) {
			Socket currentSocket = this.sockets[i];
			double currentUtilization = currentSocket.getInstantUtilization();

			if (currentUtilization > highestUtilization
					&& currentSocket.getRemainingCapacity() > 0) {
				highestUtilization = currentUtilization;
				mostUtilizedSocket = currentSocket;
			}

			if (currentUtilization < lowestUtilization
					&& currentSocket.getRemainingCapacity() > 0) {
				lowestUtilization = currentUtilization;
				leastUtilizedSocket = currentSocket;
			}
//			leastUtilizedSocket = currentSocket;

		}
		//System.out.println("task"+Task+"mostUtilizedSocket"+mostUtilizedSocket+"leastUtilizedSocket"+leastUtilizedSocket);
		
		// Pick a socket to put the Task on depending on the scheduling policy
		if (this.scheduler == Scheduler.BIN_PACK) {
			targetSocket = mostUtilizedSocket;
		} else if (this.scheduler == Scheduler.LOAD_BALANCE) {
			targetSocket = leastUtilizedSocket;
		} else {
			Sim.fatalError("Bad scheduler");
		}

		// fanyao comments: our definition of mark
		Task.markStart(time);
		// if(targetSocket == null){
		// targetSocket = sockets[0];
		// }
		targetSocket.insertTask(time, Task);
		this.TaskToSocketMap.put(Task, targetSocket);
		//System.out.println("task"+Task+"targetSocket"+targetSocket);
		//System.out.println("this.TaskToSocketMap"+this.TaskToSocketMap);
	}

	/**
	 * This method is called when a Task leaves the server because it has
	 * finished service.
	 * 
	 * @param time
	 *            - the time the Task is removed
	 * @param task
	 *            - the Tasks that is removed
	 */
	public void removeTask(final double time, final Task task) {

		// Remove the Task from the socket it is running on
		//System.out.println(this.TaskToSocketMap);
		Socket socket = this.TaskToSocketMap.remove(task);
		//System.out.println(task.getServer().getNodeId());
		//System.out.println(task.getaJob().getJobId()+"taskid"+task.getTaskId());
		// Error check we could resolve which socket the Task was on
		//System.out.println(socket);
		if (socket == null) {
			Sim.fatalError("Task to Socket mapping failed");
		}

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

	/**
	 * Gets the instant utilization of the server. utilization = (Tasks
	 * running)/(total capacity)
	 * 
	 * @return the instant utilization of the server
	 */
	public double getInstantUtilization() {
		double avg = 0.0d;

		for (int i = 0; i < this.sockets.length; i++) {
			avg += this.sockets[i].getInstantUtilization();
		}
		avg /= this.sockets.length;

		return avg;
	}

	/**
	 * Get the experiment this server is part of.
	 * 
	 * @return the experiment this server is part of
	 */
	public Experiment getExperiment() {
		return this.experiment;
	}

	/**
	 * Get the sockets this server has.
	 * 
	 * @return the sockets the server has
	 */
	public Socket[] getSockets() {
		return this.sockets;
	}

	/**
	 * Sets the power management policy for the CPU cores in this server.
	 * 
	 * @param corePowerPolicy
	 *            - the power management policy to use
	 */
	public void setCorePolicy(final CorePowerPolicy corePowerPolicy) {
		for (int i = 0; i < sockets.length; i++) {
			this.sockets[i].setCorePolicy(corePowerPolicy);
		}
	}

	// fanyao modified for power model
	/**
	 * Gets the current power consumption of the server (in watts).
	 * 
	 * @return the current power consumption of the server (in watts)
	 */
	// public double getPower() {
	// double totalPower = this.getDynamicPower() + this.getIdlePower();
	//
	// return totalPower;
	// }

	// public double getPower() {
	// double totalPower = 0.0;
	// for (Socket aSocket : sockets) {
	// totalPower += aSocket.getPower();
	// }
	//
	// return totalPower + ServerPowerReader.getPlatformPower(globalState);
	// }

	/**
	 * Gets the current throughput of the server (in transmission rate).
	 * 
	 * @return the current throughput of the server (in transmission rate)
	 */
	public double getThroughput() {
		// FIXME: considering bidirectional port and directional port
		double throughput = 0.0;
		Set<Integer> keySet = portMap.keySet();
		for (int index : keySet) {
			Port port = portMap.get(index);
			/** PortState portstate = port.getPortState(); **/
			PortPowerState portState = port.getInPortState();
			int ps = 0;
			if (portState == PortPowerState.busy)
				ps = 1;
			throughput += ps * port.getPortRate();
		}
		return throughput;
	}

	// TODO get rid of magic numbers
	/**
	 * Gets the dynamic power consumption of the server.(in watts).
	 * 
	 * @return the dynamic power consumption of the server (in watts)
	 */
	// public double getDynamicPower() {
	// double dynamicPower = 0.0d;
	// for (int i = 0; i < this.sockets.length; i++) {
	// dynamicPower += this.sockets[i].getDynamicPower();
	// }
	// double util = this.getInstantUtilization();
	// double memoryPower = 10 * util;
	// double diskPower = 1.0 * util;
	// double otherPower = 5.0 * util;
	// dynamicPower += memoryPower + diskPower + otherPower;
	//
	// return dynamicPower;
	// }

	public void setCorePState(CorePState corePState) {
		for (int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setCorePState(corePState);
		}
	}

	// TODO get rid of magic numbers
	/**
	 * Gets the maximum dynamic power consumption of the server's CPUs (in
	 * watts).
	 * 
	 * @return the maximum dynamic power consumption of the server's CPUs (in
	 *         watts).
	 */
	private double getMaxCpuDynamicPower() {
		return 25.0;
	}

	/**
	 * Get the idle power consumption of the server (in watts).
	 * 
	 * @return the idle power consumption of the server (in watts)
	 */
	// public double getIdlePower() {
	// double idlePower = 0.0d;
	//
	// for (int i = 0; i < this.sockets.length; i++) {
	// idlePower += this.sockets[i].getIdlePower();
	// }
	//
	// // TODO: fanyao modified for sleepstates power profiles
	// // may need modification later
	// double memoryPower = 25;
	// double diskPower = 9;
	// double otherPower = 10;
	// idlePower += memoryPower + diskPower + otherPower;
	//
	// return idlePower;
	// }

	/**
	 * Resume processing at the server.
	 * 
	 * @param time
	 *            - the time processing is resumed
	 */
	public void resumeProcessing(final double time) {
		this.paused = false;

		for (int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].resumeProcessing(time);
		}

		while (this.getRemainingCapacity() > 0 && this.queue.size() != 0) {
			Task Task = this.queue.poll();
			this.startTaskService(time, Task);
		}
	}

	/**
	 * Pause processing at the server.
	 * 
	 * @param time
	 *            - the time the processing is paused
	 */
	public void pauseProcessing(final double time) {
		this.paused = true;

		for (int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].pauseProcessing(time);
		}
	}

	/**
	 * Set the server's sockets' power management policy.
	 * 
	 * @param socketPolicy
	 *            - the power management policy
	 */
	public void setSocketPolicy(final SocketPowerPolicy socketPolicy) {
		for (int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setPowerPolicy(socketPolicy);
		}

	}

	/**
	 * Sets the server's cores' active power (in watts).
	 * 
	 * @param coreActivePower
	 *            - the server's cores' active power (in watts)
	 */
	// public void setCoreActivePower(final double coreActivePower) {
	// for (int i = 0; i < this.sockets.length; i++) {
	// this.sockets[i].setCoreActivePower(coreActivePower);
	// }
	//
	// }

	/**
	 * Set the power of the server's cores when in park (in watts).
	 * 
	 * @param coreParkPower
	 *            - the power of the server's cores when in park (in watts)
	 */
	// public void setCoreParkPower(final double coreParkPower) {
	// for (int i = 0; i < this.sockets.length; i++) {
	// this.sockets[i].setCoreParkPower(coreParkPower);
	// }
	// }

	/**
	 * Sets the power of the server's cores while idle (in watts).
	 * 
	 * @param coreIdlePower
	 *            - the power of the server's cores while idle (in watts).
	 */
	// public void setCoreIdlePower(final double coreIdlePower) {
	// for (int i = 0; i < this.sockets.length; i++) {
	// this.sockets[i].setCoreIdlePower(coreIdlePower);
	// }
	// }

	/**
	 * Sets the active power of the server's sockets (in watts).
	 * 
	 * @param socketActivePower
	 *            - the active power of the server's sockets (in watts).
	 */
	// public void setSocketActivePower(final double socketActivePower) {
	// for (int i = 0; i < this.sockets.length; i++) {
	// this.sockets[i].setSocketActivePower(socketActivePower);
	// }
	// }

	/**
	 * Sets the park power of the server's sockets (in watts).
	 * 
	 * @param socketParkPower
	 *            - the park power of the socket in park (in watts)
	 */
	// public void setSocketParkPower(final double socketParkPower) {
	// for (int i = 0; i < this.sockets.length; i++) {
	// this.sockets[i].setSocketParkPower(socketParkPower);
	// }
	// }

	/**
	 * Set the DVFS speed of the server's cores.
	 * 
	 * @param time
	 *            - the time the speed is set
	 * @param speed
	 *            - the speed to set the cores to (relative to 1.0)
	 */
	public void setDvfsSpeed(final double time, final double speed) {
		for (int i = 0; i < this.sockets.length; i++) {
			this.sockets[i].setDvfsSpeed(time, speed);
		}
	}

//	public void setNextSleepState(final int socketIndex, final int sleepState) {
//		this.sockets[socketIndex].setNextSleepState(sleepState);
//	}

	/**
	 * Assign a power budget to a server (in watts). The server will change it's
	 * DVFS setting to try to meet this budget.
	 * 
	 * @param time
	 *            - the time the budget is assigned
	 * @param allocatedPower
	 *            - the power budget (in watts)
	 */
	public void assignPowerBudget(final double time, final double allocatedPower) {
		double dvfsSpeed = 0.0;
		double nonScalablePower = this.getMaxPower()
				- this.getMaxCpuDynamicPower();
		if (allocatedPower < nonScalablePower) {
			dvfsSpeed = 0.5;
		} else if (allocatedPower > this.getMaxPower()) {
			dvfsSpeed = 1.0;
		} else {
			double targetCpuPower = allocatedPower - nonScalablePower;
			dvfsSpeed = Math.pow(targetCpuPower / this.getMaxCpuDynamicPower(),
					1 / 3.0);
			dvfsSpeed = Math.max(dvfsSpeed, .5);
		}

		this.setDvfsSpeed(time, dvfsSpeed);
	}

	// TODO change this
	/**
	 * Get the maximum possible power consumption for the server (in watts).
	 * 
	 * @return the maximum possible power consumption for the server (in watts)
	 */
	public double getMaxPower() {
		return 100.0;
	}

	@Override
	public void scheduleTask(final double time, final Task task) {
		// TODO Auto-generated method stub
		insertTask(time, task);
	}

	@Override
	public NodeState queryPowerState() {
		// TODO Auto-generated method stub
		return null;
	}

	// fanyao added for RandomSleepState experiment
	public Core getFirstCore() {
		return this.sockets[0].getCores().get(0);
	}

	public Vector<Pair<Double, Integer>> getQueuedJobStats() {
		return queuedJobsStats;
	}

	public void AddToTaskSocketMap(Task task, Socket socket) {
		this.TaskToSocketMap.put(task, socket);
	}



	public void updateTaskStats(job.Task task) {
		// do nonting in Server Class
		
	}

}
