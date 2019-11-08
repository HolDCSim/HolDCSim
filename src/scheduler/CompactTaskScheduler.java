package scheduler;
import java.util.Map;
import java.util.Vector;

import job.Job;
import job.Task;
import infrastructure.DataCenter;
import infrastructure.Server;
import experiment.Experiment;

public class CompactTaskScheduler implements ITaskScheduler{
	private Experiment mExp;
	public CompactTaskScheduler(Experiment experiment){
		this.mExp = experiment;
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		Server server;
		int serverid=0;
		DataCenter dc = mExp.getDataCenter();
		Job job = _task.getaJob();
		Map<Task, Vector<Task>> TaskDependency = job.getTaskDependency();
		Vector<Task> dependedList = TaskDependency.get(_task);
		if(dependedList!=null){
			for (Task iTask : dependedList) {
				serverid=iTask.getServer().getNodeId();
				break;
			}
		}else{
			//serverid=((int) _task.getTaskId()) % dc.getServers().size();
			serverid=((int) _task.getTaskId()) % dc.getExpConfig().getServersToSchedule();
			if (serverid == 0)
				//serverid = dc.getServers().size();
				serverid = dc.getExpConfig().getServersToSchedule();
		}
		server = dc.getServers().get(serverid-1);		
		_task.setServer(server);
		return server;
	}
}
