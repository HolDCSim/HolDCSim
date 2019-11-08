package scheduler;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.Core.PowerState;
import infrastructure.DataCenter;
import infrastructure.ERoverStates;
import infrastructure.GeneralSleepController;
import infrastructure.ShallowDeepServer;
import infrastructure.ERoverServer;
import infrastructure.Server;
import infrastructure.ShallowDeepStates;
import infrastructure.UniprocessorServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
//import java.util.Random;
import java.util.Vector;

import queue.BaseQueue;
import queue.MSGlobalQueue;
import utility.AsynLoggedVector;
import utility.FakeRecordVector;
import utility.Pair;

import debug.Sim;

import job.Task;

import event.StartSleepEvent;
import event.MSTransToActiveEvent;
import experiment.ShallowDeepExperiment;
import experiment.ERoverExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * Scheduler for easy version of ERover When server is in low power mode, it
 * goes to s5 directly
 * 
 * @author fanyao
 * 
 */
public class ShallowDeepScheduler extends MultiServerScheduler {

	// use single workload threshold per server
	// Ww would be k*W_PER_SERVER
	// Ws would be (k-1)*_PER_SERVER
	// private static double T_PER_SERVER = 5;
	public static double Tw = 5.0;
	public static double Ts = 2.0;

	protected Vector<ShallowDeepServer> activeServers;
	protected Vector<ShallowDeepServer> lowPowerServers;

	private Random sleepIndexGen;
	private Random availServerIndexGen;

	protected Random wakeupIndexGen;

	int lastScheduledServer = 0;

	private Vector<Pair<Double, Integer>> schActiveServerHis;
	// private boolean dumpSchServerHis;
	private static String serverHisName = "sch_active_his.txt";

	private ShallowDeepScheduler(int numOfServers, SchedulerPolicy policy,
			ShallowDeepExperiment experiment) {
		super(experiment);
		// serverNoGen = new Random(10000);
		// T_PER_SERVER = experiment.getExpConfig().getWorkloadThreshold();
		// this.numOfSevers = numOfServers;
		// activeK = 1;
		// this.schPolicy = policy;

		activeServers = new Vector<ShallowDeepServer>();
		lowPowerServers = new Vector<ShallowDeepServer>();

		DataCenter dc = mExperiment.getDataCenter();
		Vector<Server> scheServers = dc.getServers();
		for (int i = 0; i < numOfServers; i++) {
			ShallowDeepServer server = (ShallowDeepServer) scheServers.get(i);

			server.setCurrentSleepState(1);
			// server.setPowerState(Core.PowerState.LOW_POWER_SLEEP);
			server.initialPowerState();

			// fanyao added: mark all servers as scheduled active at the
			// beginning
			server.setScheduledActive();
			activeServers.add(server);

		}

		if (Sim.RANDOM_SEEDING) {
			sleepIndexGen = new Random(System.currentTimeMillis());
			availServerIndexGen = new Random(System.currentTimeMillis());
			wakeupIndexGen = new Random(System.currentTimeMillis());

		} else {
			sleepIndexGen = new Random(99999);
			availServerIndexGen = new Random(11111);
			wakeupIndexGen = new Random(23222);

		}

		// this.Ww = activeK * T_PER_SERVER;
		// this.Ws = (activeK - 1) * T_PER_SERVER;

		// queue = new MSGlobalQueue(mExperiment.getExpConfig()
		// .dumpQueueHis(), this);

		// dumpSchServerHis = experiment.getExpConfig().dumpSchServerHis();
		boolean asyLogging = experiment.getExpConfig().isAsynLogging();

		if (asyLogging) {
			schActiveServerHis = new AsynLoggedVector<Pair<Double, Integer>>(serverHisName);
			experiment.registerLoggedVectors((AsynLoggedVector<?>) schActiveServerHis);
		} else {
			// schActiveServerHis = new FakeRecordVector<Pair<Double,
			// Integer>>();
			schActiveServerHis = new Vector<Pair<Double, Integer>>();
		}

		// initialize schServer his
		this.updateSchServerHis(0.0, 0);

	}

	protected void updateSchServerHis(double time, int delta) {
		// needs to keep the last record
		// if (!dumpSchServerHis) {
		// return;
		// }
		Pair<Double, Integer> currentHis = new Pair<Double, Integer>();
		if (time == 0.0) {
			currentHis.setFirst(0.0);
			currentHis.setSecond(activeServers.size());

		} else {
			// collect stats for num of active servers history
			// Pair<Double, Integer> his = new Pair<Double, Integer>();
			Pair<Double, Integer> pre = schActiveServerHis.lastElement();
			currentHis.setSecond(pre.getSecond() + delta);
			currentHis.setFirst(time);
			// schActiveServerHis.add(his);
		}
		schActiveServerHis.add(currentHis);
	}

