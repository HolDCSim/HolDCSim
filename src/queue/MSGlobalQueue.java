package queue;

import job.Task;
import scheduler.ShallowDeepScheduler;

/**
 * multi-server environment, a global queue which update threshold for server
 * power state management
 * 
 * @author fan
 * 
 */
public class MSGlobalQueue extends BaseQueue {

	protected ShallowDeepScheduler taskScheduler;

	public Task poll(double time) {
		Task task = super.poll(time);
		updateThreadholds(time);
		return task;
	}

	public void add(Task task, double time) {
		super.add(task, time);
		updateThreadholds(time);
	}

	public void updateThreadholds(double time) {
		taskScheduler.updateThresholds(time);
	}

	public MSGlobalQueue(boolean collectHis, ShallowDeepScheduler aTaskScheduler) {
		super(collectHis);
		this.taskScheduler = aTaskScheduler;
	}

}
