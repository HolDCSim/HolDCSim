package scheduler;

import java.util.*;

import debug.Sim;
import queue.BaseQueue;
import queue.MSGlobalQueue;
import queue.PriorityTaskQueue;
import infrastructure.AbstractSleepController;
import infrastructure.ERoverServer;
import infrastructure.ERoverStates;
import infrastructure.ShallowDeepServer;
import infrastructure.ShallowDeepStates;
import infrastructure.UniprocessorServer;
import infrastructure.Core.PowerState;
import event.MSTransToActiveEvent;
import event.StartSleepEvent;
import experiment.ERoverExperiment;
import experiment.ExperimentConfig;
import experiment.ShallowDeepExperiment;
import experiment.SingletonJobExperiment;
import experiment.SingletonJobExperiment.IdleRecord;
import experiment.SingletonJobExperiment.JobWorkload;
import topology.Topology;

/**
 * ERover scheduler class
 * 
 * @author fanyao
 * 
 */
public class ERoverScheduler extends ShallowDeepScheduler {

	public ERoverScheduler(ShallowDeepExperiment experiment) {
		super(experiment);

		// TODO Auto-generated constructor stub
	}

//	@Override
//	protected UniprocessorServer findServerToWakeup() {
//		// TODO Auto-generated method stub
//		return randomServerToWakeup();
//	}

