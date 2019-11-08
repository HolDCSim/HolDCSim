package scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import debug.Sim;
import event.LineCardTransitiontoSleepEvent;
import event.MSTransToActiveEvent;
import event.PortTransitiontoLPIEvent;
import event.StartSleepEvent;
import experiment.ERoverExperiment;
import experiment.Experiment;
import experiment.ShallowDeepExperiment;
import experiment.SingletonJobExperiment.IdleRecord;
import infrastructure.AbstractSleepController;
import infrastructure.DCNode;
import infrastructure.DataCenter;
import infrastructure.ERoverServer;
import infrastructure.ERoverSleepController;
import infrastructure.ERoverStates;
import infrastructure.LCSwitch;
import infrastructure.LineCard;
import infrastructure.LineCard.LineCardPowerPolicy;
import infrastructure.Port;
import infrastructure.Server;
import infrastructure.ShallowDeepServer;
import infrastructure.SwitchSleepController;
import infrastructure.UniprocessorServer;
import infrastructure.Port.PortPowerPolicy;
import infrastructure.Port.PortPowerState;
import job.Task;
import topology.Topology.NetworkRoutingAlgorithm;

public class LinecardNetworkScheduler extends ERoverScheduler {
	private Experiment mExp;
	private boolean isScheduleTimeout;	// if timeout, can be scheduled, otherwise use parent server or child server
	private Server parentServer;
	private Server childServer;

	// Used for picking random server
	private Random random;

	public LinecardNetworkScheduler(ERoverExperiment experiment) {
		//super(null);
		super(experiment);
		this.mExp = experiment;
		// TODO Auto-generated constructor stub
		this.isScheduleTimeout = true;

		// Initialize random generator for picking random server
		long randomSeed = mExp.getExpConfig().getSeed();
		if(randomSeed == -1) {
			// True random seed
			random = new Random(System.currentTimeMillis());
		}
		else {
			// Fixed random seed
			random = new Random(randomSeed);
		}
	}

