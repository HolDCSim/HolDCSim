package loadpredictor;

import java.util.Vector;

import debug.Sim;

import scheduler.GlobalQueueScheduler;
import utility.Pair;

public class AllAveragePredictor extends AbstractLoadPredictor {

	//private Vector<Pair<Double, Integer>> queueHis;
	private Vector<Pair<Double, Integer>> activeServerHis;

	private Double combinedQueueHis;
	//private Double combinedServerNumHis;
	// private Double lastUpdateTime;

	private Double curTime;
	
	@Override
	public double predictCurWorkload(double time, int currentQueueSize) {
		// TODO Auto-generated method stub

		combinedQueueHis += updateIntegral(time, queueHis);
	//	combinedServerNumHis += updateIntegral(time, activeServerHis);
		curTime = time;

	//	return combinedQueueHis / combinedServerNumHis;
		double predictedWd = combinedQueueHis/(time) / activeServerHis.lastElement().getSecond();
		
		addPrediction(time, predictedWd);
		return predictedWd;
	}

	private Double updateIntegral(double time, Vector<Pair<Double, Integer>> his) {
		// TODO Auto-generated method stub
		int size = his.size();
		double delta = 0.0;
		double height = 0.0;
		double startPoint = his.lastElement().getFirst();
		if (time == startPoint) {
			if (size <= 1) {
				Sim.fatalError("history size not enough");
			}
			height = his.get(size - 2).getSecond();
		}

		else {
			height = his.get(size - 1).getSecond();
		}
				
		delta = (time - curTime) * height;
		return delta;

	}

	public AllAveragePredictor(GlobalQueueScheduler taskScheduler, Vector<Pair<Double, Integer>> theQueueHis,
			Vector<Pair<Double, Integer>> theServerHis, boolean dumpPrediction) {
		super(taskScheduler, theQueueHis, theServerHis, dumpPrediction);
		//this.queueHis = theQueueHis;
		this.activeServerHis = theServerHis;
		this.combinedQueueHis = 0.0;
//		this.combinedServerNumHis = 0.0;
		this.curTime = 0.0;
	}


	/**
	 * find the index of
	 * 
	 * @return
	 */
	// protected Double updateIntegralServerHis(double currentTime,
	// Vector<Pair<Double, Integer>> history) {
	// Double delta = 0.0;
	// Integer stopIndex = -1;
	// for (Integer i = 0; i < history.size(); i++) {
	// Pair<Double, Integer> aPair = history.get(i);
	// if (currentTime > aPair.getFirst() && stopIndex == -1) {
	// stopIndex = i;
	// }
	//
	// }
	//
	// for (Integer j = preIndexInServerHis; j < stopIndex; j++) {
	// Integer currentLevel = history.get(j).getSecond();
	// if (j == preIndexInServerHis) {
	// delta += currentLevel
	// * (history.get(j + 1).getFirst() - prevTime);
	// } else {
	// delta += currentLevel
	// * (history.get(j + 1).getFirst() - history.get(j)
	// .getFirst());
	// }
	//
	// }
	//
	// preIndexInServerHis = stopIndex;
	// delta += (currentTime - history.lastElement().getFirst())
	// * history.lastElement().getSecond();
	//
	// return delta;
	//
	// }
	//
	// protected Double updateIntegralQueueHis(double currentTime,
	// Vector<Pair<Double, Integer>> history) {
	// Double delta = 0.0;
	// Integer stopIndex = -1;
	// for (Integer i = 0; i < history.size(); i++) {
	// Pair<Double, Integer> aPair = history.get(i);
	// if (currentTime > aPair.getFirst() && stopIndex == -1) {
	// stopIndex = i;
	// }
	//
	// }
	//
	// for (Integer j = preIndexInQueueHis; j < stopIndex; j++) {
	// Integer currentLevel = history.get(j).getSecond();
	// if (j == preIndexInQueueHis) {
	// delta += currentLevel
	// * (history.get(j + 1).getFirst() - prevTime);
	// } else {
	// delta += currentLevel
	// * (history.get(j + 1).getFirst() - history.get(j)
	// .getFirst());
	// }
	//
	// }
	//
	// preIndexInQueueHis = stopIndex;
	// delta += (currentTime - history.lastElement().getFirst())
	// * history.lastElement().getSecond();
	//
	// return delta;
	//
	// }

}
