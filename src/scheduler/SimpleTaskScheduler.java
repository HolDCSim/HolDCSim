package scheduler;

import job.Task;
import loadpredictor.AbstractLoadPredictor;
import queue.BaseQueue;
import queue.MSGlobalQueue;
import queue.PriorityTaskQueue;
import scheduler.MultiServerScheduler.SchedulerPolicy;
import infrastructure.AbstractSleepController;
import infrastructure.DCNode;
import infrastructure.DataCenter;
import infrastructure.ERoverServer;
import infrastructure.ERoverStates;
import infrastructure.LineCard;
import infrastructure.Port;
import infrastructure.Server;
import infrastructure.ShallowDeepServer;
import infrastructure.UniprocessorServer;
import infrastructure.Server.serverState;
import experiment.ERoverExperiment;
import experiment.Experiment;
import experiment.ExperimentConfig;
import experiment.MultiServerExperiment;
import experiment.ShallowDeepExperiment;
import experiment.ExperimentConfig.TaskSchedulerPolicy;
import experiment.SingletonJobExperiment.IdleRecord;
import experiment.SingletonJobExperiment.JobWorkload;
import experiment.LinecardNetworkExperiment;
import topology.Topology;
import utility.AsynLoggedVector;
import utility.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import constants.Constants;
import debug.Sim;
import event.MSTransToActiveEvent;
import event.StartSleepEvent;

public class SimpleTaskScheduler implements ITaskScheduler {

	private Experiment mExp;
	private boolean isScheduleTimeout;	// if timeout, can be scheduled, otherwise use parent server or child server
	private Server parentServer;
	private Server childServer;
	
	public SimpleTaskScheduler(Experiment experiment) {
		//super(null);
		this.mExp = experiment;
		// TODO Auto-generated constructor stub
		this.isScheduleTimeout = true;
	}

	@Override
	public Server scheduleTask(Task _task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		DataCenter dc = mExp.getDataCenter();
		int taskId = (int) _task.getTaskId();
		int serverId = taskId % dc.getServers().size();
		Server server;
		if (serverId == 0)
			serverId = dc.getServers().size();
		// Iterator iter = this.dataCenter.entrySet().iterator();
		// while(iter.hasNext()){
		// Map.Entry pair = (Map.Entry)iter.next();
		// System.out.println((Integer)pair.getKey() + "\n");
		// if((Integer)pair.getKey() == 2)
		// server =(Server) pair.getValue();
		// }
		// long to int bug
		server = dc.getServers().get(serverId - 1);
		_task.setServer(server);
		return server;
	}		
	
	public ArrayList<Server> getShortestWeightPath(int srcId) {
		DataCenter dc = mExp.getDataCenter();
		ArrayList<Server> assignedServers = new ArrayList<Server>();
		ArrayList<Server> availServers = new ArrayList<Server>();
		Vector<Integer> availServersIndex = new Vector<Integer>();
//		dc.mTopo.Dijsktra(dc.mTopo.updateWeightMatrix(dc.mTopo.getNodeConnectivity(), dc.mTopo.getInitialWeightMatrix()), 20);
		if (srcId == -1) {	// parent task
			//DataCenter dc = mExp.getDataCenter();
			double weight = Double.MAX_VALUE;
			Vector<Vector<Integer>> route = new Vector<Vector<Integer>>();
			
			for (int i = 0; i < dc.getServers().size(); i++) {
				Server server = dc.getServers().get(i);
				if (server.getRemainingCapacity() > 0) {
					availServers.add(server);
					availServersIndex.add(server.getNodeId()+dc.getNumOfSwitches()-1);
				}
			}
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
			assignedServers.add(dc.getServers().get(src - dc.getNumOfSwitches()));
			assignedServers.add(dc.getServers().get(src - dc.getNumOfSwitches()));
			return assignedServers;	
		} else {	// child task, parent task has been scheduled
			//DataCenter dc = mExp.getDataCenter();
			for (int i = 0; i < dc.getServers().size(); i++) {
				Server server = dc.getServers().get(i);
				if (server.getRemainingCapacity() > 0) {
					availServers.add(server);
					availServersIndex.add(server.getNodeId()+dc.getNumOfSwitches()-1);
				}
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
			assignedServers.add(dc.getServers().get(srcId - 1));
			assignedServers.add(dc.getServers().get(dst - dc.getNumOfSwitches()));
			return assignedServers;	
		}
	}
}