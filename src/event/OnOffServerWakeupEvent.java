package event;

import job.Task;
import queue.BaseQueue;
import debug.Sim;
import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.DelayOffController;
import infrastructure.OnOffServer;
import experiment.MultiServerExperiment;
import experiment.OnOffExperiment;
import experiment.OnSleepExperiment;

/**
 * corresponding to OnOffScheduler wakeup event for server in off state in
 * OnOffExperiment
 * 
 * @author fanyao
 */
public class OnOffServerWakeupEvent extends OnSleepServerWakeupEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OnOffServerWakeupEvent(double theTime,
			OnSleepExperiment anExperiment, OnOffServer server) {
		super(theTime, anExperiment, server);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

		server.updateWakupTime();
		//FIXME: hardcoded sleep state
		server.accumulateSSTimes(6, time - server.getLastEneterSleepStateTime());

		BaseQueue globalQueue = onSleepExp.getGlobalQueue();

		if (globalQueue == null) {
			Sim.fatalError("fatal error: global queue is not initialized");
		} else if (globalQueue.size() == 0) { // this else should never happen
			// no more jobs in the queue, put the server to deep
			// sleepstate immediately
			server.setLastSleepStateTime(0.0);
			
			//FIXME: temp fix for bypassing idle state bug
			AbstractSleepController abController = server.getSleepController();
			if(abController instanceof DelayOffController){
				DelayOffController doController = (DelayOffController)abController;
				doController.setByPass(true);
			}
			
			//use the subclassed sleep event in the associated core
			StartSleepEvent startSleepEvent = server.getSleepController()
					.generateSleepEvent(time, onSleepExp, 6);// new
														// CoreStartSleepEvent(time,
														// onOffExp,
														// server.getCore(), );
			onSleepExp.addEvent(startSleepEvent);
		} else {

			// now set the server to active
			server.setActive();

			/************************************************************************/
			// fanyao added for accululating number of active server history
			if (experiment instanceof MultiServerExperiment) {
				MultiServerExperiment msExp = (MultiServerExperiment) experiment;
				msExp.updateServerHis(time, 1);
			}

			/************************************************************************/

			// need to set lastEntersleepstate to 0.0
			// similar to CoreWakedupEvent
			server.setLastSleepStateTime(0.0);

			Task dequeuedTask = globalQueue.poll();
			Sim.debug(3,
					"Turned on server picked job: " + dequeuedTask.getTaskId()
							+ " server " + server.getNodeId());

			OnOffServer prevServer = (OnOffServer) dequeuedTask.getServer();
			if (prevServer != null) {
				// if the turned on server pick one task which is preassigned to
				// other server
				OnOffExperiment onOff2Exp = (OnOffExperiment) onSleepExp;
				onOff2Exp.removeTranServer((OnOffServer) server);

			} else {
				Sim.fatalError("preserver is null");
			}
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

}
