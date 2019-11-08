package event;

import infrastructure.OnOffController;

import java.util.*;

import infrastructure.*;
import debug.Sim;
import scheduler.GlobalQueueScheduler;
import scheduler.OnSleepScheduler;
import experiment.Experiment;
import experiment.OnSleepExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

public class OracleLoadEvent extends AbstractEvent {

	private OnSleepScheduler onSleepScheduler;
	private int serversNeeded;
	
	public OracleLoadEvent(double theTime, int serversNeeded, OnSleepScheduler scheduler,  Experiment anExperiment) {
		super(theTime, anExperiment);
		// TODO Auto-generated constructor stub
		onSleepScheduler = scheduler;
		this.serversNeeded = serversNeeded;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		int currentFastServers = onSleepScheduler.getNumOfFastServers();
		int serversToWakeup = serversNeeded - currentFastServers;
		
		Sim.debug(1, "need to pre-wakeup " + serversToWakeup + " servers" );
		if(serversToWakeup < 0 ){
			return;
		}
		else{
			Vector<Server> servers = experiment.getDataCenter().getServers();
			Vector<UniprocessorServer> sleepServers = new Vector<UniprocessorServer>();
			for(int num = 0; num < experiment.getExpConfig().getServersToSchedule(); num++){
				EnergyServer eServer = (EnergyServer)(servers.get(num));
				//FIXME: hardcoded sleep state
				if(eServer.getCurrentSleepState() == 5 && !eServer.isTransitionToActive()){
					sleepServers.add((UniprocessorServer)servers.get(num));
				}
			}
			
			if(sleepServers.size() == 0){
				Sim.warning("no enough servers to be wakeup by oracle");
				return;
			}
			
			for(int i=0; i < serversToWakeup; i++){
				// update idle record
			
				if(sleepServers.size() == 0)
					break;
				
				UniprocessorServer uniServer = sleepServers.remove(0);
				
				OnOffController abController = (OnOffController)uniServer.getSleepController();
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

				double transitionTime = uniServer.getTansitionToActiveTime();
				//server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
				uniServer.setTransitionToActive();
				
				OnOffServerPreWakeupEvent event = new OnOffServerPreWakeupEvent(time
						+ transitionTime, (OnSleepExperiment)experiment, uniServer);
				experiment.addEvent(event);
			}
			//wakeup enough servers here
		}
	}

}
