package job;

import infrastructure.*;
import communication.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

//import communication.Flow;
//import communication.Packet;
import job.Task.TaskStatus;
import debug.Sim;
import event.JobFinishEvent;
import event.RJobFinishEvent;
import event.TaskArrivalEvent;
import event.TaskFinishEvent;
import experiment.Experiment;

/**
 * @author fan
 * 
 */
// multiple jobs running
public class HeuristicJob {
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

	private long jobId;

	private double jobSize;

	private double amountCompleted;

	private static long currentId = 1;

	private Experiment experiment;

	private JobFinishEvent jobFinishEvent;

	private double lastResumeTime;
	
	private int jobType;

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

		// find ready tasks at time 0.0 (there may be several parallel tasks)
		//ArrayList<Task> tasks = this.getNextSchedulableTasks(0.0);
		Vector<Vector<Task>> tasks = this.getNextSchedulableTasks(0.0);
		System.out.println("tasks"+tasks);
		System.out.println("dependencegraph"+dependenceGraph);
		// needs to be removed.
		if (tasks == null)
			System.err.println("No tasks to schedule at the beginning!");
		for (Vector<Task> aTask : tasks) {
//			if (dependenceGraph != null) {
//				dependenceGraph.remove(aTask);
//			}
			//System.out.println("dependencegraph"+dependenceGraph);
			//Server assignedServer = aTask.get(0).getServer();
			Vector<Server> assignedServer = new Vector<Server>();
			//System.out.println("assignedserver"+assignedServer);
			if(aTask.get(0).getServer()==null){
				assignedServer = dataCenter.scheduleTaskHeuristic(aTask, time, false);
			}
			//System.out.println("assignedserver"+assignedServer);
			/*
			 * could not find an instant server to service the request simply put
			 * it to global queue and mark the arrival time for the task
			 */
			if (assignedServer == null) {
				for (Task bTask: aTask) {
					bTask.markArrival(time);
				}
				//return;
				continue;
			}
			
			//instead of adding a TaskArrivalEvent, start processing task immediately
			for (int i = 0; i < assignedServer.size(); i++) {
				assignedServer.get(i).scheduleTask(time, aTask.get(i));
				aTask.get(i).markArrival(time);
				System.out.println("Time: " + time + " Job "
						+ this.getJobId() + ", Task " + aTask.get(i).getTaskId()
						+ " arrvial event in server#" + assignedServer.get(i).getNodeId()
						+ ". Task size: " + aTask.get(i).getSize());
			}
			
//			TaskArrivalEvent tEvent = new TaskArrivalEvent(time,
//					experiment, aTask, assignedServer);
//			experiment.addEvent(tEvent);
		}
	}

	public void setTaskDependency(Map<Task, Vector<Task>> td) {
		this.dependenceGraph = td;
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

		/*for (Task sTask : allTasks) {

			Vector<Task> list = getDependingTaskList(sTask);
			if (list == null)
				return;

			Vector<Vector<Integer>> path = null;
			for (Task dTask : list) {
				// now create the list of packets needs to be transferred
				// from iTask ==> dTask

				// randomly generate a number of packets with random sizes
				// this simply generate number of packets randomly from 1~10
				// Random rd = new Random(9);
				*//*****************************************************************//*
				// int numOfPackets = rd.nextInt(Constants.MAX_NUM_OF_PACKETS) +
				// 1;
				*//*****************************************************************//*

				// temporary settings for testing purpose
				*//** int numOfPackets = 1; **//*
				
				//consider two communication modes: packet based or flow based
				int numOfPackets = 0;
				ArrayList<Packet> pcktsList = null;
				CommunicationBundle commBundle = null;
				
				// task scheduler for communication experiment won't need
				// time stamp
				Server srcServer = sTask.getServer();
				Server desServer = dTask.getServer();
				
				if(srcServer==null)	{
					srcServer = dataCenter.scheduleTask(sTask, 0.0);
				}
				if(desServer==null){
					desServer = dataCenter.scheduleTask(dTask, 0.0);
				}
				
				path = dataCenter.mTopo.getSinglePathRoute(srcServer.getNodeId(), desServer.getNodeId());
				
				if (dataCenter.getExpConfig().doPacketRouting()) {
					numOfPackets = dataCenter.getExpConfig().getPacketNum();

					// FIXME may need to change this after changing job/task
					// scheduling algorithm
					// this just works for static task scheduling
					pcktsList = new ArrayList<Packet>();
					
					for (int i = 0; i < numOfPackets; i++) {
						Packet packet = new Packet(srcServer, desServer, sTask,
								dTask, path, this.dataCenter, i + 1);
						pcktsList.add(packet);
					}
					
					System.out.println("task" + sTask.getTaskId() + "--->" + "task"
							+ dTask.getTaskId() + " packets: " + numOfPackets);
					
					commBundle = new CommunicationBundle(pcktsList);
				}
				else{
					double flowSize = dataCenter.getExpConfig().getAvgFlowSize();
					commBundle = new CommunicationBundle(flowSize, path);
				}
				*//**
				 * stores the bunch of packets to (sTask, dTask) ==> packets
				 * list by mapping
				 *//*
				Vector<Task> sdpair = new Vector<Task>();
				sdpair.add(sTask);
				sdpair.add(dTask);
				
				commBetweenTasks.put(sdpair, commBundle);
			}
		}*/
	}

	/**
	 * get a list of tasks that depends on _task
	 * 
	 * @param _task
	 * @return dependingTaskList
	 */
	public Vector<Task> getDependingTaskList(Task _task) {
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

	//public ArrayList<Task> getNextSchedulableTasks(double time) {
	public Vector<Vector<Task>> getNextSchedulableTasks(double time) {
		// schedule inter-dependent job pair
		Vector<Vector<Task>> tasks = new Vector<Vector<Task>>();
		for (Map.Entry<Task, Vector<Task>> entry : dependenceGraph
				.entrySet()) {
			Vector<Task> dTask = entry.getValue();
			Task sTask = entry.getKey();
			dTask.add(0, sTask);		//first one: source task, remaining: destination tasks
			tasks.add(dTask);
		}
		
		return tasks;
	}

	// System.out.println("No tasks could be scheduled!");
	
	public void removeDependencyByFlow(double time, Flow flow){
		Task sTask = flow.getSourceTask();
		Task dTask = flow.getDestinationTask();
		
		Vector<Task> dependedList = dependenceGraph.get(dTask);
		dependedList.remove(sTask);
		
		if (this.getDependingTaskList(sTask).size() == 0) {
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
					dTask, flow.getDestinationNode());
			experiment.addEvent(tEvent);
		}
		
		
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

		if (this.getDependingTaskList(sTask).size() == 0) {
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

		}

	}

	public HeuristicJob(DataCenter dataCenter) {
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
	}
	
	public void setJobType(int jobType){
		this.jobType = jobType;
	}
	
	public int getJobType(){
		return jobType;
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
		long toReturn = HeuristicJob.currentId;
		HeuristicJob.currentId++;
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
	

}
