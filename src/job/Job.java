package job;

import event.*;
import infrastructure.*;
import communication.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import communication.CommunicationBundle;
import communication.Flow;
import communication.Packet;

import job.Task.TaskStatus;
import queue.BaseQueue;
import scheduler.GlobalQueueScheduler;
//import sun.security.util.PendingException;
import debug.Sim;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;

/**
 * @author fan
 * 
 */
// multiple jobs running
public class Job {
	public enum JobState {
		PENDING, PARTIAL_SCHEDULED, FULLY_SCHEDULED, JOBFINISHED
	}

	/**
	 * Collecton of tasks in the job
	 */
	private Vector<Task> allTasks;
	HashMap<Task, TaskStatus> allTaskStatus;

	/**
	 * simple dependence graph of tasks within the job
	 */
	// For more complicated relationship between tasks, we need to use
	// the job descriptor
	private Map<Task, Vector<Task>> dependenceGraph;
	
	/**
	 * stores the packets bundle between any pair of tasks
	 */
	// FIXME: use pair if only two elements are involved in vector
	private Map<Vector<Task>, CommunicationBundle> commBetweenTasks;

	public DataCenter dataCenter;

	private JobState mJobState;

	private long finishedTasks;

	private double arrivalTime;

	private double amountDelayed;

	private double startTime;

	private double finishTime;
	
//	private double flowStartTime;
//	
//	private double flowFinishTime;
	
	private double flowCompletionTime;

	private long jobId;

	private double jobSize;

	private double amountCompleted;

	private static long currentId = 1;

	private Experiment experiment;

	private JobFinishEvent jobFinishEvent;

	private double lastResumeTime;
	
	private int jobType;
	
	private int taskDelay;

	// private double amountDelayed;

	/**
	 * @author fanyao @11/22 added, get the message list between source task and
	 *         destination task
	 * @param src
	 * @param dst
	 * @return this function is called when one task finished and is going to
	 *         initialize message transmissions
	 */
	// public ArrayList<Message> getMessagesBetween2Task(Task src, Task dst) {
	// // this function
	// Vector<Task> taskPair = new Vector<Task>();
	// taskPair.add(src);
	// taskPair.add(dst);
	// // FIXME: just want use one message now for testing purpose
	// Message msg = new Message(scheduleTask(src), scheduleTask(dst), src,
	// dst, this.dataCenter);
	// ArrayList<Message> listOfMessage = new ArrayList<Message>();
	// listOfMessage.add(msg);
	// return listOfMessage;
	//
	// /*
	// * MessageBundle msgBundle = msgsBetweenTasks.get(taskPair); if
	// * (msgBundle == null) {
	// * System.err.println("Message bundle is not initialized properly\n");
	// * System.exit(0); }
	// *
	// * return msgBundle.getMessageList();
	// */
	// }

	// TODO need to add task scheduling scheme here
	public void initialize(double time) {

		// Schedule all tasks in a job
		Vector<Task> tasks = this.allTasks;
		
		if (tasks.size() == 0 || tasks == null)
			System.err.println("No tasks to schedule at the beginning!");
		
		for (Task aTask : tasks) {
			if(aTask.getTaskId() == 22) {
				System.out.println();
			}
			
			//FIXME: tasks shouldn't be removed from dependenceGraph until communications are built
			/*if (dependenceGraph != null) {
				dependenceGraph.remove(aTask);
			}*/
			double simulationTime = time;

			//Server assignedServer = dataCenter.scheduleTask(aTask, time);
			
			Server assignedServer = aTask.getServer();
			//System.out.println("assignedserver"+assignedServer);
			if(assignedServer==null){
				assignedServer = dataCenter.scheduleTask(aTask, time, false);
			}

			/*
			 * could not find an instant server to service the request simply put
			 * it to global queue and mark the arrival time for the task
			 */
			if (assignedServer == null) {
				aTask.markArrival(time);
				experiment.getGlobalQueue().add(aTask, time);

				
				// No server available for task
				int timerDepth = getGraphDepth() - 1;
				
				// Create TaskTimerEvent to prevent starvation
				if(timerDepth != 0) {
					// For now, average task size is uBar
					double sizeTasks = experiment.getExpConfig().getuBar();
					
					int jobQoS = experiment.getExpConfig().getJobQoS();
					
					double timerValue = timerDepth * sizeTasks * jobQoS;
					
					TaskTimerEvent taskTimerEvent = new TaskTimerEvent(experiment.getCurrentTime() + timerValue, experiment, aTask);
					aTask.setTaskTimerEvent(taskTimerEvent);
					experiment.addEvent(taskTimerEvent);
				}
				
				continue;
			}
			
//			System.out.println("Time :" + time + " Job "
//					+ this.getJobId() + " Task " + aTask.getTaskId()
//					+ " arrvial event in server : " + assignedServer.getNodeId()
//					+ " Task size :" + aTask.getSize());
			//instead of adding a TaskArrivalEvent, start processing task immediately
			assignedServer.scheduleTask(time, aTask);
			aTask.markArrival(time);
			
//			TaskArrivalEvent tEvent = new TaskArrivalEvent(time,
//					experiment, aTask, assignedServer);
//			experiment.addEvent(tEvent);
		}
	}
	
