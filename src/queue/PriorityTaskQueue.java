package queue;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Vector;

import debug.Sim;

import scheduler.ShallowDeepScheduler;
import utility.Pair;

import job.Task;

public class PriorityTaskQueue extends MSGlobalQueue {

	private PriorityQueue<Task> priorityTaskQueue;
	public PriorityTaskQueue(boolean collectHis, ShallowDeepScheduler aTaskScheduler) {
		super(collectHis, aTaskScheduler);
		
		Comparator<Task> comparator = new Comparator<Task>(){

			@Override
			public int compare(Task task1, Task task2) {
				// TODO Auto-generated method stub
				Double firstStartTime = task1.getEarliestStartTime();
				Double secondStartTime = task2.getEarliestStartTime();
				return firstStartTime.compareTo(secondStartTime);
			}
			
		};
		
		
		priorityTaskQueue = new PriorityQueue<Task>(1000, comparator);
		
		//initialize the queue after priorityqueue is created
	    this.updateQueueHis(0.0);
	}
	
	public double getTotalPendingTime(){
		return totalPendingTime;
	}

	

	public Vector<Pair<Double, Integer>> getGlobalQueueHis() {
		return this.globalQueueHis;
	}

	public Task poll(double time) {
		Task task = priorityTaskQueue.poll();
		
		if(task.getJobType() == 0)
			totalPendingTime -= GOOGLE_SIZE;
		else if(task.getJobType() == 1)
			totalPendingTime -= DNS_SIZE;
		
		//updateThreadholds(time);
		if (doCollectHis) {
			this.updateQueueHis(time);
		}
		
		updateThreadholds(time);

		return task;
	}

	public void add(Task task, double time) {
		if(task.getJobType() == 0)
			totalPendingTime += GOOGLE_SIZE;
		else if(task.getJobType() == 1)
			totalPendingTime += DNS_SIZE;
		
		priorityTaskQueue.add(task);
		Sim.debug(5, "task " + task.getTaskId() + " earliest start time: " + task.getEarliestStartTime());
		// updateThreadholds(time);
		if (doCollectHis) {
			this.updateQueueHis(time);
		}
		
		updateThreadholds(time);

	}

	@Override
	public void updateQueueHis(double time) {
		/* queue has not been created, but update queue his is called
		 * in parent class
		 */
		if(priorityTaskQueue == null)
			return;
		
		// collect queuing information
		Pair<Double, Integer> aPair = new Pair<Double, Integer>();
		aPair.setFirst(time);
		aPair.setSecond(priorityTaskQueue.size());
		globalQueueHis.add(aPair);
	}
	
	public int size(){
		return priorityTaskQueue.size();
	}
	
	
	public Task poll(){
		Task task = priorityTaskQueue.poll();
		
		if(task.getJobType() == 0)
			totalPendingTime -= GOOGLE_SIZE;
		else if(task.getJobType() == 1)
			totalPendingTime -= DNS_SIZE;
		
		return task;
	}
	
	public void updateThreadholds(double time){
		taskScheduler.updateThresholds(time);
	}

	
}
