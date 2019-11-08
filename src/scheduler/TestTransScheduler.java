package scheduler;

import job.Task;
import infrastructure.DataCenter;
import infrastructure.Server;
import experiment.SingletonJobExperiment;

public class TestTransScheduler extends LocalQueueScheduler {

	public TestTransScheduler(SingletonJobExperiment experiment) {
		super(experiment);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		Server server = null;
		long taskId = _task.getTaskId();
		
		DataCenter dc = mExp.getDataCenter();
		if(taskId == 1 || taskId == 4 )
			server = dc.getServers().get(0);
		else if(taskId == 2 || taskId == 3)
			server = dc.getServers().get(1);
		
	    _task.setServer(server);
	    return server;
	}

}