	public void dumpPredictionHis() {
		mPredictor.dumpPredictionHis();
	}

	/**
	 * dump history of server set to active by the scheduler
	 */
	public void dumpSchServerHis() {

		if (schActiveServerHis == null) {
			Sim.warning("no controlled active server history from this scheduler");

		}
		if (schActiveServerHis.size() == 0) {
			return;
		}
		File serverHisFile = new File(serverHisName);
		try {
			FileWriter fw = new FileWriter(serverHisFile, false);
			BufferedWriter bw = new BufferedWriter(fw);
			// bw.write(String.format("%-15s", "time")
			// + String.format("%-15s", "servers_set_to_active"));
			// bw.newLine();

			// write history to file
			for (Pair<Double, Integer> aPair : schActiveServerHis) {
				double time = aPair.getFirst();
				int schServers = aPair.getSecond();

				bw.write(String.format("%-15.5f", time)
						+ String.format("%-15d", schServers));
				bw.newLine();
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public ShallowDeepScheduler(ShallowDeepExperiment experiment) {
		this(experiment.getExpConfig().getServersToSchedule(),
				SchedulerPolicy.RANDOM, experiment);

	}
	public void wakeUpServer(ERoverServer server, double time) {
//		server.setScheduledActive();
//		
//		if (server.isActive() || server.isTransitionToActive()) {	
//			/*this is not necessary since already done in setScheduleActive*/
//			// int initialState = server.getInitialSleepState();
//			// server.setNextSleepState(initialState);
//
//			/*
//			 * if server status is transition_to_active we need to
//			 * nullify sleepEvent so that MSTransToActive knows that the
//			 * server should be active
//			 */
//			if (server.isTransitionToActive()) {
//				Sim.debug(3,
//						"TTT Time: " + time
//						+ " transitioning server : "
//						+ server.getNodeId()
//						+ " would be moved to activearrays");
//			} else {
//				Sim.debug(3, "TTT Time: " + time + " active server : "
//						+ server.getNodeId()
//						+ " would be moved to activearrays");
//			}
//			server.setToSleepEvent(false);
//			return;
//		}

		// otherwise, server is in deep sleep state
		Sim.debug(3,
				"ooo Time: " + time + " Server : " + server.getNodeId()
				+ " would be waked up"
				+ " now active servers: "
				+ activeServers.size());

		/**********************************************************************/
//		// update idle record
//		// Core singleCore = server.getCore();
//		AbstractSleepController gsController = server.getSleepController();
//		Vector<IdleRecord> idleDistribution = gsController.getIdleDistribution();
//		if (idleDistribution.size() == 0) {
//			IdleRecord firstRecord = new IdleRecord();
//			firstRecord.startIdleTime = 0.0;
//			firstRecord.sleepstate = gsController
//					.getCurrentSleepState();
//			idleDistribution.add(firstRecord);
//		}
//		IdleRecord record = idleDistribution.lastElement();
//		if (record.duration != IdleRecord.INITIAL) {
//			Sim.fatalError("attempt to set idlerecord duration twice");
//		}
//		record.sleepstate = gsController.getCurrentSleepState();
//		
//		if(server.getNodeId() == 1) {
//			System.out.println("Setting record duration for server 1");
//		}
//		record.duration = time - record.startIdleTime;
		/**********************************************************************/

		/**********************************************************************/
		// cancel the next sleep state event since multiple sleep state are
		// used
		int serverState = server.getCurrentSleepState();

		if (serverState != server.getDeepestSleepState()) {
			StartSleepEvent event = server.getNextSleepEvent();

			/********************************************************
			 * //with s4 provision, it is possible that nextsleepsvent
			 * is null if (event == null) Sim.fatalError(
			 * "fatal error, using multiple states but no valid next state event found"
			 * ); mExperiment.cancelEvent(event);
			 ********************************************************/

			if (event != null)
				mExperiment.cancelEvent(event);

			if (serverState == ERoverStates.DEEP_SLEEP) {
				ERoverExperiment mseeExp = (ERoverExperiment) mExperiment;
				if (mseeExp.currentNumOfS4Servers >= 1) {
					mseeExp.currentNumOfS4Servers--;

					Sim.debug(3, "s4 server : " + server.getNodeId()
							+ " wakeup: current s4 servers : "
							+ mseeExp.currentNumOfS4Servers
							+ " s4 provision: "
							+ mseeExp.numOfMinS4Servers);
				}
			}

		}

//		/**********************************************************************/
//
//		/* mark the server as scheduled active */
//		server.setScheduledActive();
//		int initialState = server.getInitialSleepState();
//		server.setNextFixedSleepState(initialState);

		/**********************************************************************/
//		// generate transition event
//		double transitionTime = server.getTansitionToActiveTime();
//		MSTransToActiveEvent event = new MSTransToActiveEvent(time
//				+ transitionTime, (ShallowDeepExperiment) mExperiment,
//				server, server.getCurrentSleepState());
//		//fanyao added: accumulate wakeup time for specific event
//		
//		server.setToActiveEvent(event);
//		mExperiment.addEvent(event);
//		/**********************************************************************/
//		/*
//		 * update current powerstate after getCurrentSleepState()
//		 */
//		// server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
//		server.setTransitionToActive();
	}
	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		if (schPolicy == SchedulerPolicy.ROUND_ROBIN) {
			// schedule all the tasks to one server
			lastScheduledServer++;
			Server server;
			server = mExperiment.getDataCenter().getServers()
					.get(lastScheduledServer % numOfSevers);
			_task.setServer(server);
			return server;
		}

		else if (schPolicy == SchedulerPolicy.RANDOM) {

			// updateThresholds(time);

			if (allServersBusy()) {
				queue.add(_task, time);
				
				return null;
			}

			else {
				UniprocessorServer server;
				server = findAvailableServer();
				if (server == null) return null;
				Sim.debug(5,"--Shallow Deep Scheduler--- Job "+ _task.getJobId()+ " " + _task.getTaskId() + " scheduled " + mExperiment.printStateofServers());
				// Cancel pending server sleep event
				if(server.getSleepController().getCurrentSleepState() != server.getSleepController().getDeepestSleepState()) {
					StartSleepEvent startSleepEvent = server.getSleepController().getNextSleepEvent();
					if(startSleepEvent != null) {
						Sim.debug(5,"Cancelling next sleepevent ShallowdeepScheduler Job "+ _task.getJobId()+ " " + _task.getTaskId() + " scheduled " + mExperiment.printStateofServers());
						mExperiment.cancelEvent(startSleepEvent);
					}
				}
				
				if(server instanceof ERoverServer) {
					ERoverServer wakeServer = (ERoverServer)server;
					if(server.getCurrentSleepState() == 4 || server.getCurrentSleepState() == 5) {
						if(!wakeServer.isTransitionToActive()) {
							wakeUpServer(wakeServer, time);
						}
						return null;
					}
				}

				// if (server.getCurrentSleepState() == deepState) {
				// this indicates that the server is still in scheduled low
				// power state
				if (server.doUseMultipleSleepStates()) {
					Sim.fatalError("fatal error: server is in deep sleep state in activeServers array");
				}

				Sim.debug(3, "||| task " + _task.getTaskId()
						+ " picked shallow sleep server: " + server.getNodeId());
				// activeServers.remove(server);
				// Task oneTask = queue.poll();

				ShallowDeepServer eeServer = (ShallowDeepServer) server;

				/***************************************************************/
				/*
				 * marker to let updateThresholds know that do not put this
				 * server to sleep,
				 */
				eeServer.setJobAssigned(true);
				/*
				 * update thresholds needed here as in such scenario there are
				 * no jobs in the global queue
				 */
				updateThresholds(time);
				/*
				 * this needs to be unset after updateThresholds() so that this
				 * server could be put to sleep later
				 */
				eeServer.setJobAssigned(false);
				/***************************************************************/

				_task.setServer(server);

				return server;

			}
		}

		return null;

	}

	//only find available server in the active server pool
	@Override
	protected UniprocessorServer randomAvailServer() {
		UniprocessorServer highUtiServer = null;
		Vector<UniprocessorServer> availableServers = new Vector<UniprocessorServer>();
		
		for (UniprocessorServer aServer : activeServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				if(!aServer.isServerAllIdle() ){
					if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()){
						highUtiServer = aServer;
					}
				}
				//int currentState = aServer.getCurrentSleepState();
				else if (serverSleepState == ShallowDeepStates.SHALLOW) {
					// idleServer = aServer;
					availableServers.add(aServer);

				} 
				else{
					Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler " + serverSleepState);
				}
			}
		}

		if(highUtiServer != null){
			return highUtiServer;
		}
				
		int numOfIdleServers = availableServers.size();
		if(numOfIdleServers == 0){
			return null;
		}

		int index = 0;

		index = indexGen.nextInt(numOfIdleServers);
		return availableServers.get(index);

	}

