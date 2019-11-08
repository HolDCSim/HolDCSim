package loadpredictor;

import java.util.Vector;

import scheduler.GlobalQueueScheduler;
import utility.Pair;

/**
 * @author fanyao a fake queue predictor used for multi-server experiment
 * 
 */
public class FakeLoadPredictor extends AbstractLoadPredictor {

	public FakeLoadPredictor(GlobalQueueScheduler taskScheduler,
			Vector<Pair<Double, Integer>> queueHis) {
		super(taskScheduler, queueHis, null, false);

		// TODO Auto-generated constructor stub
	}

	@Override
	public double predictCurWorkload(double time, int currentQueueSize) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void dumpPredictionHis() {
		return;
	}

}
