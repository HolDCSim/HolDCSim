package scheduler;

import infrastructure.DelayDoze2Controller;
import infrastructure.OnOffController;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.util.Vector;

import job.Task;
import scheduler.MultiServerScheduler.SchedulerPolicy;

import debug.Sim;
import event.OnSleepServerWakeupEvent;
import event.StartSleepEvent;
import experiment.MultiServerExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

public class OnSleep2Scheduler extends OnSleepScheduler {

	public OnSleep2Scheduler(MultiServerExperiment multiServerExperiment) {
		super(multiServerExperiment);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected UniprocessorServer randomAvailServer() {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;
		Vector<UniprocessorServer> idleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lp1SleepServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lp2SleepServers = new Vector<UniprocessorServer>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()){
						highUtiServer = aServer;
					}
				}
				//int currentState = aServer.getCurrentSleepState();
				else if (serverSleepState == 1) {
					// idleServer = aServer;
					idleServers.add(aServer);

				} else if (serverSleepState== 4) {
					lp1SleepServers.add(aServer);
				}
				else if(serverSleepState == 5){
					lp2SleepServers.add(aServer);
				}
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler " + serverSleepState);
				}
			}
		}

		if(highUtiServer != null){
			return highUtiServer;
		}
		
		int numOfIdleServers = idleServers.size();
		int numOfLp1Servers = lp1SleepServers.size();
		int numOfLp2Servers = lp2SleepServers.size();

		if (numOfIdleServers == 0 && numOfLp1Servers == 0 && numOfLp2Servers == 0) {
			Sim.fatalError("fatal error: could not find available servers");
		}

		int index = 0;
		if (numOfIdleServers > 0) {
			// TODO: parameterize seed

			index = indexGen.nextInt(numOfIdleServers);
			return idleServers.get(index);
		}

		else if (numOfLp1Servers > 0){
			// TODO: parameterize seed
			// Random indexGen = new Random(10000);
			index = indexGen.nextInt(numOfLp1Servers);
			return lp1SleepServers.get(index);
		}
		
		else {
			// TODO: parameterize seed
			// Random indexGen = new Random(10000);
			index = indexGen.nextInt(numOfLp2Servers);
			return lp2SleepServers.get(index);
		}
	}
	
	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// override MultiServerScheduler
		// when off server is wakedup, do not assign the task to that server
		if (schPolicy == SchedulerPolicy.RANDOM) {

			// updateThresholds(time);

			if (allServersBusy()) {
				queue.add(_task, time);
				return null;
			}

			else {
				UniprocessorServer server;
				server = findAvailableServer();
				
				//adding a threshold for deep sleep server wakeup instead of doing it instantly
				 OnOffController ofController = (OnOffController)server.getSleepController();
				//FIXME: hardcoded sleep state
				if(ofController.getCurrentSleepState() == 5){
					if(queue.size() < pendingThreshold){
						queue.add(_task, time);
						return null;
					}
				}
				
				_task.setServer(server);

				if (server.getCurrentSleepState() == 1 || (server.getCurrentSleepState() == 0 && server.getRemainingCapacity() > 0)) {
					Sim.debug(3, "||| task " + _task.getTaskId()
							+ " picked server : " + server.getNodeId()
							+ " in sleep state " + server.getCurrentSleepState());
					// activeServers.remove(server);
					// Task oneTask = queue.poll();
					// updateThresholds(time);

					// if (queue.size() < Ws) {
					// if (activeServers.size() != 0) {
					// server = activeServers.remove(0);
					// lowPowerServers.add(server);
					// server.setFixedSleepState(deepState);
					// }
					// }
					return server;
				}

				else {
					// if the server is in sleep/off state, we decide to wake it up
					// before assign the task
					queue.add(_task, time);

					Sim.debug(
							3,
							"orz Time: " + time + " server : "
									+ server.getNodeId() + " would be waked up, currentstate: " + server.getCurrentSleepState());

					// update idle record
					DelayDoze2Controller abController = (DelayDoze2Controller)server.getSleepController();
					//Core singleCore = server.getCore();
					
					//FIXME: this portion of code should be implemented as a function
					Vector<IdleRecord> idleDistribution = abController
							.getIdleDistribution();
					if (idleDistribution.size() == 0) {
						IdleRecord firstRecord = new IdleRecord();
						firstRecord.startIdleTime = 0.0;
						firstRecord.sleepstate = abController
								.getCurrentSleepState();
						idleDistribution.add(firstRecord);
					}
					IdleRecord record = idleDistribution.lastElement();
					if (record.duration != IdleRecord.INITIAL) {
						Sim.fatalError("attempt to set idlerecord duration twice");
					}
					record.sleepstate = abController.getCurrentSleepState();
					record.duration = time - record.startIdleTime;

					/**********************************************************************/
					// cancel the next sleep state event since multiple sleep state are
					// used
					int serverState = abController.getCurrentSleepState();

					if (serverState != abController.getDeepestSleepState()) {
						StartSleepEvent event = abController.getNextSleepEvent();
						if (event != null)
							mExperiment.cancelEvent(event);
					}
					double transitionTime = server.getTansitionToActiveTime();
					//server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
					server.setTransitionToActive();
					
//					//fanyao added: need to flag the sleep state when unit is transitioning to active
//					/*FIXME: currently this cannot be called otherwise tarnsitiontoactive would be 0, and updateWakeup would fail*/
					//abController.setCurrentSleepState(0);
					
					OnSleepServerWakeupEvent event = new OnSleepServerWakeupEvent(time
							+ transitionTime, onOffExp, server);
					onOffExp.addEvent(event);

					return null;
				}
			}
		} else {
			Sim.fatalError("fatal error: currently only support random scheduling among available servers");
			return null;
		}
	}


}
