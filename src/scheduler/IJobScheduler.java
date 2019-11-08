package scheduler;
import job.Job;

/**
 * job scheduler interface
 * @author fan
 */

public interface IJobScheduler {
	public void scheduleJob(Job job, double time);
}
