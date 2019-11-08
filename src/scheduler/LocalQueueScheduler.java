package scheduler;

import experiment.Experiment;

/**
 * scheduler without global queue setting
 * @author fanyao
 *
 */
public abstract class LocalQueueScheduler implements ITaskScheduler {

	protected Experiment mExp;
	
	public LocalQueueScheduler(Experiment experiment){
		this.mExp = experiment;
	}

}
