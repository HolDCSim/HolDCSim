package queue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

import debug.Sim;

import job.Task;

import utility.Pair;

public class BaseQueue {

	private static String queueHisName = "queue_his.txt";

	// FIXEME: hardcoded job size
	double GOOGLE_SIZE = 4.2e-3;
	double DNS_SIZE = 194e-3;

	/** internal queue for holding incoming taks requese */
	public LinkedList<Task> internalQueue;

	/** need to collect queuing history data ?*/
	protected boolean doCollectHis;
	
	/** queuing history data container*/
	protected Vector<Pair<Double, Integer>> globalQueueHis;

	// statistics
	protected double totalPendingTime;

	public double getTotalPendingTime() {
		return totalPendingTime;
	}

	public void dumpQueueHis() {
		if (globalQueueHis == null || globalQueueHis.size() == 0) {
			Sim.debug(3, "no queue history collected");
			return;
		}
		File queueFile = new File(queueHisName);
		try {
			FileWriter fw = new FileWriter(queueFile, false);
			BufferedWriter bw = new BufferedWriter(fw);
			// bw.write(String.format("%-15s", "time")
			// + String.format("%-15s", "queuesize"));
			// bw.newLine();

			// write history to file
			for (Pair<Double, Integer> aPair : globalQueueHis) {
				double time = aPair.getFirst();
				int queueSize = aPair.getSecond();

				bw.write(String.format("%-15.5f", time)
						+ String.format("%-15d", queueSize));
				bw.newLine();
				// bw.flush();
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BaseQueue(boolean collectHis) {
		internalQueue = new LinkedList<Task>();
		globalQueueHis = new Vector<Pair<Double, Integer>>();
		// Pair<Double, Integer> currentHis = new Pair<Double, Integer>();
		this.doCollectHis = collectHis;
		totalPendingTime = 0.0;

		if (doCollectHis) {
			this.updateQueueHis(0.0);
		}

	}

	public Vector<Pair<Double, Integer>> getGlobalQueueHis() {
		return this.globalQueueHis;
	}

	public Task poll(double time) {
		Task task = internalQueue.poll();

		if (task.getJobType() == 0)
			totalPendingTime -= GOOGLE_SIZE;
		else if (task.getJobType() == 1)
			totalPendingTime -= DNS_SIZE;

		// updateThreadholds(time);
		// FIXME: collectQueueHis should be unset if use windowed average
		// predictor
		if (doCollectHis) {
			this.updateQueueHis(time);
		}

		return task;
	}
	
	public Task polltask(double time, Task t) {
		Task task;
		int ind = internalQueue.indexOf(t);
		if (ind != -1) {
			task = internalQueue.get(ind);
			internalQueue.remove(task);
		} else {
		
		 task = internalQueue.poll();
		}

		if (task.getJobType() == 0)
			totalPendingTime -= GOOGLE_SIZE;
		else if (task.getJobType() == 1)
			totalPendingTime -= DNS_SIZE;

		// updateThreadholds(time);
		// FIXME: collectQueueHis should be unset if use windowed average
		// predictor
		if (doCollectHis) {
			this.updateQueueHis(time);
		}

		return task;
	}

	public void add(Task task, double time) {
		if (task.getJobType() == 0)
			totalPendingTime += GOOGLE_SIZE;
		else if (task.getJobType() == 1)
			totalPendingTime += DNS_SIZE;

		internalQueue.add(task);
		// updateThreadholds(time);
		if (doCollectHis) {
			this.updateQueueHis(time);
		}

	}

	public void updateQueueHis(double time) {
		// collect queuing information
		Pair<Double, Integer> aPair = new Pair<Double, Integer>();
		aPair.setFirst(time);
		aPair.setSecond(internalQueue.size());
		globalQueueHis.add(aPair);
	}

	public int size() {
		return internalQueue.size();
	}

	public Task get(int index) {
		return internalQueue.get(index);
	}

	public Task poll() {
		Task task = internalQueue.poll();

		if (task.getJobType() == 0)
			totalPendingTime -= GOOGLE_SIZE;
		else if (task.getJobType() == 1)
			totalPendingTime -= DNS_SIZE;

		return task;
	}
}