	@Override
	protected UniprocessorServer firstAvailServer() {
		// TODO Auto-generated method stub
		ShallowDeepServer server = null;
		// FIXME: shuffle method may be time-consuming
		// remove shuffle in order to reproduce result

		/** Collections.shuffle(activeServers); **/
		// Collections.shuffle(activeServers);
		for (ShallowDeepServer aServer : activeServers) {
			if (!aServer.isServerAllBusy()) {
				server = aServer;
				break;
			}
		}

		return server;
	}

	@Override
	protected boolean allServersBusy() {
		boolean allBusy = true;
		for (UniprocessorServer aServer : activeServers) {
			if (!aServer.isServerAllBusy()) {
				allBusy = false;
				break;
			}
		}

		return allBusy;
	}

	protected UniprocessorServer findServerToSleep(double time) {
		return randomServerToSleep();
	}

	protected UniprocessorServer randomServerToSleep() {

		UniprocessorServer selectedServer = null;
		Vector<UniprocessorServer> idleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> busyServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> transServers = new Vector<UniprocessorServer>();

		for (ShallowDeepServer aServer : activeServers) {
			/*modified for multicore scenario*/
			//if (!aServer.isServerBusy()) {
			if (! aServer.isServerBusy()) {
				if (!aServer.isJobAssigned())
				{
					if (aServer instanceof  ERoverServer && ((ERoverServer)aServer).getWaitingTasksList().size() == 0)
					idleServers.add(aServer);
				}
				else {
					aServer.setJobAssigned(false);
				}
			}

			else {
				// if (aServer.getPowerState() ==
				// PowerState.TRANSITIONING_TO_ACTIVE) {
				if (aServer.isTransitionToActive()) {
					transServers.add(aServer);
				} else {
					busyServers.add(aServer);
				}
			}

		}

		/*
		 * the current priority is oriented to save more energy rather than
		 * performance
		 */
		if (idleServers.size() > 0) {
			int index = sleepIndexGen.nextInt(idleServers.size());
			selectedServer = idleServers.get(index);
		}

		else if (transServers.size() > 0) {
			int index = sleepIndexGen.nextInt(transServers.size());
			selectedServer = transServers.get(index);
		}

		else if (busyServers.size() > 0) {
			int index = sleepIndexGen.nextInt(busyServers.size());
			selectedServer = busyServers.get(index);
		}

		if (selectedServer instanceof  ERoverServer && ((ERoverServer)selectedServer).getWaitingTasksList().size() != 0) {
			selectedServer = null;
		}

		return selectedServer;

	}

