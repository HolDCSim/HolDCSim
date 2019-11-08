package scheduler;

import java.util.Vector;

import job.Task;
import scheduler.MultiServerScheduler.SchedulerPolicy;
import debug.Sim;
import infrastructure.DualDelayOffController;
import infrastructure.EnergyServer;
import infrastructure.OnOffController;
import infrastructure.Server;
import infrastructure.UniprocessorServer;
import event.OnSleepServerWakeupEvent;
import experiment.CombineExperiment;
import experiment.DualDelayDozeExperiment;
import experiment.MultiServerExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

public class DualDelayScheduler extends OnSleepScheduler {

	protected DualDelayDozeExperiment dualExp;
	protected int numOfHighTauServers; 
	
	private int pendingThreshold;
	
	public DualDelayScheduler(MultiServerExperiment multiServerExperiment) {
		super(multiServerExperiment);
		
		pendingThreshold = multiServerExperiment.getExpConfig().getPendingThreshold();
		numOfHighTauServers = multiServerExperiment.getExpConfig().getHighDelayServerNum();
		for(int i=0; i< multiServerExperiment.getExpConfig().getServersToSchedule(); i++){
			EnergyServer server = (EnergyServer)multiServerExperiment.getDataCenter().getServers().get(i);
			DualDelayOffController dualController = (DualDelayOffController)(server.getSleepController());
			
			if(i < numOfHighTauServers){
				dualController.setUseHighTau(true);
			}
			else{
				dualController.setUseHighTau(false);
			}
		}
	}
	
	@Override
	protected UniprocessorServer randomAvailServer() {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;
		
		Vector<UniprocessorServer> lowDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lowDelayDeepSleepServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayDeepSleepServers = new Vector<UniprocessorServer>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				DualDelayOffController ddController = (DualDelayOffController)aServer.getSleepController();
				
				//int currentState = aServer.getCurrentSleepState();
				//TODO: need to distinguish from hightao and lowtao server as well
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity())
					highUtiServer = aServer;
				}
				else if (serverSleepState == 1) {
					// idleServer = aServer;
					if(ddController.doUseHighTau())
						highDelayIdleServers.add(aServer);
					else
						lowDelayIdleServers.add(aServer);

				} else if (serverSleepState== 5) {
					if(ddController.doUseHighTau())
						highDelayDeepSleepServers.add(aServer);
					else
						lowDelayDeepSleepServers.add(aServer);
				}
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler " + serverSleepState);
				}
			}
		}
		if(highUtiServer != null){
			return highUtiServer;
		}

		UniprocessorServer pickedServer = null;
		int hdIdleNum = highDelayIdleServers.size() ;
		int ldIdleNum = lowDelayIdleServers.size() ;
		int hdSleepNum = highDelayDeepSleepServers.size() ;
		int ldSleepNum = lowDelayDeepSleepServers.size() ;
		int index;
        //pick server to be scheduled in the following order
		if (hdIdleNum > 0) {
			index = indexGen.nextInt(hdIdleNum);
			pickedServer = highDelayIdleServers.get(index);
		} else if (ldIdleNum > 0) {
			index = indexGen.nextInt(ldIdleNum);
			pickedServer = lowDelayIdleServers.get(index);
		} else if (hdSleepNum > 0) {
			index = indexGen.nextInt(hdSleepNum);
			pickedServer = highDelayDeepSleepServers.get(index);
		} else if (ldSleepNum > 0) {
			index = indexGen.nextInt(ldSleepNum);
			pickedServer = lowDelayDeepSleepServers.get(index);
		} else {
			Sim.fatalError("fatal error, not available servers to be found");
		}
		
		return pickedServer;
		

	}

	@Override
	protected UniprocessorServer firstAvailServer() {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;
		Vector<UniprocessorServer> lowDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lowDelayDeepSleepServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayDeepSleepServers = new Vector<UniprocessorServer>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				DualDelayOffController ddController = (DualDelayOffController)aServer.getSleepController();
				
				//TODO: need to distinguish from hightao and lowtao server as well
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity())
					highUtiServer = aServer;
				}
				//int currentState = aServer.getCurrentSleepState();
				else if (serverSleepState == 1) {
					// idleServer = aServer;
					if(ddController.doUseHighTau())
						highDelayIdleServers.add(aServer);
					else
						lowDelayIdleServers.add(aServer);

				} else if (serverSleepState== 5) {
					if(ddController.doUseHighTau())
						highDelayDeepSleepServers.add(aServer);
					else
						lowDelayDeepSleepServers.add(aServer);
				}
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler " + serverSleepState);
				}
			}
		}
		
		if(highUtiServer != null){
			return highUtiServer;
		}

		UniprocessorServer pickedServer = null;
		//int index;
        //pick server to be scheduled in the following order
		if (highDelayIdleServers.size() > 0) {
			//index = indexGen.nextInt(highDelayIdleServers.size());
			pickedServer = highDelayIdleServers.get(0);
		} else if (lowDelayIdleServers.size() > 0) {
			//index = indexGen.nextInt(lowDelayIdleServers.size());
			pickedServer = lowDelayIdleServers.get(0);
		} else if (highDelayDeepSleepServers.size() > 0) {
			//index = indexGen.nextInt(highDelayDeepSleepServers.size());
			pickedServer = highDelayDeepSleepServers.get(0);
		} else if (lowDelayDeepSleepServers.size() > 0) {
			//index = indexGen.nextInt(lowDelayDeepSleepServers.size());
			pickedServer = lowDelayDeepSleepServers.get(0);
		} else {
			Sim.fatalError("fatal error, not available servers to be found");
		}
		
		return pickedServer;
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
			
//			if (pendingJobs > 0 && pendingJobs < pendingThreshold) {
//				if()
//				queue.add(_task, time);
//				return null;
//			}

			else {
				UniprocessorServer server;
				server = findAvailableServer();
				
				//adding a threshold for deep sleep server wakeup instead of doing it instantly
				DualDelayOffController ddController = (DualDelayOffController)server.getSleepController();
				//FIXME: hardcoded sleep state
				if(!ddController.doUseHighTau() && ddController.getCurrentSleepState() == 5){
					if(queue.size() < pendingThreshold){
						if((onOffExp instanceof CombineExperiment)&&isDependingTask){
							//randomly choose one active server for the depending task
							Vector<UniprocessorServer> highDelayActServers = new Vector<UniprocessorServer>();
							for (UniprocessorServer aServer : allServers) {
								if (aServer.getCurrentSleepState()==0) {
									highDelayActServers.add(aServer);									
								}
							}
							int hdActNum = highDelayActServers.size();
							if(hdActNum>0){
								int index = indexGen.nextInt(hdActNum);
								server = highDelayActServers.get(index);
								Sim.debug(3, "||| task " + _task.getTaskId()
										+ " picked server : " + server.getNodeId()
										+ " in sleep state " + server.getCurrentSleepState());
								_task.setServer(server);
								return server;
							}else{
								Sim.fatalError("fatal error, no available servers to be found");
							}
						}
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
					// if the server is in off state, we decide to wake it up
					// before assign the task
					queue.add(_task, time);

					Sim.debug(
							3,
							"orz Time: " + time + " server : "
									+ server.getNodeId() + " would be waked up");

					// update idle record
					OnOffController abController = (OnOffController)server.getSleepController();
					//Core singleCore = server.getCore();
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
//					/*FIXME: need verification*/
//					abController.setCurrentSleepState(0);
					
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
