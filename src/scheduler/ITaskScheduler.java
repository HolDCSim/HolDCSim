package scheduler;

import job.Task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import infrastructure.Server;


/**
 * task scheduler interface
 * @author fan
 */
public interface ITaskScheduler {
	public Server scheduleTask(Task _task, double time, boolean isDependingTask);
	
	//public Vector<Server> scheduleTaskPair(Entry<Task, Task> taskPair, double time);
}
