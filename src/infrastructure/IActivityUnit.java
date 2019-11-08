package infrastructure;

import job.Task;


public interface IActivityUnit {


	public abstract Server getServer();

	public abstract void setActive();

	public abstract boolean isActive();

	public abstract boolean isTransitionToActive();

	public abstract void setTransitionToActive();
	
	public abstract int getFinishedTasks();
	
	public abstract void updateTaskStats(Task task);
	
	public abstract void initialLowPowerState();
	
	public abstract boolean isSocket();  
	
	//public abstract void fetchTaskToExecute(Task aTask, double time);
	
	/**
	 * when unit is wakeup from sleep, activity unit needs to take control
	 * sleep controller would also be updated within it
	 * 
	 * @param time
	 *           
	 */
	public abstract void wakedupFromSleep(final double time);
	
	/**
	 * when unit starts sleep, activity unit needs to be called to set its power state
	 * this could be eliminated if Socket and Core use same power state enum
	 * sleep controller would also be updated within it
	 * @param time
	 * @param sleepState
	 */
	public abstract void startSleep(final double time, int sleepState);
	
}
