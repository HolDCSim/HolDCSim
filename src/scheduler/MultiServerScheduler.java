package scheduler;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Vector;

import debug.Sim;

import queue.BaseQueue;
import utility.Pair;
import infrastructure.Server;
import infrastructure.UniprocessorServer;
import job.Task;
import loadpredictor.AbstractLoadPredictor;
import experiment.ExperimentConfig.TaskSchedulerPolicy;
import experiment.MultiServerExperiment;

public class MultiServerScheduler extends GlobalQueueScheduler {

	public enum SchedulerPolicy {
		ROUND_ROBIN, RANDOM, FEEDBACK
	}

	protected int numOfSevers;

	protected Random indexGen;

	protected TaskSchedulerPolicy mTaskSchPolicy;

	// private Random serverNoGen;

	protected SchedulerPolicy schPolicy = SchedulerPolicy.ROUND_ROBIN;

	protected Vector<UniprocessorServer> allServers;

	// multiserver experiment uses FakeQueuePredictor
	protected AbstractLoadPredictor mPredictor;

	public MultiServerScheduler(int numOfServers, SchedulerPolicy policy, MultiServerExperiment experiment) {
		super(experiment);
		// TODO Auto-generated constructor stub
		if (numOfServers > experiment.getDataCenter().getServers().size()) {
			Sim.fatalError("there are not enough servers to be scheduled");
		}
		this.numOfSevers = numOfServers;
		this.schPolicy = policy;

		// fanyao added: task scheduler policy
		mTaskSchPolicy = mExperiment.getExpConfig().getTaskSchePolicy();

		// random seed or fixed seed
		if (Sim.RANDOM_SEEDING) {
			this.indexGen = new Random(System.currentTimeMillis());
		} else {
			this.indexGen = new Random(10000);
		}

		allServers = new Vector<UniprocessorServer>();
		Vector<Server> dataCenterServers = experiment.getDataCenter().getServers();

		// all the scheduled servers are set to be active
		for (int i = 0; i < experiment.getExpConfig().getServersToSchedule(); i++) {

			UniprocessorServer sServer = (UniprocessorServer) dataCenterServers.get(i);
			allServers.add(sServer);
		}
		// queue = new BaseQueue(mExperiment.getExpConfig().dumpQueueHis());
		queue = createQueue();
	}

	public MultiServerScheduler(MultiServerExperiment multiServerExperiment) {
		// TODO Auto-generated constructor stub
		this(multiServerExperiment.getExpConfig().getServersToSchedule(), SchedulerPolicy.RANDOM,
				multiServerExperiment);
	}

	public Vector<UniprocessorServer> getControlledServers() {
		return allServers;
	}