	protected UniprocessorServer firstServerToSleep() {
		ShallowDeepServer transitionServer = null;
		ShallowDeepServer busyServer = null;
		for (ShallowDeepServer aServer : activeServers) {
			if (!aServer.isServerAllBusy()) {
				if (aServer.isJobAssigned()) {
					aServer.setJobAssigned(false);
				} else {
					return aServer;
				}
			}

			// else if (aServer.getPowerState() ==
			// PowerState.TRANSITIONING_TO_ACTIVE) {
			else if (aServer.isTransitionToActive()) {
				transitionServer = aServer;
			}

			else if (busyServer == null) {
				busyServer = aServer;
			}
		}
		// if there is no idle server, we pick one that have least impact of
		// the
		// current job processing performance

		if (transitionServer == null)
			return busyServer;
		else
			return transitionServer;
	}

	// // return true if the threshold has been changed
	// public boolean updateThresholds(double time) {
	// Sim.debug(3,
	// "!!! Time: " + time + " jobs in global queue: " + queue.size());
	// int k = queue.size();
	// if (k > Ww + T_PER_SERVER - 1) {
	// if (lowPowerServers.size() > 0) {
	// activeK++;
	// this.Ww = activeK * T_PER_SERVER;
	// this.Ws = (activeK - 1) * T_PER_SERVER;
	//
	// EnergyAwareServer server = lowPowerServers.remove(0);
	//
	// /**
	// * server in lowPower vector could be busy at the time picked if
	// * the threshold transitions quickly. check if the server is
	// * busy if busy, don't make the server wait to deep sleep state
	// * DO NOT trigger MSTransToAcitiveEvent
	// */
	//
	// activeServers.add(server);
	// if (server.isServerBusy()) {
	// server.setNextSleepState(shallowState);
	//
	// // if server status is transition_to_active
	// // we need to nullify sleepEvent so that MSTransToActive
	// // knows that the server should be active
	// server.setToSleepEvent(null);
	// return true;
	// }
	//
	// Sim.debug(3,
	// "ooo Time: " + time + " server " + server.getNodeId()
	// + " would be waked up");
	//
	// // update idle record
	// Core singleCore = server.getCore();
	// Vector<IdleRecord> idleDistribution = singleCore
	// .getIdleDistribution();
	// if (idleDistribution.size() == 0) {
	// IdleRecord firstRecord = new IdleRecord();
	// firstRecord.startIdleTime = 0.0;
	// firstRecord.sleepstate = singleCore.getCurrentSleepState();
	// idleDistribution.add(firstRecord);
	// }
	// IdleRecord record = idleDistribution.lastElement();
	// if (record.duration != 0.0) {
	// Sim.fatalError("attempt to set idlerecord duration twice");
	// }
	// record.sleepstate = singleCore.getCurrentSleepState();
	// record.duration = time - record.startIdleTime;
	//
	// double transitionTime = server.getTansitionToActiveTime();
	// server.setPowerState(Core.PowerState.TRANSITIONINGG_TO_ACTIVE);
	// MSTransToActiveEvent event = new MSTransToActiveEvent(time
	// + transitionTime, (MultiServerExperiment) mExperiment,
	// server, shallowState, deepState);
	// server.setToActiveEvent(event);
	// mExperiment.addEvent(event);
	// return true;
	// }
	//
	// else
	// return false;
	// }
	//
	// else if (k < Ws + 1) {
	// // always keep at least one server to be active
	// if (activeServers.size() > 1) {
	// EnergyAwareServer server = findServerToSleep(time);
	// lowPowerServers.add(server);
	// activeServers.remove(server);
	// MSTransToSleepEvent event = new MSTransToSleepEvent(time,
	// (MultiServerExperiment) mExperiment, server,
	// shallowState, deepState);
	// mExperiment.addEvent(event);
	//
	// activeK = (activeK - 1) > 0 ? activeK - 1 : 0;
	// this.Ww = activeK * T_PER_SERVER;
	//
	// if (activeK > 0) {
	// this.Ws = Ww - T_PER_SERVER;
	// } else
	// this.Ws = -1;
	//
	// return true;
	// }
	//
	// else
	// return false;
	// }
	//
	// else
	// return false;
	//
	// }

