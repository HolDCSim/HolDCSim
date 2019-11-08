package scheduler;

import queue.BaseQueue;

import experiment.Experiment;

/**
 * abstract class for task schedulers with a global task queue
 * @author fan 
 */
public abstract class GlobalQueueScheduler implements ITaskScheduler {
	// this is the global queue
	public BaseQueue queue = null;
	protected Experiment mExperiment;

	public BaseQueue getGlobalQueue() {
		return queue;
	}

	public GlobalQueueScheduler(Experiment experiment) {
		this.mExperiment = experiment;
	}

	public Experiment getExperiment() {
		return mExperiment;
	}
	
	/**
	 * factory method for creating queues 
	 * @return
	 */
	protected abstract BaseQueue createQueue();
}
