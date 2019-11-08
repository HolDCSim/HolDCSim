package experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import utility.Pair;
import queue.BaseQueue;
import scheduler.CompactTaskScheduler;
import scheduler.SimpleTaskScheduler;
import stochastic.ExponentialRandom;
import utility.StringTokens;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;

/**
 * Simple link sleep strategy by default experiment 
 *(link enters LPI when no packet to process and wake up upon packets arrival)
 * @author fanyao
 *
 */
public class LPIBaseExperiment extends NetworkExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static int MS_EXP_PARAMS = 1;

	private boolean dumpActualSwitchHis;

	private static String actualSwitchHisName = "actual_switch_his.txt";

	protected Vector<Pair<Float, Integer>> actualActiveSwitchHis;
	
	
	/**
	 * map that store result output fields and values 
	 */	
	protected Map<String, String> resultFieldValueMap;

	public LPIBaseExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, theExperimentInput,
				thExperimentOutput, expConfig, argParser);

		// this should be done in initialize when asyn_log is set
		// this.actualActiveServerHis = new Vector<Pair<Double, Integer>>();
		this.statsFileName = "NetworkBase_stats";
		
		resultFieldValueMap = new LinkedHashMap<String, String>();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void setTaskScheduler() {
		//taskScheduler = new SimpleTaskScheduler(this);
		taskScheduler = new CompactTaskScheduler(this);
		//SimpleTaskScheduler nScheduler = (SimpleTaskScheduler) taskScheduler;
		//msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler, msScheduler.getQueueHis()));
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
	}

	@Override
	public void dumpEnergyPerfStats() {
		
	}
	
	protected void serializeResult(){
		File file = new File(statsFileName);
		FileWriter fw;
		
		 //write statistics to file
		try {

			if (!file.exists()) {
				// use append mode
				fw = new FileWriter(new File(statsFileName), true);
				
				String fieldString = "";
				
				for(Map.Entry<String, String> entry : resultFieldValueMap.entrySet()){
					fieldString += entry.getKey();
				}
				fw.write(fieldString);
				fw.write("\n");
			} else {
				fw = new FileWriter(file, true);
			}
			
			String valueString = "";
			for(Map.Entry<String, String> entry : resultFieldValueMap.entrySet()){
				valueString += entry.getValue();
			}

			fw.write(valueString);
			fw.write("\n");

			// fw.write(fileCreatTime + "\t" + mExpConfig.getNumOfJobs() + "\t"
			// + mExpConfig.getRou() + "\t" + mExpConfig.getuBar() + "\t"
			// + speed + "\t" + sleepStateMode + "\t"
			// + mExpConfig.getServersToSchedule() + "\t" + 0 + "\t" + 0
			// + "\t" + this.simulationTime + "\t"
			// + this.getAverageServiceTime() + "\t" + averageLatency
			// + "\t" + normalizedLatency + "\t" + energy + "\t" + energy
			// / this.simulationTime);
			// fw.write("\n");

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	protected void formulateResults() {}

	protected void writeAllServerStats() {}

	@Override
	public double dumpIdleDistributions() {

		double totalIdleTime=0;
		return totalIdleTime;
	}

	@Override
	public void initialize() {
		super.initialize();
		// in LPI base experiment, all the links are set to LPI state initially

		updateServerHis(0.0, 0);

	}

	public void updateServerHis(double time, int delta) {
		// if no need to dump actual server history, do not collect them
		if (!dumpActualSwitchHis)
			return;

		Pair<Float, Integer> newHis = new Pair<Float, Integer>();

		if (actualActiveSwitchHis.size() == 0) {
			newHis.setFirst((float) time);
			newHis.setSecond(delta);

		} else {
			Pair<Float, Integer> lastHis = actualActiveSwitchHis.lastElement();
			newHis.setFirst((float) time);
			newHis.setSecond(lastHis.getSecond() + delta);
		}

		actualActiveSwitchHis.add(newHis);
	}

	public void dumpActualSwitchHis() {

		// no dump needed, in order to save space

		File queueFile = new File(actualSwitchHisName);
		try {
			FileWriter fw = new FileWriter(queueFile, false);
			BufferedWriter bw = new BufferedWriter(fw);
			// bw.write(String.format("%-15s", "time")
			// + String.format("%-15s", "actual_server_num"));
			// bw.newLine();

			// write history to file
			for (Pair<Float, Integer> aPair : actualActiveSwitchHis) {
				double time = aPair.getFirst();
				int actualActiveServers = aPair.getSecond();

				bw.write(String.format("%-15.5f", time)
						+ String.format("%-15d", actualActiveServers));
				bw.newLine();
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