	public ArrayList<UniprocessorServer> getShortestWeightPath(int srcId) {
		DataCenter dc = mExp.getDataCenter();
		ArrayList<UniprocessorServer> assignedServers = new ArrayList<UniprocessorServer>();
		ArrayList<UniprocessorServer> availServers = new ArrayList<UniprocessorServer>();
		ArrayList<UniprocessorServer> sleepingServers = new ArrayList<UniprocessorServer>();
		ArrayList<UniprocessorServer> AllavailableServers = new ArrayList<UniprocessorServer>();
		ArrayList<UniprocessorServer> Fullybusyserver = new ArrayList<UniprocessorServer>();

		Vector<Integer> availServersIndex = new Vector<Integer>();
//		dc.mTopo.Dijsktra(dc.mTopo.updateWeightMatrix(dc.mTopo.getNodeConnectivity(), dc.mTopo.getInitialWeightMatrix()), 20);
		if (srcId == -1) {	// parent task
			//DataCenter dc = mExp.getDataCenter();
			double weight = Double.MAX_VALUE;
			Vector<Vector<Integer>> route = new Vector<Vector<Integer>>();

			if(mExp.useGlobalQueue()) {
				availServers = getAvailableServers(sleepingServers,Fullybusyserver);

				AllavailableServers.addAll(availServers);
				AllavailableServers.addAll(sleepingServers);
				if(AllavailableServers.size() == 0) {
					return null;
				}

				for(Server server : AllavailableServers) {
					availServersIndex.add(server.getNodeId()+dc.getNumOfSwitches()-1);
				}
			}
			else {
				for(int i = 0; i < super.allServers.size(); i++) {
					UniprocessorServer server = super.allServers.get(i);

					// Server is available for task to be scheduled on if it has available capacity or has not reached server queue threshold
					// TODO: What if globalQueue is used?
					if (server.getRemainingCapacity() > 0 || server.getQueueLength() < mExp.getExpConfig().getServerQueueThreshold()) {
						availServers.add(server);
						availServersIndex.add(server.getNodeId()+dc.getNumOfSwitches()-1);
					}
				}
			}
			double[][] shortPath = null;
			//double[][] shortPath = new double[availServers.size()][availServers.size()];
			int src = 0, dst = 1;
			double min = Double.MAX_VALUE;
			for (int j = 0; j < availServers.size(); j++) {
				double[] result = dc.mTopo.Dijsktra(dc.mTopo.updateWeightMatrix_energy(dc.mTopo.getNodeConnectivity(),
						dc.mTopo.getInitialWeightMatrix()), availServers.get(j).getNodeId() + dc.getNumOfSwitches() - 1
						, availServersIndex);

				//shortPath[(int)result[0]][(int)result[1]] = result[2];
				if (result[0] < min) {
					min = result[0];
					src = (int)result[1];
					dst = (int)result[2];
				}
			}
			assignedServers.add(super.allServers.get(src - dc.getNumOfSwitches()));
			assignedServers.add(super.allServers.get(dst - dc.getNumOfSwitches()));
			return assignedServers;
		} else {	// child task, parent task has been scheduled

			int queue_used=0;
			if (super.allServers.get(srcId -1) instanceof ERoverServer) {
				queue_used = ((ERoverServer)super.allServers.get(srcId -1)).getWaitingTasksList().size();
			}

			if ((super.allServers.get(srcId -1).getRemainingCapacity()- queue_used) > 0) {
				assignedServers.add(super.allServers.get(srcId - 1));
				assignedServers.add(super.allServers.get(srcId - 1));
				return assignedServers;
			}


			availServers = getAvailableServers(sleepingServers,Fullybusyserver);

			AllavailableServers.addAll(availServers);
			AllavailableServers.addAll(sleepingServers);
			if(AllavailableServers.size() == 0) {
				return null;
			}

			for(Server server : AllavailableServers) {
				availServersIndex.add(server.getNodeId()+dc.getNumOfSwitches()-1);
			}

			int dst = 0;
//			do {
//				double[] result = dc.mTopo.Dijsktra(dc.mTopo.updateWeightMatrix(dc.mTopo.getNodeConnectivity(),
//						dc.mTopo.getInitialWeightMatrix()), srcId + dc.getNumOfSwitches() - 1);
//						//shortPath[(int)result[0]][(int)result[1]] = result[2];
//				dst = (int)result[2];
//			} while (dc.getServers().get(dst - dc.getNumOfSwitches()).getRemainingCapacity() <= 0);
			double[] result = dc.mTopo.Dijsktra(dc.mTopo.updateWeightMatrix_energy(dc.mTopo.getNodeConnectivity(),
					dc.mTopo.getInitialWeightMatrix()), srcId + dc.getNumOfSwitches() - 1, availServersIndex);
					//shortPath[(int)result[0]][(int)result[1]] = result[2];
			dst = (int)result[2];

			assignedServers.add(super.allServers.get(srcId - 1));
			assignedServers.add(super.allServers.get(dst - dc.getNumOfSwitches()));
			return assignedServers;
		}
	}

	/**
	 * Method is called when there are no available servers
	 */
	protected UniprocessorServer randomServer() {
		int index = indexGen.nextInt(allServers.size());
		return allServers.get(index);
	}


