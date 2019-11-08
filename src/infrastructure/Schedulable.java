package infrastructure;

import job.Task;

/**
 * @author Fan Yao
 * interface for nodes that could schedule task on
 * server abstraction 
 */
public interface  Schedulable {	
	
   public abstract void scheduleTask(final double time, final Task task);
}