	public boolean updateThresholds(double time) {

		int queueSize = queue.size();
		Sim.debug(3, "!!! Time: " + time + " jobs in global queue: "
				+ queueSize);
		// if (queue.size() !=
		// queue.getGlobalQueueHis().lastElement().getSecond())
		// Sim.fatalError("fator error, current queue size not equal to queue history");
		double currentWorkload = mPredictor.predictCurWorkload(time, queueSize);
		if (currentWorkload > Tw) {
			Sim.debug(3, "TTT current workload per server " + currentWorkload
					+ " exceeds " + " Tw " + Tw);
			if (lowPowerServers.size() > 0) {
				// this.Ww = activeK * T_PER_SERVER;
				// this.Ws = (activeK - 1) * T_PER_SERVER;

				// collect stats for num of active servers history
				// Pair<Double, Integer> his = new Pair<Double, Integer>();
				// Pair<Double, Integer> pre = schActiveServerHis.lastElement();
				// his.setSecond(pre.getSecond() + 1);
				// his.setFirst(time);
				// schActiveServerHis.add(his);
				this.updateSchServerHis(time, 1);

				ShallowDeepServer server = (ShallowDeepServer) findServerToWakeup();
				activeServers.add(server);
				lowPowerServers.remove(server);
				Sim.debug(5,"<<<< Shallowdeepscheduler:UpdateThresholds add server to active : " + server.getNodeId() + " going to woken up "+  this.getExperiment().printStateofServers());
				/**
				 * server in lowPower vector could be busy at the time picked if
				 * the load gets updated quickly. check if the server is
				 * busy. If busy, don't make the server go to deep sleep state.
				 * If the server is transiting to active, we need to set it as 
				 * scheduled active. 
				 * DO NOT trigger MSTransToAcitiveEvent
				 */

				/*server could be busy or transiting to active*/
				/*modified for multi-core scenario*/
				//if (server.isServerBusy()) {
				
				/* mark the server as scheduled active */
				server.setScheduledActive();
				
				if (server.isActive() || server.isTransitionToActive()) {	
					/*this is not necessary since already done in setScheduleActive*/
					// int initialState = server.getInitialSleepState();
					// server.setNextSleepState(initialState);

					/*
					 * if server status is transition_to_active we need to
					 * nullify sleepEvent so that MSTransToActive knows that the
					 * server should be active
					 */
					if (server.isTransitionToActive()) {
						Sim.debug(3, "TTT Time: " + time
								+ " transitioning server : "
								+ server.getNodeId()
								+ " would be moved to activearrays");
					} else {
						Sim.debug(3, "TTT Time: " + time + " active server : "
								+ server.getNodeId()
								+ " would be moved to activearrays");
					}
					server.setToSleepEvent(false);
					return true;
				}

				// otherwise, server is in deep sleep state
				Sim.debug(3, "ooo Time: " + time + " Server : " + server.getNodeId()
						+ " would be waked up"
						+ " now active servers: "
						+ activeServers.size());

				/**********************************************************************/
				// update idle record
				// Core singleCore = server.getCore();
				AbstractSleepController gsController = server.getSleepController();
				Vector<IdleRecord> idleDistribution = gsController.getIdleDistribution();
				if (idleDistribution.size() == 0) {
					IdleRecord firstRecord = new IdleRecord();
					firstRecord.startIdleTime = 0.0;
					firstRecord.sleepstate = gsController.getCurrentSleepState();
					idleDistribution.add(firstRecord);
				}
				IdleRecord record = idleDistribution.lastElement();
				if (record.duration != IdleRecord.INITIAL) {
					Sim.fatalError("attempt to set idlerecord duration twice");
				}
				record.sleepstate = gsController.getCurrentSleepState();
				
				if(server.getNodeId() == 1) {
					System.out.println("Setting record duration for server 1");
				}
				record.duration = time - record.startIdleTime;
				/**********************************************************************/

			
//				/* mark the server as scheduled active */
//				server.setScheduledActive();
//				int initialState = server.getInitialSleepState();
//				server.setNextFixedSleepState(initialState);

				/**********************************************************************/
				// generate transition event
				double transitionTime = server.getTansitionToActiveTime();
				MSTransToActiveEvent event = new MSTransToActiveEvent(time
						+ transitionTime, (ShallowDeepExperiment) mExperiment,
						server, server.getCurrentSleepState());
				//fanyao added: accumulate wakeup time for specific event
				
				server.setToActiveEvent(event);
				mExperiment.addEvent(event);
				/**********************************************************************/

				/*
				 * update current powerstate after getCurrentSleepState()
				 */
				// server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
				server.setTransitionToActive();
				return true;
			}

			else {
				Sim.debug(3, "TTT current workload per server "
						+ currentWorkload + " exceeds " + " Tw " + Tw
						+ " all servers are put to active");
				return false;
			}
		}

		else if (currentWorkload < Ts) {

			// always keep at least one server to be active
			if (activeServers.size() > 1) {
				Sim.debug(3, "TTT time : " + time
						+ " current workload per server " + currentWorkload
						+ " less than " + " Ts " + Ts);

				// collect stats
				// Pair<Double, Integer> his = new Pair<Double, Integer>();
				// Pair<Double, Integer> pre = schActiveServerHis.lastElement();
				// his.setSecond(pre.getSecond() - 1);
				// his.setFirst(time);
				// schActiveServerHis.add(his);
				this.updateSchServerHis(time, -1);

				/*
				 * findServerToSleep won't return null if the only active server
				 * found is already assigned a job
				 */
				ShallowDeepServer server = (ShallowDeepServer) findServerToSleep(time);
				lowPowerServers.add(server);
				activeServers.remove(server);

				/* unset scheduled active flag */
				server.unsetScheduledActive();
				
				Sim.debug(5,">>>> Shallowdeepscheduler:UpdateThresholds remove server to sleep : " + server.getNodeId() + " going to sleep "+  this.getExperiment().printStateofServers());

				Sim.debug(3,
						"ooo Time: " + time + " server : " + server.getNodeId()
						+ " would be put to deep sleep state"
						+ " now active servers: "
						+ activeServers.size());

				/*
				 * this means one server has just finished its task and is about to
				 * go to shallow sleep state
				 */
				if (server.isActive() && server.getSingelSleepEvent() != null) {
					// put the server to deep sleep instead
					StartSleepEvent event = server.getSingelSleepEvent();
					event.setSleepState(server.getInitialSleepState());

					// no need to send MSTransToSleepEvent
					return true;
				}
				// if(server == null){
				// return false;
				// }

				// MSTransToSleepEvent event = new MSTransToSleepEvent(time,
				// (EnergyAwareExperiment) mExperiment, server,
				// shallowState, deepState);
				// mExperiment.addEvent(event);

				// server is in shallow sleep state
				/*change to isAllIdle for multicore scenario*/
				//if (!server.isServerBusy()) {
				if (! server.isServerBusy()) {
					StartSleepEvent coreStartSleepEvent = server
							.generateSleepEvent(time, mExperiment,
									server.getInitialSleepState());

					AbstractSleepController gsController = server
							.getSleepController();
					// Core singleCore = server.getCore();

					Vector<IdleRecord> idleDistribution = gsController
							.getIdleDistribution();
					// //when there is no record, this means the server is in
					// the intial
					// shallow sleep state
					// if(idleDistribution.size() == 0){
					// IdleRecord firstRecord = new IdleRecord();
					// firstRecord.s
					// }
					IdleRecord record = idleDistribution.lastElement();

					/*
					 * updating the last sleep state record, which should be
					 * shallow sleep state
					 */

					/* FIXME: hardcoded to 1 for shallow state*/
					if (record.duration != IdleRecord.INITIAL
							|| record.sleepstate != 1) {
						Sim.fatalError("attempt to update the wrong record");
					}
					// if (record.duration != 0.0
					// || record.sleepstate != shallowSleepState) {
					// Sim.fatalError("attempt to update the wrong record");
					// }
					record.sleepstate = gsController.getCurrentSleepState();
					
					if(server.getNodeId() == 1) {
						System.out.println("Setting record duration for server 1");
					}
					record.duration = time - record.startIdleTime;

					// wrap up the sleep time for original shallow sleep state
					/*
					 * FIXME: only works when shallow sleep state is 1, check
					 * implementation of accumulateSSTimes() 
					 */
					/*
					 * FIXME: hardcoded to 1 for shallow state. This is an exception. 
					 * accumulateSSTime typically are called when unit is wakedup. If called
					 * other than that, it may pollute the wakeupsplits data. In the current 
					 * scenario it is okay since the wakeup time for sleep state 1 is 0.
					 */
					gsController.accumulateSSTimes(1, time - gsController.getLastEnterSleepStateTime());
					// begin count the new sleepstate durations
					gsController.setLastEnterSleepStateTime(0.0);

					/////////////////////////////////////////////////////////
					/**
					 * this is not necessary since coreEnteredParkEvent would
					 * create the new record
					 */
					// begin new record
					/*
					 * IdleRecord newRecord = new IdleRecord();
					 * newRecord.startIdleTime = time;
					 * idleDistribution.add(newRecord);
					 */
					/////////////////////////////////////////////////////////

					// add park event to experiment
					mExperiment.addEvent(coreStartSleepEvent);
				}

				else {
					/*
					 * if server is busy,it could be active or transition_to_active. 
					 * When transtion_to_active, there should be a MSTransToActive event.
					 * When the server is currently busy, the only work is to unsetscheduledactive,
					 * which has already been set before
					 */

					// active/wakeup --> sleep
					// MSTransToActiveEvent toActiveEvent =
					// server.getToActiveEvent();
					// if (toActiveEvent != null)
					// mulExp.cancelEvent(toActiveEvent);
					// server.setToActiveEvent(null);

					/*
					 * FIXME: if the shallow sleepstate is not 1, the server may
					 * be currently transitioning out of shallow sleep state,
					 * then we need to distinguish from which sleepstate it is
					 * transitioning from
					 */

					/*
					 * if the active server just finished a job and called
					 * updateThresholds
					 */
					server.setNextFixedSleepState(server.getInitialSleepState());
					// EnergyAwareServer eeServer = (EnergyAwareServer) server;
					// CoreStartSleepEvent event =
					// eeServer.getSingelSleepEvent();
					// if (event != null) {
					// if (event.getSleepState() != 6) {
					// Sim.fatalError("fatal error, try to put a deep sleep server to deepsleeparrays");
					// }
					//
					// }
					// if (server.getPowerState() ==
					// PowerState.TRANSITIONING_TO_ACTIVE) {
					if (server.isTransitionToActive()) {
						server.setToSleepEvent(true);
					}
				}

				// this.Ww = activeK * T_PER_SERVER;
				//
				// if (activeK > 0) {
				// this.Ws = Ww - T_PER_SERVER;
				// } else
				// this.Ws = -1;

				return true;
			}

			else {
				Sim.debug(3, "TTT time: " + time
						+ " current workload per server " + currentWorkload
						+ " less than " + " Ts " + Ts + " only 1 server active");

				return false;
			}
		}

		else {
			Sim.debug(3, "TTT time: " + time + " current workload per server "
					+ currentWorkload + " is between " + Ts + " " + Tw);
			return false;
		}

	}

