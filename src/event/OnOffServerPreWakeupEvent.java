package event;

import infrastructure.AbstractSleepController;
import infrastructure.DelayOffController;
import infrastructure.UniprocessorServer;
import job.Task;
import queue.BaseQueue;
import debug.Sim;
import experiment.Experiment;
import experiment.OnSleepExperiment;

public class OnOffServerPreWakeupEvent extends AbstractEvent {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected UniprocessorServer server;

	protected OnSleepExperiment onSleepExp;

	public OnOffServerPreWakeupEvent(double theTime, OnSleepExperiment anExperiment,
			UniprocessorServer server) {
		super(theTime, anExperiment);
		this.server = server;
		this.onSleepExp = anExperiment;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		verbose();

		server.updateWakupTime();
		//FIXME: hardcoded sleep state
		server.accumulateSSTimes(5, time - server.getLastEneterSleepStateTime());

		BaseQueue globalQueue = onSleepExp.getGlobalQueue();

		if (globalQueue == null) {
			Sim.fatalError("fatal error: global queue is not initialized");
		} else if (globalQueue.size() == 0) {
			// no more jobs in the queue, put the server to shallow
			// sleepstate immediately
			Sim.debug(3, "server : " + server.getNodeId() + " will idle wait after wakedup");
			server.setLastSleepStateTime(0.0);
			int id = server.getNodeId();
			
			//FIXME: temp fix for bypassing idle state bug
			AbstractSleepController abController = server.getSleepController();
//			if(abController instanceof DelayOffController){
//				DelayOffController doController = (DelayOffController)abController;
//				doController.setByPass(true);
//			}
			//go to idle and then deep sleep instead of directy go to sleep as in OnSleepServerWakeup
			abController.prepareForSleep(time);
		} else {
			int num = globalQueue.size();
			// now set the server to active
			server.setActive();

			// need to set lastEntersleepstate to 0.0
			// similar to CoreWakedupEvent
			server.setLastSleepStateTime(0.0);

			Task dequeuedTask = globalQueue.poll();
			Sim.debug(3,
					"Turned on server picked job: " + dequeuedTask.getTaskId()
							+ " server " + server.getNodeId());

			// experiment.getGlobalQueue().updateThreadholds(time);
			// don't forget to set the server for task
			dequeuedTask.setServer(server);
			// System.out.println("/// task  " + dequeuedTask.getTaskId() +
			// " get fetched by newly wakeup server: " +
			// server.getNodeId());
			server.startTaskService(time, dequeuedTask);
			server.TasksInServerInvariant++;

		}

	}

	@Override
	public void printEventInfo() {
		Sim.debug(3, "^^^ Time: " + time + " server : " + server.getNodeId()
				+ " is turned on");
	}
}