	public Map<Task, Task> howToScheduleTasks() {
		Map<Task, Task> tasks = new HashMap<Task, Task>();
		
		for (int i = 0; i < allTasks.size(); i++) {
			Task task = allTasks.get(i);
			// dependenceGraph itself could be null if there is only one task
			if (dependenceGraph == null) {
				System.out.println("Dependency graph is empty.");
			} else {
				Vector<Task> dependedTasks = dependenceGraph.get(task);
				if (dependedTasks.size() > 0) {
					tasks.put(task, dependedTasks.get(0));
				} else {
					continue;
				}
			}
		}
		System.out.println("nextschedule"+tasks);
		return tasks;
	}
	
	public ArrayList<Task> getParentTasksToSchedule() {
		ArrayList<Task> tasks = new ArrayList<Task>();
		
		// No task dependencies
		if(dependenceGraph == null) {
			tasks.add(this.allTasks.get(0));
		}
		
		// Return parent tasks
		else {
			for (Map.Entry<Task, Vector<Task>> entry: dependenceGraph.entrySet()) {
				Task parent = entry.getKey();
				
				// Mark as parent task
				parent.setParentTask(true);
				
				// Set parent's child tasks
				Vector<Task> childTasks = dependenceGraph.get(parent);
				parent.setChildTask(childTasks);
				
				// Set children's parent task
				for(int i = 0; i < childTasks.size(); i++) {
					childTasks.get(i).setParentTask(parent);
				}
				
				tasks.add(parent);
			}
		}
		
		return tasks;
	}

	public void setTaskDependency(Map<Task, Vector<Task>> td) {
		this.dependenceGraph = td;
		//System.out.println("this.dependenceGraph"+this.dependenceGraph);
		generateTasksPacketsList();
	}

	public Map<Task, Vector<Task>> getTaskDependency() {
		return this.dependenceGraph;
	}

	public void setAllTasks(Vector<Task> tasks) {
		this.allTasks = tasks;
	}

	public Vector<Task> getAllTasks() {
		return this.allTasks;
	}

	public void changeJobState(JobState newState) {
		this.mJobState = newState;
	}

	public void reportTaskStatus(Task _task, TaskStatus _taskStatus) {
		allTaskStatus.remove(_task);
		allTaskStatus.put(_task, _taskStatus);
		return;
	}

	public JobState queryState() {
		return this.mJobState;
	}

	/**
	 * 
	 * @author fanyao @11/22 added, get the packets list between source task and
	 *         destination task
	 * @param src
	 * @param dst
	 * @param time
	 * @return this function is called when one task finished and is going to
	 *         initialize packet transmissions
	 */
	public ArrayList<Packet> getPacketsBetween2Task(Task src, Task dst, double time) {
		Vector<Task> taskPair = new Vector<Task>();
		taskPair.add(src);
		taskPair.add(dst);
		
		Server srcServer = src.getServer();
		Server desServer = dst.getServer();
		if(desServer==null){
			desServer = dataCenter.scheduleTask(dst, time, true);
		}
		Vector<Vector<Integer>> path = dataCenter.mTopo.getSinglePathRoute(srcServer.getNodeId(), desServer.getNodeId());
		int numOfPackets = dataCenter.getExpConfig().getPacketNum();
		ArrayList<Packet> pcktsList = new ArrayList<Packet>();
		for (int i = 0; i < numOfPackets; i++) {
			Packet packet = new Packet(srcServer, desServer, src,
					dst, path, this.dataCenter, i + 1);
			pcktsList.add(packet);
		}
		System.out.println("task" + src.getTaskId() + "--->" + "task"
				+ dst.getTaskId() + " packets: " + numOfPackets);
		CommunicationBundle commBundle = new CommunicationBundle(pcktsList);
		commBetweenTasks.put(taskPair, commBundle);
		
		CommunicationBundle pcktsBundle = commBetweenTasks.get(taskPair);
		if (pcktsBundle == null)
			System.err.println("packets bundle is null");
		return pcktsBundle.getPacketsList();
	}
	
