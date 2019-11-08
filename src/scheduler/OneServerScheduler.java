package scheduler;

import debug.Sim;
import job.Task;
import infrastructure.DataCenter;
import infrastructure.Server;
import experiment.Experiment;
import experiment.SingletonJobExperiment;

public class OneServerScheduler implements ITaskScheduler {

	private Experiment mExp;
	public OneServerScheduler(SingletonJobExperiment experiment) {
		//super(experiment);
		mExp = experiment;
		if (experiment.getExpConfig().getServersToSchedule() != 1)
			Sim.fatalError("OneServer scheduler could only schedule one server instead of : "
					+ experiment.getExpConfig().getServersToSchedule());
		// TODO Auto-generated constructor stub
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// schedule all the tasks to one server
		Server server;

		DataCenter dc = mExp.getDataCenter();
		server = dc.getServers().get(0);
		_task.setServer(server);
		return server;

	}
}