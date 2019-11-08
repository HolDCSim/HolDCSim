package loadpredictor;

import java.util.Vector;

import queue.BaseQueue;
import debug.Sim;
import experiment.Experiment;
import experiment.SingletonJobExperiment;
import scheduler.GlobalQueueScheduler;
import utility.Pair;

public class MixedInstantPredictor extends InstantPredictor {

	SingletonJobExperiment theExp;
	BaseQueue theQueue;
	public MixedInstantPredictor(GlobalQueueScheduler taskScheduler,
			Vector<Pair<Double, Integer>> theQueueHis,
			Vector<Pair<Double, Integer>> theServerHis, boolean dumpPrediction, Experiment experiment) {
		super(taskScheduler, theQueueHis, theServerHis, dumpPrediction);
		this.theExp = (SingletonJobExperiment)experiment;
		this.theQueue = taskScheduler.getGlobalQueue();
		// TODO Auto-generated constructor stub
		
		
	}
	
	@Override
	public double predictCurWorkload(double time, int currentQueueSize) {
		// TODO Auto-generated method stub
		if (queueHis == null || activeServerHis == null) {
			Sim.fatalError("queuehis or activeServerHis are not initialized");
		}
		int activeServers = activeServerHis.lastElement().getSecond();
		//int jobsInQueue = queueHis.lastElement().getSecond();
		
		
		double averageFinishedJobSize = theExp.getAvgFinishedJobSize();
		double totalPendingExecTime = theQueue.getTotalPendingTime();
		
		
		double predictedWd;
		if(totalPendingExecTime == 0.0 || averageFinishedJobSize == 0.0)
			return 0.0;

		predictedWd = (totalPendingExecTime/ averageFinishedJobSize) / (double) activeServers;
		addPrediction(time, predictedWd);
		return predictedWd;
	}


}