	@Override
	protected UniprocessorServer randomServerToWakeup() {
		/*
		 * server priority: 1. active servers 2. transitioning servers 3.
		 * deepstate server 4 deepest state server
		 */
		UniprocessorServer selectedServer = null;
		Vector<UniprocessorServer> lp1Servers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lp2Servers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> activeServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> transServers = new Vector<UniprocessorServer>();

		for (ShallowDeepServer aServer : lowPowerServers) {
			if (!aServer.isServerBusy()) {
				if (aServer.getCurrentSleepState() == ERoverStates.DEEP_SLEEP)
					lp1Servers.add(aServer);
				else
					lp2Servers.add(aServer);
			}

			else {
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

		else if (transServers.size() > 0) {
			int index = wakeupIndexGen.nextInt(transServers.size());
			selectedServer = transServers.get(index);
		}

		else if (lp1Servers.size() > 0) {
			int index = wakeupIndexGen.nextInt(lp1Servers.size());
			selectedServer = lp1Servers.get(index);
		}

		else if (lp2Servers.size() > 0) {
			int index = wakeupIndexGen.nextInt(lp2Servers.size());
			selectedServer = lp2Servers.get(index);
		}

		lowPowerServers.remove(selectedServer);
		return selectedServer;

	}
	


	public boolean updateThresholds(double time) {

		int queueSize = queue.size();
		
				if ((this.getExperiment() instanceof SingletonJobExperiment) &&  
						(this.getExperiment().getExpConfig().getNetworkRoutingAlgorithm() != Topology.NetworkRoutingAlgorithm.WASP ||
								(((SingletonJobExperiment) this.getExperiment()).sleepUnit == constants.Constants.SleepUnit.NO_SLEEP))) {
							return false;
						}

		Sim.debug(3, "!!! Update thresholds--ERover Time: " + time + " jobs in global queue: "
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

				ERoverServer server = (ERoverServer) findServerToWakeup();
				activeServers.add(server);
				lowPowerServers.remove(server);
				Sim.debug(5,"<<<< UpdateThresholds add server to active : " + server.getNodeId() + " going to be woken up"+  this.getExperiment().printStateofServers());
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
						Sim.debug(3,
								"TTT Time: " + time
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
				Sim.debug(3,
						"ooo Time: " + time + " Server : " + server.getNodeId()
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
					firstRecord.sleepstate = gsController
							.getCurrentSleepState();
					idleDistribution.add(firstRecord);
				}
				IdleRecord record = idleDistribution.lastElement();
				
				
				if (record.duration != IdleRecord.INITIAL) {
		//			Sim.fatalError("attempt to set idlerecord duration twice");
				}
				record.sleepstate = gsController.getCurrentSleepState();
				
				if(server.getNodeId() == 1) {
//					System.out.println("break");
				}
				
				if(server.getNodeId() == 1) {
//					System.out.println("Setting record duration for server 1");
				}
				record.duration = time - record.startIdleTime;
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

//				/**********************************************************************/
//
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
			if (activeServers.size() > 2) {
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
				ERoverServer server = (ERoverServer) findServerToSleep(time);
				if (server == null) {
					return true;
				}
				lowPowerServers.add(server);
				activeServers.remove(server);

				/* unset scheduled active flag */
				server.unsetScheduledActive();
				Sim.debug(5,">>>> UpdateThresholds remove server to sleep : " + server.getNodeId() + " going to sleep "+  this.getExperiment().printStateofServers());
				Sim.debug(
						3,
						"ooo Time: " + time + " server : " + server.getNodeId()
								+ " would be put to deep sleep state"
								+ " now active servers: "
								+ activeServers.size());

				/*
				 * this means one job has just finished its task and is about to
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
//					Sim.fatalError("attempt to update the wrong record");

					}
					// if (record.duration != 0.0
					// || record.sleepstate != shallowSleepState) {
					// Sim.fatalError("attempt to update the wrong record");
					// }
					record.sleepstate = gsController.getCurrentSleepState();
					
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
					//server.setNextFixedSleepState(server.getInitialSleepState());
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

	@Override
	protected BaseQueue createQueue() {

		ExperimentConfig expConfig = mExperiment.getExpConfig();
		
		//use prioritytaskqueue if it is mixed workload
		if (mExperiment.getExpConfig().getJobWorkload() == JobWorkload.MIXED) {
			return new PriorityTaskQueue(expConfig.doCollectQueueHis(), this);
		} else {
			return new MSGlobalQueue(expConfig.doCollectQueueHis(), this);
		}
		
		//return new MSGlobalQueue(expConfig.dumpQueueHis(), this);
	}
	
	//only find available server in the active server pool
		@Override
		protected UniprocessorServer randomAvailServer() {
			UniprocessorServer highUtiServer = null;
			Vector<UniprocessorServer> availableServers = new Vector<UniprocessorServer>();
			Vector<UniprocessorServer> sleepingServers4 = new Vector<UniprocessorServer>();
			Vector<UniprocessorServer> sleepingServers5 = new Vector<UniprocessorServer>();

			
			for (UniprocessorServer aServer : activeServers) {
				
				int serverSleepState = aServer.getCurrentSleepState();
				if (!aServer.isServerAllBusy()) {
					
					if(!aServer.isServerAllIdle()) {
						if(highUtiServer == null || highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()){
							highUtiServer = aServer;
						}
					}
					
					// Server is all idle and in LPI mode
					else if (serverSleepState == 1) {
						availableServers.add(aServer);

					}
					
					// Server is all idle but kept active bc ERoverScheduler requires 1 server to be active at all times
					else if(activeServers.size() == 1) {
						availableServers.add(aServer);
						
					}
					if (aServer.getCurrentSleepState() == 4 ) {
						sleepingServers4.add(aServer);
					} else if (aServer.getCurrentSleepState() == 5){
						sleepingServers5.add(aServer);
					}
				}
			}
			int index = 0;

			

			if(highUtiServer != null){
				return highUtiServer;
			}
					
			int numOfIdleServers = availableServers.size();
			if(numOfIdleServers > 0){
				index = indexGen.nextInt(numOfIdleServers);
				return availableServers.get(index);
				// check in the sleeping servers
				
			} else if (sleepingServers4.size() > 0) {
				return sleepingServers4.get(0);
				
			} else if (sleepingServers5.size() > 0) {
				return sleepingServers5.get(0);
			} else {
				return null;
			}
		
		}
		

}
