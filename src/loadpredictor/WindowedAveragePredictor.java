package loadpredictor;

import java.util.Vector;

import debug.Sim;

import scheduler.GlobalQueueScheduler;
import utility.Pair;

public class WindowedAveragePredictor extends AbstractLoadPredictor {

	//private Vector<Pair<Double, Integer>> queueHis;
	private Vector<Pair<Double, Integer>> activeServerHis;

	private double windowedQueueSize;
	// private double windowedServerNum;

	/**
	 * end time of current window
	 */
	private double rightWinTime;
	/**
	 * index of the start time of current window in Queue size history __|--|___
	 * the index of left window time in QueueHis
	 */
	private int leftWinIndexInQueue;

	private double leftWinTime;

	private double windowLength;

	@Override
	public double predictCurWorkload(double time, int currentQueueSize) {
		// TODO Auto-generated method stub
		double height = 0.0;
		int queueSize = queueHis.size();
		if (time == queueHis.lastElement().getFirst()) {
			if (queueSize < 2) {
				Sim.fatalError("not enough history in Queue size history");
			}
			height = queueHis.get(queueSize - 2).getSecond();
		} else {
			height = queueHis.lastElement().getSecond();
		}

		windowedQueueSize += (time - rightWinTime) * height;
		windowedQueueSize -= getSubstraction(time, queueHis);

		// windowedServerNum += (time - activeServerHis.lastElement()
		// .getFirst()) * activeServerHis.lastElement().getSecond();
		// windowedServerNum -= getSubstraction(time, activeServerHis);

		rightWinTime = time;

		double predictedWd = windowedQueueSize
				/ (windowLength > time ? time : windowLength)
				/ activeServerHis.lastElement().getSecond();

		addPrediction(time, predictedWd);
		return predictedWd;

	}

	private double getSubstraction(double time,
			Vector<Pair<Double, Integer>> his) {
		// TODO Auto-generated method stub
		if (time <= windowLength) {
			return 0.0;
		} else {
			if (leftWinIndexInQueue == -1) {
				leftWinIndexInQueue = 0;
				leftWinTime = 0.0;
			}
			// int startIndex = -1;
			int stopIndex = -1;
			for (int i = leftWinIndexInQueue; i < his.size(); i++) {
				if (time - windowLength >= his.get(i).getFirst()) {
					stopIndex = i;

				}
			}

			if (leftWinIndexInQueue == stopIndex) {
				leftWinTime = time - windowLength;
				return (time - rightWinTime)
						* his.get(leftWinIndexInQueue).getSecond();
			} else {
				double delta = 0.0;
				for (int j = leftWinIndexInQueue; j <= stopIndex; j++) {
					if (j == leftWinIndexInQueue) {
						delta += (his.get(j + 1).getFirst() - leftWinTime)
								* his.get(j).getSecond();
					}

					else if (j == stopIndex) {
						delta += (time - windowLength - his.get(j).getFirst())
								* his.get(j).getSecond();
					}

					else {
						delta += (his.get(j + 1).getFirst() - his.get(j)
								.getFirst()) * his.get(j).getSecond();
					}
				}

				leftWinIndexInQueue = stopIndex;
				leftWinTime = time - windowLength;
				return delta;
			}
		}
	}

	public WindowedAveragePredictor(GlobalQueueScheduler taskScheduler, Vector<Pair<Double, Integer>> theQueueHis,
			Vector<Pair<Double, Integer>> theServerHis, boolean dumpPrediction, double length) {
		super(taskScheduler, theQueueHis, theServerHis, dumpPrediction);
		//this.queueHis = theQueueHis;
		this.activeServerHis = theServerHis;
		this.windowLength = length;

		this.windowedQueueSize = 0.0;
		this.rightWinTime = 0.0;
		this.leftWinTime = 0.0;
		leftWinIndexInQueue = -1;
	}

}
