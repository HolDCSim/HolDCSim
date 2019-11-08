package scheduler;

import infrastructure.AbstractSleepController;
import infrastructure.OnOffController;
import infrastructure.OnOffServer;
import infrastructure.Server;
import infrastructure.GeneralSleepController;
import infrastructure.UniprocessorServer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import job.Task;

import debug.Sim;

import event.OnOffServerWakeupEvent;
import event.OnOffSleepEvent;
import experiment.MultiServerExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * OnOffScheduler implementation based on CMU paper. When server is waking up
 * from off state, it would be interrupted and reset to off state immediately
 * 
 * @author fanyao
 */
public class OnOffScheduler extends OnSleepScheduler {

	protected Map<OnOffServer, OnOffServerWakeupEvent> serverTransitionMap;

	public OnOffScheduler(MultiServerExperiment multiServerExperiment) {
		super(multiServerExperiment);
		serverTransitionMap = new LinkedHashMap<OnOffServer, OnOffServerWakeupEvent>();

		// TODO Auto-generated constructor stub
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// override MultiServerScheduler
		// when off server is wakedup, do not assign the task to that server
		if (schPolicy == SchedulerPolicy.RANDOM) {

			// updateThresholds(time);

			if (allServersBusy()) {
				queue.add(_task, time);
				//System.out.println("number of jobs in queue: " + queue.size());
				return null;
			}

			else {
				OnOffServer server;
				server = (OnOffServer) findAvailableServer();
				_task.setServer(server);

				if (server.getCurrentSleepState() == 1) {
					Sim.debug(3,
							  "||| task " + _task.getTaskId()
							  + " picked server : " + server.getNodeId()
							  + " in sleep state "
							  + server.getCurrentSleepState());
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
					// if the server is in off state, we decide to wake it up
					// before assign the task
					queue.add(_task, time);

					Sim.debug(3, "orz Time: " + time + " off server : "
							+ server.getNodeId() + " would be waked up");

					// update idle record
					AbstractSleepController abController = server.getSleepController();
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

					double transitionTime = server.getTansitionToActiveTime();
					//server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
					server.setTransitionToActive();
					OnOffServerWakeupEvent event = new OnOffServerWakeupEvent(
							time + transitionTime, onOffExp, server);
					onOffExp.addEvent(event);

					serverTransitionMap.put(server, event);

					return null;
				}
			}
		} else {
			Sim.fatalError("fatal error: currently only support random scheduling among available servers");
			return null;
		}
	}

	// @Override
	// protected SingleCoreServer findFirstAvailableServer() {
	// // always first pick the server in idle state
	// // TODO Auto-generated method stub
	// SingleCoreServer idleServer = null;
	// SingleCoreServer offServer = null;
	// // FIXME: shuffle method may be time-consuming
	// // Collections.shuffle(allServers);
	// for (SingleCoreServer aServer : allServers) {
	// if (!aServer.isServerBusy()) {
	// if (aServer.getSleepState() == 1 && idleServer == null) {
	// idleServer = aServer;
	// break;
	// } else if (aServer.getSleepState() == 6 && offServer == null) {
	// offServer = aServer;
	// }
	// }
	// }
	//
	// if (idleServer == null && offServer == null) {
	// Sim.fatalError("fatal error: could not find available servers");
	// }
	//
	// if (idleServer != null) {
	// return idleServer;
	// } else {
	//
	// return offServer;
	// }
	//
	// }

