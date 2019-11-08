package loadpredictor;

import java.util.Vector;

import debug.Sim;

import scheduler.GlobalQueueScheduler;
import utility.Pair;

public class InstantPredictor extends AbstractLoadPredictor {
	//private Vector<Pair<Double, Integer>> queueHis;
	protected Vector<Pair<Double, Integer>> activeServerHis;

	public InstantPredictor(GlobalQueueScheduler taskScheduler, Vector<Pair<Double, Integer>> theQueueHis,
			Vector<Pair<Double, Integer>> theServerHis, boolean dumpPrediction) {
		super(taskScheduler, theQueueHis, theServerHis, dumpPrediction);
		this.queueHis = theQueueHis;
		this.activeServerHis = theServerHis;
	}

	@Override
	public double predictCurWorkload(double time, int currentQueueSize) {
		// TODO Auto-generated method stub
		if (queueHis == null || activeServerHis == null) {
			Sim.fatalError("queuehis or activeServerHis are not initialized");
		}
		int activeServers = activeServerHis.lastElement().getSecond();
		//int jobsInQueue = queueHis.lastElement().getSecond();
		
		
		
		double predictedWd = currentQueueSize / (double) activeServers;
		
		addPrediction(time, predictedWd);
		return predictedWd;
	}


}