	protected UniprocessorServer findServerToWakeup() {
		// TODO Auto-generated method stub
		// FIXME: needs more intelligence here
		return randomServerToWakeup();
	}

	/**
	 * always pick the server that needs less preparation time
	 * 
	 * @return
	 */
	protected UniprocessorServer randomServerToWakeup() {
		/*
		 * server priority: 1. active servers 2. transitioning servers 3.
		 * deepstate server 4 deepest state server
		 */
		UniprocessorServer selectedServer = null;
		Vector<UniprocessorServer> lpServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> activeServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> transServers = new Vector<UniprocessorServer>();

		for (ShallowDeepServer aServer : lowPowerServers) {
			/*modified for multi-core scenario*/
			//if (!aServer.isServerBusy()) {
			if (! aServer.isServerBusy()) {
				if (aServer.getCurrentSleepState() == aServer.getDeepestSleepState())
					lpServers.add(aServer);
				else
					Sim.fatalError("unexpteced sleep state in EnergyAware experiment: "
							+ aServer.getCurrentSleepState());
			}

			else {
				// if (aServer.getPowerState() ==
				// PowerState.TRANSITIONING_TO_ACTIVE) {
				if (aServer.isTransitionToActive()) {
					transServers.add(aServer);
				} else {
					activeServers.add(aServer);
				}
			}

		}

		if (activeServers.size() > 0) {
			int index = wakeupIndexGen.nextInt(activeServers.size());
			selectedServer = activeServers.get(index);
		}

		/*
		 * we dont know how long it is the transition left, so currently just
		 * keep this priority order
		 */
		else if (transServers.size() > 0) {
			int index = wakeupIndexGen.nextInt(transServers.size());
			selectedServer = transServers.get(index);
		}

		else if (lpServers.size() > 0) {
			int index = wakeupIndexGen.nextInt(lpServers.size());
			selectedServer = lpServers.get(index);
		}

		lowPowerServers.remove(selectedServer);
		return selectedServer;

	}

