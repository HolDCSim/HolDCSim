package scheduler;

import job.Task;

import java.util.Vector;

import infrastructure.Server;


/**
 * task scheduler interface
 * @author fan
 */
public interface ITaskSchedulerHeuristic {
	public Vector<Server> scheduleTask(Vector<Task> _task, double time, boolean isDependingTask);
}