	protected boolean allServersBusy() {
		boolean allBusy = true;
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				allBusy = false;
				break;
			}
		}

		return allBusy;
	}

	// protected SingleCoreServer findFirstAvailableServer() {
	// // TODO Auto-generated method stub
	// SingleCoreServer server = null;
	// // FIXME: shuffle method may be time-consuming
	// // Collections.shuffle(allServers);
	// for (SingleCoreServer aServer : allServers) {
	// if (!aServer.isServerBusy()) {
	// server = aServer;
	// break;
	// }
	// }
	//
	// return server;
	//
	// }

	/**
	 * find a server that could be used to schedule a job
	 * 
	 * @return
	 */
	protected UniprocessorServer findAvailableServer() {
		UniprocessorServer server = null;
		if (mTaskSchPolicy == TaskSchedulerPolicy.FIRST_PICK) {
			server = firstAvailServer();

		} else if (mTaskSchPolicy == TaskSchedulerPolicy.RANDOM_AVAIL) {
			server = randomAvailServer();
		}

		return server;
		// return PopcornsChooseParentServer();
		// changed to firstAvailableServer to condense active servers
	}

	/**
	 * find a random server in the vector of available servers
	 * 
	 * @return
	 */
	protected UniprocessorServer randomAvailServer() {
		// always first pick the server in idle state

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		Vector<UniprocessorServer> allIdleServers = new Vector<UniprocessorServer>();
		UniprocessorServer highUtiServer = null;
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				if (aServer.isServerAllIdle())
					allIdleServers.add(aServer);
				else {

					// TODO: this is one strategy that always saturates the busiest server
					if (highUtiServer == null
							|| highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()) {
						highUtiServer = aServer;
					}
				}
			}
		}

		// step one: try to pick server with an available slot that has highest
		// utilization
		if (highUtiServer != null) {
			return highUtiServer;
		}

		// otherwise, randomly pick one server from allIdle servers
		int numOfIdleServers = allIdleServers.size();

		if (numOfIdleServers == 0) {
			Sim.fatalError("could not find avaiable server");
			return null;
		} else {
			// TODO: parameterize seed
			int index = indexGen.nextInt(numOfIdleServers);
			return allIdleServers.get(index);
		}

	}

	/**
	 * find first available server to sleep
	 * 
	 * @return
	 */
	protected UniprocessorServer firstAvailServer() {
		// TODO Auto-generated method stub
		UniprocessorServer server = null;
		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
//		for (UniprocessorServer aServer : allServers) {
//			if (!aServer.isServerBusy()) {
//				server = aServer;
//				break;
//			}
//		}
//
//		return server;

		// FIXME: shuffle method may be time-consuming
		// Collections.shuffle(allServers);
		// Vector<UniprocessorServer> allIdleServers = new Vector<UniprocessorServer>();
		UniprocessorServer firstAllIdleServer = null;
		UniprocessorServer highUtiServer = null;
		for (UniprocessorServer aServer : allServers) {
			if (!aServer.isServerAllBusy()) {
				if (aServer.isServerAllIdle()) {
					if (firstAllIdleServer == null)
						firstAllIdleServer = aServer;
				} else {

					// TODO: this is one strategy that always saturates the busiest server
					if (highUtiServer == null
							|| highUtiServer.getRemainingCapacity() > aServer.getRemainingCapacity()) {
						highUtiServer = aServer;
					}
				}
			}
		}

		if (highUtiServer != null) {
			return highUtiServer;
		}
		if (firstAllIdleServer != null) {
			return firstAllIdleServer;
		}

		Sim.fatalError("could not find available server in firstAvailServer");
		return null;
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		if (schPolicy == SchedulerPolicy.RANDOM) {

			// updateThresholds(time);

			if (allServersBusy()) {
				queue.add(_task, time);
				return null;
			}

			else {
				UniprocessorServer server;
				server = findAvailableServer();

				Sim.debug(3, "||| task " + _task.getTaskId() + " picked server : " + server.getNodeId()
						+ " in sleep state " + server.getCurrentSleepState());
				// activeServers.remove(server);
				// Task oneTask = queue.poll();
				// updateThresholds(time);
				_task.setServer(server);

				// if (queue.size() < Ws) {
				// if (activeServers.size() != 0) {
				// server = activeServers.remove(0);
				// lowPowerServers.add(server);
				// server.setFixedSleepState(deepState);
				// }
				// }
				return server;
			}
		} else {
			Sim.fatalError("fatal error: currently only support random scheduling among available servers");
			return null;
		}
	}

	public Vector<Pair<Double, Integer>> getQueueHis() {
		return queue.getGlobalQueueHis();
	}

	public void setQueuePredictor(AbstractLoadPredictor aPredictor) {
		if (aPredictor == null) {
			Sim.fatalError("queuepredictor is null");
		}
		this.mPredictor = aPredictor;
	}

	public void dumpQueuingHis() {
		// mPredictor.dumpPredictionHis();
		queue.dumpQueueHis();
	}

	@Override
	protected BaseQueue createQueue() {
		// TODO Auto-generated method stub
		return new BaseQueue(mExperiment.getExpConfig().doCollectQueueHis());
	}

}