	public CommunicationBundle getCommBundle(Task src, Task dst, double time) {
		Vector<Task> taskPair = new Vector<Task>();
		taskPair.add(src);
		taskPair.add(dst);
		
		Server srcServer = src.getServer();
		Server desServer = dst.getServer();
		if(desServer==null){
			desServer = dataCenter.scheduleTask(dst, time, true);
		}
		dataCenter.mTopo.setmDataCenter(dataCenter);
		Vector<Vector<Integer>> path = dataCenter.mTopo.getSinglePathRoute(srcServer.getNodeId(), desServer.getNodeId());
		double flowSize = dataCenter.getExpConfig().getAvgFlowSize();
		CommunicationBundle flowBundle = new CommunicationBundle(flowSize, path);
		commBetweenTasks.put(taskPair, flowBundle);
		
		CommunicationBundle commBundle = commBetweenTasks.get(taskPair);
		if (commBundle == null)
			System.err.println("packets bundle is null");
		return commBundle;
	}

	public void setExperiment(Experiment exp) {
		this.experiment = exp;
	}

	/**
	 * 
	 * this function can generate the packets list between all pair of source
	 * and destination tasks needs to be called somewhere when initialized in
	 * simple experiment class
	 */
	protected void generateTasksPacketsList() {

		if (dependenceGraph == null || allTasks == null) {
			// System.out.println("no flows are generated");
			return;
		}
		
		if (commBetweenTasks == null)
			commBetweenTasks = new HashMap<Vector<Task>, CommunicationBundle>();

		if (commBetweenTasks == null)
			commBetweenTasks = new HashMap<Vector<Task>, CommunicationBundle>();
	}

	/**
	 * get a list of tasks that depends on _task
	 * 
	 * @param _task
	 */
	public Vector<Task> getChildTasks(Task _task) {
		if (dependenceGraph == null)
			return null;
		
		Iterator<Entry<Task, Vector<Task>>> it = dependenceGraph.entrySet()
				.iterator();
		
		while (it.hasNext()) {
			Map.Entry<Task, Vector<Task>> pairs = (Map.Entry<Task, Vector<Task>>) it
					.next();

			Task keyTask = (Task) pairs.getKey();
			
			if(_task.getTaskId() == keyTask.getTaskId()) {
				//Return dependent tasks
				return (Vector<Task>) pairs.getValue();
			}
		}

		//No dependent tasks
		return null;
	}
	
	/**
	 * get a list of tasks that _task depends on
	 * @param _task
	 * @return
	 */
	public Vector<Task> getParentTasks(Task _task) {
		if (dependenceGraph == null)
			return null;
		
		Iterator<Entry<Task, Vector<Task>>> it = dependenceGraph.entrySet()
				.iterator();
		Vector<Task> dependingTaskList = new Vector<Task>();
		while (it.hasNext()) {
			Map.Entry<Task, Vector<Task>> pairs = (Map.Entry<Task, Vector<Task>>) it
					.next();

			Task keyTask = (Task) pairs.getKey();
			Vector<Task> dependedTaskList = (Vector<Task>) pairs.getValue();

			if (dependedTaskList == null)
				System.err
						.println("error, dependedTaskList not initialized \n");
			for (Task iTask : dependedTaskList) {
				if (iTask == _task)
					dependingTaskList.add(keyTask);

			}
		}

		return dependingTaskList;
	}

	public ArrayList<Task> getNextSchedulableTasks(double time) {

		// TODO, first need to check the dependence graph
		// with the dependence graph, get the list of tasks
		// that could be scheduled.
		// Basically this means that there are no inter-dependence
		// those tasks

		// E.g, say the following task could be scheduled

		ArrayList<Task> tasks = new ArrayList<Task>();
		for (int i = 0; i < allTasks.size(); i++) {
			Task task = allTasks.get(i);
			// dependenceGraph itself could be null if there is only one task
			if (dependenceGraph == null) {
				tasks.add(task);
			} else {
				Vector<Task> dependedTasks = dependenceGraph.get(task);
				if (dependedTasks == null || dependedTasks.size() == 0) {
					//Tasks that are dependent on another task
					tasks.add(task);
				}
				else {
					//Task which others are dependent on. Should be first in list so it gets scheduled first
					tasks.add(0, task);
				}
			}
		}
		System.out.println("nextschedule"+tasks);
		return tasks;
	}

