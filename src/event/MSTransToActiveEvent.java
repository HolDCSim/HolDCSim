package event;

import queue.BaseQueue;
//import queue.MSGlobalQueue;

import java.util.LinkedList;

import debug.Sim;

import job.Task;
import infrastructure.ERoverServer;
import infrastructure.ShallowDeepServer;
import experiment.ShallowDeepExperiment;
import experiment.MultiServerExperiment;

/**
 * @author fanyao event for transit one server from sleep to active mode in
 *         MultiServerExperiment when this event arrived, the core should be set
 *         to active state
 */
public class MSTransToActiveEvent extends AbstractEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ShallowDeepServer server;
	private ShallowDeepExperiment mulExp;
	private int stateBeforeWakeup;

	public MSTransToActiveEvent(double theTime,
			ShallowDeepExperiment anExperiment, ShallowDeepServer server,
			int stateBeforeWakeup) {
		super(theTime, anExperiment);
		this.server = server;
		this.mulExp = anExperiment;
		this.stateBeforeWakeup = stateBeforeWakeup;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void printEventInfo() {
		Sim.debug(3, "PPP Time: " + time + " server : " + server.getNodeId()
				+ " is waked up!");
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

		// needs to update the wakeup latency here for the core
		verbose();
		server.setToActiveEvent(null);
		server.updateWakupTime();

		// FIXME: accululateSSTimes() should be later altered to be
		// self-contained in Core
		server.accumulateSSTimes(stateBeforeWakeup,
				time - server.getLastEneterSleepStateTime());

		// this means the server now set to deep sleepstate before it was awaked
		if (server.getToSleepEvent()) {
			
			if(server.isScheduledActive()){
				Sim.fatalError("fatal error, server has SleepEvent set but is currently scheduled as active");
			}
			server.setToSleepEvent(false);
			server.unsetScheduledActive();
			Sim.debug(5,"^^^^ -- MStranstoActiv going to sleep-- server : " + server.getNodeId()  + " going to be woken up "+ experiment.printStateofServers());
			server.setLastSleepStateTime(0.0);

			StartSleepEvent startSleepEvent = server
					.generateSleepEvent(time, mulExp,
							server.getInitialSleepState());
			/*
			 * server should be put back to sleep again. Need to figure out what
			 * is the next sleep state
			 */
			// CoreStartSleepEvent corestartSleepEvent = new
			// CoreStartSleepEvent(
			// time, mulExp, server.getCore(),
			// server.getInitialSleepState());
			mulExp.addEvent(startSleepEvent);

			// server.setPowerState(Core.PowerState.LOW_POWER_IDLE);
			// server.setCurrentSleepState(deepSleepState);
		}

		else {
			// server.updateWakupTime();

			// need to accumulate sleepstate stats, treat this event as
			// CoreWakeupEvent
			Sim.debug(5,"^^^^ -- MStranstoActiv -- server : " + server.getNodeId()  + " going to be woken up "+ experiment.printStateofServers());
			
			LinkedList<Task> queue;
			if (server instanceof ERoverServer) {
				queue = ((ERoverServer) server).getWaitingTasksList();
			} else {
			queue = mulExp.getGlobalQueue().internalQueue;
			}
			
			
			if (queue == null) {
				Sim.fatalError("fatal error: global queue is not initialized");
			} 
			else if (queue.size() == 0) {
				// no more jobs in the queue, put the server to shallow
				// sleepstate immediately
				server.setLastSleepStateTime(0.0);

				// server.setScheduledActive();
				/*
				 * bug fix: add this to prevent scheduler pick deep sleep state
				 * server from activerserver arrays
				 */
				
				Sim.debug(5,"^^^^ -- MStranstoActiv Found no jobs when woken up-- server : " + server.getNodeId()  + " going to be woken up "+ experiment.printStateofServers());
			//	System.out.println("Wokeup server ");
				int initialState = server.getInitialSleepState();
				server.setNextFixedSleepState(initialState);
				StartSleepEvent startSleepEvent = new StartSleepEvent(
						time, mulExp, server, initialState);
				mulExp.addEvent(startSleepEvent);
			} 
			else {

				// now set the server to active
				//server.setPowerState(Core.PowerState.ACTIVE);
				server.setActive();
				/************************************************************************/
				// fanyao added for accululating number of active server history

				if (experiment instanceof MultiServerExperiment) {
					MultiServerExperiment msExp = (MultiServerExperiment) experiment;
					msExp.updateServerHis(time, 1);
				}

				/************************************************************************/

				// server.setScheduledActive();
				// server.setCurrentSleepState(0);
				int initialState = server.getInitialSleepState();
				server.setNextFixedSleepState(initialState);

				// need to set lastEntersleepstate to 0.0
				// similar to CoreWakedupEvent
				server.setLastSleepStateTime(0.0);
				Task dequeuedTask,t;
				
				
				
				if (server instanceof ERoverServer)  { //&& ((ERoverServer)server).getWaitingTasksList().size() > 0 ) {
					while ( server.getRemainingCapacity() > 0 && ((ERoverServer)server).getWaitingTasksList().size() > 0) {
					 t = ((ERoverServer)server).getWaitingTasksList().poll();
					 dequeuedTask = mulExp.getGlobalQueue().polltask(time, t);
					 experiment.cancelEvent(dequeuedTask.getTaskTimerEvent());
						dequeuedTask.setTaskTimerEvent(null);
						
						Sim.debug(3,
								"wakedup server picked job: "
								+ dequeuedTask.getTaskId() + " server : "
								+ server.getNodeId());

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
//				 else {
//					dequeuedTask = globalQueue.poll(time);
//				}
				

			}
		}

	}
}
