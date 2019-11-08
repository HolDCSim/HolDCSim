package loadpredictor;

import java.util.Vector;

import java.io.*;

import debug.Sim;

import scheduler.GlobalQueueScheduler;
import utility.AsynLoggedVector;
import utility.FakeRecordVector;
import utility.Pair;

public abstract class AbstractLoadPredictor {

	private static String predictHisName = "predict_his.txt";

	protected Vector<Pair<Double, Integer>> queueHis;
	protected Vector<Pair<Float, Float>> predictHis;
	// protected Vector<Pair<Double, Integer>> serverHis;
	protected boolean asynLogged;

	// need for dump queue history
	private boolean dumpPrediction;
	public AbstractLoadPredictor(GlobalQueueScheduler taskScheduler,
			Vector<Pair<Double, Integer>> queueHis,
			Vector<Pair<Double, Integer>> serverHis, boolean dumpPrediction) {
		this.queueHis = queueHis;
		// this.serverHis = serverHis;
		this.dumpPrediction = dumpPrediction;
		if (asynLogged) {
			predictHis = new AsynLoggedVector<Pair<Float, Float>>(
					predictHisName);
			taskScheduler.getExperiment().registerLoggedVectors(
					(AsynLoggedVector<?>) predictHis);
		} else {
//			predictHis = new FakeRecordVector<Pair<Float, Float>>();
			predictHis = new Vector<Pair<Float, Float>>();
		}
	}

	public abstract double predictCurWorkload(double time, int currentQueuSize);

	/**
	 * dump the prediction of workload
	 */
	public void dumpPredictionHis() {
		if(predictHis == null || predictHis.size() == 0){
			Sim.debug(3,"no workload predictions collected");
		}
		File queueFile = new File(predictHisName);
		try {
			FileWriter fw = new FileWriter(queueFile, false);
			BufferedWriter bw = new BufferedWriter(fw);
			// bw.write(String.format("%-15s", "time")
			// + String.format("%-15s", "prediction"));
			// bw.newLine();

			// write history to file
			for (Pair<Float, Float> aPair : predictHis) {
				double time = aPair.getFirst();
				double prediction = aPair.getSecond();

				bw.write(String.format("%-15.5f", time)
						+ String.format("%-15.5f", prediction));
				bw.newLine();
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	

	protected void addPrediction(double time, double prediction) {
		if (!dumpPrediction)
			return;

		Pair<Float, Float> aPair = new Pair<Float, Float>();
		aPair.setFirst((float)time);
		aPair.setSecond((float)prediction);
		predictHis.add(aPair);
	}

}