	protected UniprocessorServer PopcornsChooseParentServer(Task _task) {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		UniprocessorServer highUtiServer = null;

		Vector<UniprocessorServer> lowDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayIdleServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> lowDelayDeepSleepServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer> highDelayDeepSleepServers = new Vector<UniprocessorServer>();
		Vector<UniprocessorServer>  lowlowDelayIdleServers= new Vector<>();
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				int serverSleepState = aServer.getCurrentSleepState();
				ERoverSleepController ddController = (ERoverSleepController )aServer.getSleepController();

				//int currentState = aServer.getCurrentSleepState();
				//TODO: need to distinguish from hightao and lowtao server as well
				if(!aServer.isServerAllIdle() && serverSleepState == 0){
					if(highUtiServer == null || (highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity() &&
					 							  aServer.getRemainingCapacity() >=  _task.getaJob().getSize() ))
					highUtiServer = aServer;
				} else if (serverSleepState == 0 && aServer.getRemainingCapacity() > 0 ) {

					lowlowDelayIdleServers.add(aServer);
				}
				else if (serverSleepState == 1) {
					// idleServer = aServer;


						lowDelayIdleServers.add(aServer);

				} else if (serverSleepState == 4) {
					    highDelayIdleServers.add(aServer);
				}
				else if (serverSleepState== 5) {

						highDelayDeepSleepServers.add(aServer);

				}
				else{
					return null;
					//Sim.fatalError("fatal error: unexpected sleep state observerd in OnSleepScheduler " + serverSleepState);
				}
			}
		}
		if(highUtiServer != null){
			return highUtiServer;
		}

		UniprocessorServer pickedServer = null;
		int ldldIdleNum = lowlowDelayIdleServers.size();
		int hdIdleNum = highDelayIdleServers.size() ; // sleep 4
		int ldIdleNum = lowDelayIdleServers.size() ; // sleep 1
		int hdSleepNum = highDelayDeepSleepServers.size() ; // sleep 5
		int ldSleepNum = lowDelayDeepSleepServers.size() ;
		int index;
        //pick server to be scheduled in the following order
		if (ldldIdleNum > 0) {
			index = indexGen.nextInt(ldldIdleNum);
			pickedServer = lowlowDelayIdleServers.get(0);
		} else if (ldIdleNum > 0) {
			index = indexGen.nextInt(ldIdleNum);
			pickedServer = lowDelayIdleServers.get(0);
		} else if (hdIdleNum > 0) {
			index = indexGen.nextInt(hdIdleNum);
			pickedServer = highDelayIdleServers.get(0);
		} else if (hdSleepNum > 0) {
			index = indexGen.nextInt(hdSleepNum);
			pickedServer = highDelayDeepSleepServers.get(0);
		} else {
			return null;
			//Sim.fatalError("fatal error, not available servers to be found");
		}

		return pickedServer;


	}


	protected UniprocessorServer PickloadbalancingServer(Task task) {

		UniprocessorServer returnServer = allServers.get((int) (task.getTaskId() % allServers.size()));
		return returnServer;
	}


	protected UniprocessorServer PicktrueRandomServer(Task _task) {

		Vector<Task> parentTasks = _task.getaJob().getParentTasks(_task);
		Vector<Task> childTasks = _task.getaJob().getChildTasks(_task);
		Vector<Task> dependentTasks = new Vector<Task>();
		if(parentTasks != null) {
			dependentTasks.addAll(parentTasks);
		}
		if(childTasks != null) {
			dependentTasks.addAll(childTasks);
		}
        int count = 0;
        boolean retry=false;
		while (count < 50) {
			UniprocessorServer returnServer = allServers.get((random.nextInt(allServers.size())));
			for (Task t : dependentTasks) {
				if (t.getServer()!= null && returnServer.getNodeId() == t.getServer().getNodeId()) {
					retry=true;
					break;
				}
			}

			if (!retry) {
				return returnServer;
			}
			retry=false;
			count++;

		}
		return null;
	}

	protected UniprocessorServer PicktrueRandomEnabledServer(Task _task) {

		Vector<Task> parentTasks = _task.getaJob().getParentTasks(_task);
		Vector<Task> childTasks = _task.getaJob().getChildTasks(_task);
		Vector<Task> dependentTasks = new Vector<Task>();
		if(parentTasks != null) {
			dependentTasks.addAll(parentTasks);
		}
		if(childTasks != null) {
			dependentTasks.addAll(childTasks);
		}
		int count = 0;
		boolean retry=false;
		while (count < 50) {
			UniprocessorServer returnServer = activeServers.get((random.nextInt(activeServers.size())));
			for (Task t : dependentTasks) {
				if (t.getServer()!= null && returnServer.getNodeId() == t.getServer().getNodeId()) {
					retry=true;
					break;
				}
			}

			if (!retry) {
				return returnServer;
			}
			retry=false;
			count++;

		}
		return null;
	}


	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// Using global queue
		Sim.debug(5,"Arrival for task for scheduling:" + _task.getTaskId());
		if (mExperiment.useGlobalQueue()) {
			// Only one available server to schedule
			ArrayList<UniprocessorServer> sleepingServers = new ArrayList<UniprocessorServer>();
			ArrayList<UniprocessorServer> AllavailableServers = new ArrayList<UniprocessorServer>();
			ArrayList<UniprocessorServer> FullybusyServers = new ArrayList<UniprocessorServer>();
			ArrayList<UniprocessorServer> availableServers = getAvailableServers(sleepingServers,FullybusyServers);
			AllavailableServers.addAll(availableServers);
			AllavailableServers.addAll(sleepingServers);
//			if(AllavailableServers.size() == 1) {
//				UniprocessorServer returnServer = AllavailableServers.get(0);
//
//				if(returnServer instanceof ERoverServer) {
//					ERoverServer wakeServer = (ERoverServer)returnServer;
//
//					if(returnServer.getCurrentSleepState() == 4 || returnServer.getCurrentSleepState() == 5) {
//						if(!wakeServer.isTransitionToActive()) {
//							wakeUpServer(wakeServer, time);
//							Sim.debug(5,"^^^^ -- ScheduleTask waking up a first time sleeping server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
//
//						}
//						Sim.debug(5,"^^^^ -- ScheduleTask waking up a sleeping server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
//						wakeServer.addToWaitingTaskList(_task);
//						return null;
//					}
//				}
//
//				System.out.println("Only 1 server available to schedule task " + _task.getTaskId() + " on server " + returnServer.getNodeId());
//
//				prepareServerForScheduling(returnServer, _task, time);
//				return returnServer;
//			}

			// Get current task's dependent tasks
			Vector<Task> parentTasks = _task.getaJob().getParentTasks(_task);
			Vector<Task> childTasks = _task.getaJob().getChildTasks(_task);
			Vector<Task> dependentTasks = new Vector<Task>();
			if(parentTasks != null) {
				dependentTasks.addAll(parentTasks);
			}
			if(childTasks != null) {
				dependentTasks.addAll(childTasks);
			}

			// Sort tasks by task id
			Comparator<Task> taskID = new Comparator<Task>() {
				@Override
				public int compare(Task task1, Task task2) {
					if(task1.getTaskId() < task2.getTaskId()) {
						return -1;
					}
					else {
						return 1;
					}
				}
			};
			Collections.sort(dependentTasks, taskID);

			// Tasks are scheduled in ascending task id order
			for(int i = dependentTasks.size() - 1; i >= 0; i--) {
				// Dependent task has already been scheduled. Schedule current task using shortest weight path algorithm
				Task dependentTask = dependentTasks.get(i);
				if(_task.getTaskId() > dependentTask.getTaskId()) {
					Server dependentTaskServer = dependentTask.getServer();
					if(dependentTaskServer != null) {

						UniprocessorServer returnServer;

						if (mExp.getExpConfig().getNetworkRoutingAlgorithm() == NetworkRoutingAlgorithm.POPCORNS) {
							ArrayList<UniprocessorServer> shortestWeightPath = getShortestWeightPath(dependentTaskServer.getNodeId());
							if(shortestWeightPath == null) {
								// all servers busy.
								//returnServer = PicktrueRandomServer(_task);
								returnServer = PickloadbalancingServer(_task);
								System.out.println("&&&&&&&&&&&&&&&&\n&&&&&&&&&&&&&&&&&&&& Popcorns server not found \n&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&\n\n\n\n&&&&&&&&&&&&&&&&&&\n");
							} else {
								//Shortest distance server is last server returned by getShortestWeighPath method
								//System.out.println("Using shortestWeightPath algorithm to schedule task " + _task.getTaskId() + " on server " + returnServer.getNodeId());
								returnServer = shortestWeightPath.get(shortestWeightPath.size() - 1);
								//returnServer = allServers.get(5);
							}
						} else if (mExp.getExpConfig().getNetworkRoutingAlgorithm() == NetworkRoutingAlgorithm.WASP) {
						    //TODO choose a random server only among  list of enabled servers.
							returnServer = PicktrueRandomEnabledServer (_task);
						} else {

							returnServer = PicktrueRandomServer(_task);
						}




							if(returnServer instanceof ERoverServer) {
								ERoverServer wakeServer = (ERoverServer)returnServer;
									if(returnServer.getCurrentSleepState() == 4 || returnServer.getCurrentSleepState() == 5) {
									if(!wakeServer.isTransitionToActive()) {
										wakeUpServer(wakeServer, time);
										Sim.debug(5,"^^^^ -- ScheduleTask waking up a first time sleeping 2 server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());

									}
									Sim.debug(5,"^^^^ -- ScheduleTask waking up a sleeping 2 server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
									wakeServer.addToWaitingTaskList(_task);
									prepareServerForScheduling(returnServer, _task, time);

									return null;
								}
							}


							if(returnServer instanceof ERoverServer) {
								ERoverServer busyserver = (ERoverServer)returnServer;
									if (busyserver.isServerAllBusy()) {
									Sim.debug(1,"^^^^ --2 ScheduleTask busy server queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
									busyserver.addToWaitingTaskList(_task);
										prepareServerForScheduling(returnServer, _task, time);

									return null;
								}
							}

						prepareServerForScheduling(returnServer, _task, time);

							return returnServer;

					}
				}
			}
			// Choose a active server instead.
			Server returnServer;
			if (mExp.getExpConfig().getNetworkRoutingAlgorithm() == NetworkRoutingAlgorithm.POPCORNS) {
				returnServer = PopcornsChooseParentServer(_task);
				//returnServer = allServers.get(120);
			} else if (mExp.getExpConfig().getNetworkRoutingAlgorithm() == NetworkRoutingAlgorithm.WASP)  {
				//Server returnServer = PickloadbalancingServer(_task);
				returnServer = PicktrueRandomEnabledServer(_task);
				//returnServer = allServers.get(10);
			} else {
				returnServer = PicktrueRandomServer(_task);

			}
			// No dependent task, dependent task is not scheduled, or dependent task is in queue. Schedule current task using regular task scheduling policy





		    //returnServer = allServers.get(2);
			if(returnServer != null) {
				prepareServerForScheduling((ERoverServer)returnServer, _task, time);

				if(returnServer instanceof ERoverServer) {
					ERoverServer wakeServer = (ERoverServer)returnServer;
					if(wakeServer.getCurrentSleepState() == 4 || wakeServer.getCurrentSleepState() == 5) {
						if(!wakeServer.isTransitionToActive()) {
							wakeUpServer(wakeServer, time);
							Sim.debug(5,"^^^^ -- ScheduleTask waking up a first time sleeping 3 server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
						}
						wakeServer.addToWaitingTaskList(_task);
						Sim.debug(5,"^^^^ -- ScheduleTask waking up a sleeping 3 server and queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());

						return null;
					}

					 if(returnServer instanceof ERoverServer) {
							ERoverServer busyserver = (ERoverServer)returnServer;
								if (busyserver.isServerAllBusy()) {
								Sim.debug(1,"^^^^ -- ScheduleTask busy server queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
								busyserver.addToWaitingTaskList(_task);

								return null;
							}
						}
				}



				Sim.debug(5,"Using regular algorithm to schedule task " + _task.getTaskId() + " on server " + returnServer.getNodeId());
			} else {
				   returnServer = PickloadbalancingServer(_task);
				   prepareServerForScheduling((ERoverServer)returnServer, _task, time);
				   if(returnServer instanceof ERoverServer) {
						ERoverServer busyserver = (ERoverServer)returnServer;
							if (busyserver.isServerAllBusy()) {
							Sim.debug(1,"^^^^ -- ScheduleTask busy server queuingtakk : " + returnServer.getNodeId()  + " going to be woken up "+ mExperiment.printStateofServers());
							busyserver.addToWaitingTaskList(_task);

							return null;
						}
					}
			}

			return returnServer;
		}

		// Using each server's local queue
		else {
			UniprocessorServer server;

			// No available servers. Add task to a server's local queue
			if(allServersBusy()) {
				server = randomServer();
				_task.setServer(server);
				server.insertTask(time, _task);
				System.out.println("All servers busy");
				return null;
			}

			server = findAvailableServer();

			Sim.debug(3, "||| task " + _task.getTaskId()
					+ " picked server : " + server.getNodeId()
					+ " in sleep state " + server.getCurrentSleepState());

			System.out.println("Scheduled task " + _task.getTaskId() + " on server " + server.getNodeId());
			_task.setServer(server);

			return server;
		}
	}



	/**
	 * Return line card associated with specified port
	 */
	public LineCard getLineCard(LCSwitch _switch, int portId) {
		int portsPerLineCard = mExp.getExpConfig().getPortsPerLineCard();
		int lineCardId = (portId - 1) / portsPerLineCard + 1;

		Vector<LineCard> lineCards = _switch.getLineCards();
		return lineCards.get(lineCardId - 1);
	}

	public void updateSwitchThresholds(Port port, Port connectedPort, int connectedPortID, DCNode node, DCNode connectedNode, double time) {
		int portsPerLineCard = mExp.getExpConfig().getPortsPerLineCard();

		int linecardId = (node.getIdByPort(port) - 1) / portsPerLineCard + 1;

		// Wait time before ports/linecards go to sleep
		double LPItao = mExp.getDataCenter().getExpConfig().getLcWaitTime(0);

		// Port enters LPI after certain amount of time. If all ports are in LPI mode, linecard goes to sleep
		if(port.getPowerPolicy() == PortPowerPolicy.Port_LPI) {
			if(port.getInPortState() == PortPowerState.idle && port.getOutPortState() == PortPowerState.idle) {
				if(connectedPort.getInPortState() == PortPowerState.idle && connectedPort.getOutPortState() == PortPowerState.idle) {
					PortTransitiontoLPIEvent ttp1LPIEvent = new PortTransitiontoLPIEvent(
							time+LPItao, mExp,
							node, node.getIdByPort(port), linecardId);
					mExp.addEvent(ttp1LPIEvent);
					port.setNextLPIevent(ttp1LPIEvent);
					int connectedLcId = (connectedPortID - 1) / portsPerLineCard + 1;
					PortTransitiontoLPIEvent ttp2LPIEvent = new PortTransitiontoLPIEvent(
							time+LPItao, mExp,
							connectedNode, connectedPortID, connectedLcId);
					mExp.addEvent(ttp2LPIEvent);
					connectedNode.getPortById(connectedPortID).setNextLPIevent(ttp2LPIEvent);
				}
			}
		}
		// Ports are always active. Linecard goes to sleep after certain amount of time
		else if(port.getPowerPolicy() == PortPowerPolicy.NO_MANAGEMENT && node.getLinecardById(linecardId).getPowerPolicy() == LineCardPowerPolicy.LINECARD_SLEEP &&
		node.getLinecardById(linecardId).getNextLinecardSleepEvent() == null) {


			if (node.getLinecardById(linecardId).getSleepController().getCurrentSleepState() == 0) {
				LCSwitch lcSwitch = (LCSwitch)node;
				LineCard linecard = node.getLinecardById(linecardId);
				boolean isLineCardBusy = false;

				int i;
				for (i = (linecardId-1)*mExp.getExpConfig().getPortsPerLineCard()+1; i < (linecardId)*mExp.getExpConfig().getPortsPerLineCard(); i++) {
					Port portwithin = node.getPortById(i);
					if (portwithin.getInPortState() == PortPowerState.idle || portwithin.getOutPortState()==PortPowerState.idle) {
						continue;
					} else {
						break;
					}
				}
				if (i >= (linecardId)* mExp.getExpConfig().getPortsPerLineCard()) {
					linecard.setNextLinecardSleepEvent(null);
					//lcSwitch.unsetScheduledActive();
					SwitchSleepController switchSleepController = linecard.getSleepController();

					if(switchSleepController.getTransitionEvent() == null) {
						LineCardTransitiontoSleepEvent lineCardTransitionToSleepEvent = lcSwitch.generateLineCardSleepEvent(time, mExp, node, linecardId, switchSleepController);
						mExp.addEvent(lineCardTransitionToSleepEvent);
						switchSleepController.setTransitionEvent(lineCardTransitionToSleepEvent);
					}
				}

			}
		}
	}
	
	public boolean wakeServerForJobQoS(double time) {
		if (lowPowerServers.size() > 0) {
			this.updateSchServerHis(time, 1);

			ERoverServer server = (ERoverServer) findServerToWakeup();
			activeServers.add(server);
			lowPowerServers.remove(server);

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
				Sim.fatalError("attempt to set idlerecord duration twice");
			}
			record.sleepstate = gsController.getCurrentSleepState();
			
			if(server.getNodeId() == 1) {
				System.out.println("Setting record duration for server 1");
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

//			/**********************************************************************/
//
//			/* mark the server as scheduled active */
//			server.setScheduledActive();
//			int initialState = server.getInitialSleepState();
//			server.setNextFixedSleepState(initialState);

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
			// Job QoS will be violated but no servers to put to wake up. Next job in queue
			return false;
		}
	}
	
	public ArrayList<UniprocessorServer> getAvailableServers(ArrayList<UniprocessorServer> sleepingServers, ArrayList<UniprocessorServer> fullybusyServers) {
		UniprocessorServer highUtiServer = null;
		ArrayList<UniprocessorServer> availableServers = new ArrayList<UniprocessorServer>();
		

		for (UniprocessorServer aServer : activeServers) {
			int serverSleepState = aServer.getCurrentSleepState();
			if (aServer.isServerAllBusy()) {
				fullybusyServers.add(aServer);
			} else {
				
				if(!aServer.isServerAllIdle()){
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
				
			}
			
			if (aServer.getCurrentSleepState() == 4 || aServer.getCurrentSleepState() == 5) {
					 sleepingServers.add(aServer);
					//Sim.fatalError("fatal error: unexpected sleep state observerd in EroverScheduler " + serverSleepState);
				}
			}
		
		
		if(highUtiServer != null){
			availableServers.add(highUtiServer);
		}
		
		return availableServers;
	}
	
	public void prepareServerForScheduling(UniprocessorServer server, Task _task, double time) {
		// Cancel pending server sleep event

//		if (server == null) {
//			return;
//		}
		Sim.debug(5,"Job "+ _task.getJobId()+ " " + _task.getTaskId() + " scheduled " + mExperiment.printStateofServers());
		if(server.getSleepController().getCurrentSleepState() != server.getSleepController().getDeepestSleepState()) {
			StartSleepEvent startSleepEvent = server.getSleepController().getNextSleepEvent();
			if(startSleepEvent != null) {
				mExperiment.cancelEvent(startSleepEvent);
			}
		}

		server.getSleepController().cancelAllFutureSleepEvents(mExperiment);
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
	}
	
	public void wakeUpServer(ERoverServer server, double time) {
		server.setScheduledActive();
		
		if (server.getSleepController().getCurrentSleepState() == 0 || server.getSleepController().getCurrentSleepState() == 1 || 
				server.isTransitionToActive()) {	
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
			return;
		}
//
		// otherwise, server is in deep sleep state
		Sim.debug(3,
				"ooo Time: " + time + " Server : " + server.getNodeId()
				+ " would be waked up"
				+ " now active servers: "
				+ activeServers.size());

//		/**********************************************************************/
//		// update idle record
//		// Core singleCore = server.getCore();
////		AbstractSleepController gsController = server.getSleepController();
////		Vector<IdleRecord> idleDistribution = gsController.getIdleDistribution();
////		if (idleDistribution.size() == 0) {
////			IdleRecord firstRecord = new IdleRecord();
////			firstRecord.startIdleTime = 0.0;
////			firstRecord.sleepstate = gsController
////					.getCurrentSleepState();
////			idleDistribution.add(firstRecord);
////		}
//		IdleRecord record = idleDistribution.lastElement();
//		if (record.duration != IdleRecord.INITIAL) {
//			Sim.fatalError("attempt to set idlerecord duration twice");
//		}
//		record.sleepstate = gsController.getCurrentSleepState();
		
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
		int initialState = server.getInitialSleepState();
		server.setNextFixedSleepState(initialState);

		/**********************************************************************/
		// generate transition event
		double transitionTime = server.getTansitionToActiveTime();
		MSTransToActiveEvent event = new MSTransToActiveEvent(time
				+ transitionTime, (ShallowDeepExperiment) mExperiment,
				server, server.getCurrentSleepState());
		//fanyao added: accumulate wakeup time for specific event
		
		server.setToActiveEvent(event);
		mExperiment.addEvent(event);
		
//		/**********************************************************************/
//		/*
//		 * update current powerstate after getCurrentSleepState()
//		 */
		// server.setPowerState(Core.PowerState.TRANSITIONING_TO_ACTIVE);
		server.setTransitionToActive();
	}
}
