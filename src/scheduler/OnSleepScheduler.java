package scheduler;

import java.util.Vector;

import job.Task;
import infrastructure.Core;
import infrastructure.DualDelayOffController;
import infrastructure.OnOffController;
import infrastructure.Server;
import infrastructure.UniprocessorServer;
import debug.Sim;
import event.OnSleepServerWakeupEvent;
import experiment.MultiServerExperiment;
import experiment.OnSleepExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * @author fanyao: one implementation of OnOffScheduler, once server is wakeup
 *         from off state, it will transit to active without interruption
 * 
 */
public class OnSleepScheduler extends MultiServerScheduler {

	protected OnSleepExperiment onOffExp;
	protected int pendingThreshold;

	public OnSleepScheduler(MultiServerExperiment multiServerExperiment) {
		super(multiServerExperiment);
		onOffExp = (OnSleepExperiment) multiServerExperiment;
		pendingThreshold = 0;

		// TODO Auto-generated constructor stub
	}
	
	/**get the number of fast servers, including busy servers and idle state servers
	 */
    public int getNumOfFastServers(){
    	int num=0;
    	
    	for(UniprocessorServer server : allServers){
    		//FIXME: hardcoded sleep state
    		//if( server.getCurrentSleepState() != 5){
    		if( server.getCurrentSleepState() == 1 || server.getCurrentSleepState() == 0){
    			num++;
    		}
//    		else if(server.isTransitionToActive()){
//    			num++;
//    		}
//    			
    	}
    	
    	return num;
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

				} else if (serverSleepState== 5) {
					deepSleepServers.add(aServer);
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

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;
		Vector<UniprocessorServer> idleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> deepSleepServers = new Vector<UniprocessorServer>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity())
					highUtiServer = aServer;
				}
				//int currentState = aServer.getCurrentSleepState();
				else if (serverSleepState == 1) {
					// idleServer = aServer;
					idleServers.add(aServer);

				} else if (serverSleepState== 5) {
					deepSleepServers.add(aServer);
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
		int numOfOffServers = deepSleepServers.size();

		if (numOfIdleServers == 0 && numOfOffServers == 0) {
			Sim.fatalError("fatal error: could not find available servers");
		}

		//int index = 0;
		if (numOfIdleServers > 0) {
			// TODO: parameterize seed

			//index = indexGen.nextInt(numOfIdleServers);
			return idleServers.get(0);
		}

		else {
			// TODO: parameterize seed
			// Random indexGen = new Random(10000);
			//index = indexGen.nextInt(numOfOffServers);
			return deepSleepServers.get(0);
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
									+ server.getNodeId() + " would be waked up");

					// update idle record
					OnOffController abController = (OnOffController)server.getSleepController();
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