	protected UniprocessorServer firstServerToWakeup() {
		/*
		 * server priority: 1. active servers 2. transitioning servers 3.
		 * deepstate server 4 deepest state server
		 */
		UniprocessorServer selectedServer = null;
		Vector<UniprocessorServer> lpServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> activeServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> transServers = new Vector<UniprocessorServer>();

		for (ShallowDeepServer aServer : lowPowerServers) {
			if (!aServer.isServerAllBusy()) {
				if (aServer.getCurrentSleepState() == aServer
						.getDeepestSleepState())
					lpServers.add(aServer);
				else
					Sim.fatalError("unexpteced sleep state in EnergyAware experiment: "
							+ aServer.getCurrentSleepState());
			}

			else {
				// if (aServer.getPowerState() ==
				// PowerState.TRANSITIONING_TO_ACTIVE) {
				if (aServer.isTransitionToActive()) {
					transServers.add(aServer);
				} else {
					activeServers.add(aServer);
				}
			}

		}

		if (activeServers.size() > 0) {
			// int index = wakeupIndexGen.nextInt(activeServers.size());
			selectedServer = activeServers.get(0);
		}

		/*
		 * we dont know how long the transition left, so currently just keep
		 * this priority order
		 */
		else if (transServers.size() > 0) {
			// int index = wakeupIndexGen.nextInt(transServers.size());
			selectedServer = transServers.get(0);
		}

		else if (lpServers.size() > 0) {
			// int index = wakeupIndexGen.nextInt(lpServers.size());
			selectedServer = lpServers.get(0);
		}

		lowPowerServers.remove(selectedServer);
		return selectedServer;

	}

	public Vector<Pair<Double, Integer>> getActiveServerHis() {
		return this.schActiveServerHis;
	}

	@Override
	protected BaseQueue createQueue() {
		return new MSGlobalQueue(
				mExperiment.getExpConfig().doCollectQueueHis(), this);
	}
	

}