	public OnOffServer shutoffTranServer(OnOffServer aServer, double time) {
		// TODO Auto-generated method stub
		// if (aServer == null) {
		// Sim.fatalError("fatal error: try to shutdown a null server");
		// }

		// remove the server from serverTransitionMap
		if (serverTransitionMap.size() == 0) {
			Sim.fatalError("no more transevents in serverTransmap");
		}

		Entry<OnOffServer, OnOffServerWakeupEvent> firstEntry = serverTransitionMap
				.entrySet().iterator().next();
		OnOffServer offServer = firstEntry.getKey();
		OnOffServerWakeupEvent transEvent = firstEntry.getValue();
		if (offServer == null) {
			Sim.fatalError("could not remove null transition server ");
		}

		serverTransitionMap.remove(offServer);

		////////////////////////////////////////////////////////////////////////);
		OnOffController abController = (OnOffController)offServer.getSleepController();

		// wrap up the previous stats
		// Core core = aServer.getCore();
		// TODO: there is need to record last begin transition_to_active time
		// for each core
		IdleRecord record = abController.getIdleDistribution().lastElement();
		offServer.updateWakupTime(time - abController.getLastEnterSleepStateTime()
				- record.duration);
		offServer.accumulateSSTimes(6,
				time - offServer.getLastEneterSleepStateTime());

		Sim.debug(3, "\\>\\> server " + offServer.getNodeId()
				+ " is turned back off");

		offServer.setLastSleepStateTime(0.0);
		abController.generaterSetupTime();

				

		// cancel the original
		onOffExp.cancelEvent(transEvent);
		// add core sleep event
		OnOffSleepEvent corestartSleepEvent = new OnOffSleepEvent(time,
				onOffExp, offServer, 6);
		onOffExp.addEvent(corestartSleepEvent);

		return offServer;
	}
	
	@Override
	protected UniprocessorServer randomAvailServer() {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;
		Vector<UniprocessorServer> idleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> deepSleepServers = new Vector<UniprocessorServer>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int currentState = aServer.getCurrentSleepState();
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null ||  highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity())
					highUtiServer = aServer;
				}
				else if (currentState == 1) {
					// idleServer = aServer;
					idleServers.add(aServer);

				} else if (currentState == 6) {
					deepSleepServers.add(aServer);
				}
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler");
				}
			}
		}

		if(highUtiServer != null){
			return highUtiServer;
		}
		
		int numOfIdleServers = idleServers.size();
		int numOfOffServers = deepSleepServers.size();

		if (numOfIdleServers == 0 && numOfOffServers == 0) {
			Sim.fatalError("fatal error: could not find available servers");
		}

		int index = 0;
		if (numOfIdleServers > 0) {
			// TODO: parameterize seed

			index = indexGen.nextInt(numOfIdleServers);
			return idleServers.get(index);
		}

		else {
			// TODO: parameterize seed
			// Random indexGen = new Random(10000);
			index = indexGen.nextInt(numOfOffServers);
			return deepSleepServers.get(index);
		}
	}
	
	@Override
	protected UniprocessorServer firstAvailServer() {
		// always first pick the server in idle state
		// TODO Auto-generated method stub
		UniprocessorServer highUtiServer = null;
		UniprocessorServer idleServer = null;
		UniprocessorServer deepSleepServer = null;
		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				if (!aServer.isServerAllIdle()){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()){
						highUtiServer = aServer;
					}
				}
				else if (aServer.getCurrentSleepState() == 1 && idleServer == null) {
					idleServer = aServer;
				} else if (aServer.getCurrentSleepState() == 6 && deepSleepServer == null) {
					deepSleepServer = aServer;
				}
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnOffScheduler");
				}
			}
		}

		if (idleServer == null && deepSleepServer == null && highUtiServer == null) {
			Sim.fatalError("fatal error: could not find available servers");
		}

		if(highUtiServer != null){
			return highUtiServer;
		}
		else if (idleServer != null)
			return idleServer;
		else
			return deepSleepServer;
	}

	public Task getUnassignedJobInQueue() {
		if (queue.size() >= serverTransitionMap.size()) {
			return queue.get(serverTransitionMap.size() - 1);
		} else
			return null;
	}

	public void removeTranServer(OnOffServer server) {
		// TODO Auto-generated method stub
		serverTransitionMap.remove(server);
	}

}