	public void removeDependency(double time, Packet packet) {
		Task dTask = packet.get_destinationTask();
		Task sTask = packet.get_sourceTask();

		Vector<Task> dependedList = dependenceGraph.get(dTask);
		if (dependedList == null) {
			System.err.println("error, dependedList is null\n");
			System.exit(0);
		}

		// dependedList.remove(sTask);
		// Vector<Task> index = new Vector<Task>();
		// index.add(sTask);
		// index.add(dTask);
		//
		// MessageBundle msgBundle = msgsBetweenTasks.get(index);
		// msgBundle.updateTransmittedCount();
		//
		// if (msgBundle.isAllMessageTransmitted()) {
		// dependedList.remove(sTask);
		// }

		Vector<Task> sdpair = new Vector<Task>();
		sdpair.add(sTask);
		sdpair.add(dTask);

		CommunicationBundle pcktsBundle = commBetweenTasks.get(sdpair);
		pcktsBundle.updateTransmittedCount();

		if (pcktsBundle.areAllPacketsTransmitted()) {
			dependedList.remove(sTask);
		}

		// fanyao added: check if the dependinglist is empty, then mark the task
		// finish
		// event here

		if (this.getChildTasks(sTask).size() == 0) {
			// generate task finish event
			// FIXME: check coherence with setDVFS
			TaskFinishEvent finishEvent = new TaskFinishEvent(time, experiment,
					sTask, sTask.getServer(), time, 0);
			this.experiment.addEvent(finishEvent);
		}

		if (dependedList.size() == 0) {
			// needs to check if successful
			dependenceGraph.remove(dTask);
			TaskArrivalEvent tEvent = new TaskArrivalEvent(time, experiment,
					dTask, packet.getDestinationNode());
			experiment.addEvent(tEvent);
		}

	}

	// this function works only if we are modeling the dependence graph
	// stage by stage

	public void taskFinished(Task task, double time) {
		// do not remove, keep them in the memory since we need the statistics
		finishedTasks++;
		

		if (allTasks.size() == finishedTasks) {

			RJobFinishEvent finishEvent = new RJobFinishEvent(time, experiment,
					this);
			this.experiment.addEvent(finishEvent);
			((LinecardNetworkExperiment)this.experiment).jobspending.remove(this);

		}

	}

	public Job(DataCenter dataCenter) {
		this.amountCompleted = 0.0;
		this.amountDelayed = 0.0;
		this.finishedTasks = 0;
		this.dataCenter = dataCenter;
		this.jobId = assignId();
		// this.atLimit = false;
		this.jobFinishEvent = null;
		this.lastResumeTime = 0.0;
		allTasks = new Vector<Task>();
		dependenceGraph = new HashMap<Task, Vector<Task>>();
		this.jobType = 0;
//		this.flowStartTime = 0.0;
//		this.flowFinishTime = 0.0;
		this.flowCompletionTime = 0.0;
		this.startTime = 0.0;
		this.finishTime = 0.0;
		this.taskDelay = 0;
	}
	
	public void setJobType(int jobType){
		this.jobType = jobType;
	}
	
	public int getJobType(){
		return jobType;
	}
	
	public int getTaskDelay(){
		return taskDelay;
	}
	
	public void setTaskDelay(){
		taskDelay++;
	}

	/**
	 * scheme to assign a task to a server. --- here just use very simple
	 * scheduling algorithm: server number = TaskID mod numOfServer
	 * 
	 * @param _task
	 * @return the server to which _task is assigned
	 */

	// public void setAtLimit(boolean atLimit) {
	// this.atLimit = atLimit;
	// }
	//
	// public boolean getAtLimit() {
	// return this.atLimit;
	// }

	public final double getAmountDelayed() {
		return this.amountDelayed;
	}

	public final void setAmountDelayed(final double amount) {
		this.amountDelayed = amount;
	}

	public final void setAmountCompleted(final double completed) {
		this.amountCompleted = completed;
	}

	public final double getAmountCompleted() {
		return this.amountCompleted;
	}

	private long assignId() {
		long toReturn = Job.currentId;
		Job.currentId++;
		return toReturn;
	}

	public final long getJobId() {
		return this.jobId;
	}

	public final void markArrival(final double time) {
		if (this.arrivalTime > 0) {
			Sim.fatalError("Job arrival marked twice!");
		}
		this.arrivalTime = time;
	}

