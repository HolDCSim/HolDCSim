package scheduler;

import job.Task;
import infrastructure.DataCenter;
import infrastructure.Server;
import experiment.Experiment;

public class TryScheduler implements ITaskScheduler {

	private Experiment mExp;
	public TryScheduler(Experiment experiment) {
		//super(null);
		this.mExp = experiment;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		DataCenter dc = mExp.getDataCenter();
		int taskId = (int) _task.getTaskId();
		//int serverId = taskId % dc.getServers().size();
		int serverId = taskId % dc.getExpConfig().getServersToSchedule() + 1;
		//System.out.println("taskid"+taskId);
		//System.out.println("serverid"+serverId);
		Server server;
		if (serverId == 0)
			//serverId = dc.getServers().size();
			serverId = dc.getExpConfig().getServersToSchedule();
		// Iterator iter = this.dataCenter.entrySet().iterator();
		// while(iter.hasNext()){
		// Map.Entry pair = (Map.Entry)iter.next();
		// System.out.println((Integer)pair.getKey() + "\n");
		// if((Integer)pair.getKey() == 2)
		// server =(Server) pair.getValue();
		// }
		// long to int bug
		server = dc.getServers().get(serverId - 1);
		_task.setServer(server);
		return server;
	
	}
	


}