	public final void markStart(final double time) {
		if (this.startTime > 0) {
			Sim.fatalError("Job start marked twice!");
		}
		this.startTime = time;
	}

	public final void markFinish(final double time) {
		if (this.finishTime > 0) {
			Sim.fatalError("Job " + this.getJobId() + " finsih marked twice!");
		}
		this.finishTime = time;
	}

	public final double getArrivalTime() {
		return this.arrivalTime;
	}

	public final double getStartTime() {
		startTime=allTasks.get(0).getStartTime();
		for (Task aTask : allTasks){
			if(aTask.getStartTime()<startTime){
				startTime=aTask.getStartTime();
			}
		}
		return this.startTime;
	}

	public final double getFinishTime() {
		finishTime=allTasks.get(0).getFinishTime();
		for (Task aTask : allTasks){
			if(aTask.getFinishTime()>finishTime){
				finishTime=aTask.getFinishTime();
			}
		}
		return this.finishTime;
	}
	
	public final void setFlowCompletionTime(double time) {
		flowCompletionTime = (time > flowCompletionTime) ? time: flowCompletionTime;
	}
	
	public final double getFlowCompletionTime() {
		return flowCompletionTime; 
	}

	public final double getSize() {
		return this.jobSize;
	}

	@Override
	public final boolean equals(final Object obj) {
		boolean objectEqual = super.equals(obj);
		if (!objectEqual) {
			return false;
		}

		// TODO (meisner@umich.edu) this may be exessive
		boolean idEqual = ((Job) obj).getJobId() == this.jobId;

		if (!idEqual) {
			return false;
		}
		return true;
	}

	@Override
	public final int hashCode() {
		return (int) this.jobId;
	}

	public final void setJobFinishEvent(final JobFinishEvent aJobFinishEvent) {
		this.jobFinishEvent = aJobFinishEvent;
	}

	public final JobFinishEvent getJobFinishEvent() {
		return this.jobFinishEvent;
	}

	public final void setLastResumeTime(final double time) {
		this.lastResumeTime = time;
	}

	public final double getLastResumeTime() {
		return this.lastResumeTime;
	}
	
	/**
	 * Update dependenceGraph
	 */
	public void removeDependencyByFlow(double time, Flow flow){
		Task sTask = flow.getSourceTask();
		Task dTask = flow.getDestinationTask();
		Vector<Task> dependedList = dependenceGraph.get(dTask);
		
		// Remove child task
		dependedList.remove(sTask);
		
		// If no more child tasks, remove parent task
		if(dependenceGraph.get(dTask).size() == 0) {

			double finishTime = time + dTask.getSize();
			//ComputationFinishEvent finishEvent = new ComputationFinishEvent(
//					finishTime, experiment, dTask, dTask.getServer(), time, 1.0);
			//this.experiment.addEvent(finishEvent);
			dependenceGraph.remove(dTask);


		}


	}
	
	/**
	 * No flow or packet forwarding involved
	 */
	public void removeDependencyOnSameServer(Task sTask, Task dTask) {
		Vector<Task> dependedList = dependenceGraph.get(dTask);
		
		// Remove child task
		dependedList.remove(sTask);
		
		// If no more child tasks, remove parent task
		if(dependenceGraph.get(dTask).size() == 0) {
			dependenceGraph.remove(dTask);
		}
	}
	
	/**
	 * Return depth of dependence graph
	 */
	public int getGraphDepth() {
		int maxDepth = 1;
		
		for(Task currentTask : allTasks) {
			int currentTaskDepth = getTaskDepth(currentTask);
			
			if(currentTaskDepth > maxDepth) {
				maxDepth = currentTaskDepth;
			}
		}
		
		return maxDepth;
	}
	
	/**
	 * Return depth of task
	 */
	public int getTaskDepth(Task task) {
		int depth = 1;
		
		Vector<Task> parents = new Vector<Task>();
		
		for(Map.Entry<Task, Vector<Task>> entry : dependenceGraph.entrySet()) {
			if(entry.getValue().contains(task)) {
				parents.add(entry.getKey());
				
			}
		}
		
		if(parents.size() == 1) {
			depth += getTaskDepth(parents.get(0));
		}
		else if(parents.size() > 1) {
			int maxParentDepth = 0;
			
			for(Task parentTask : parents) {
				int parentTaskDepth = getTaskDepth(parentTask);
				if(parentTaskDepth > maxParentDepth) {
					maxParentDepth = parentTaskDepth;
				}
			}
			
			depth += maxParentDepth;
		}
		
		return depth;
	}
}
