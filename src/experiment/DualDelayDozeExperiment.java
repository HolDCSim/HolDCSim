package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.DualDelayOffController;
import infrastructure.EnergyServer;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import loadpredictor.FakeLoadPredictor;

import debug.Sim;
import scheduler.DualDelayScheduler;
import scheduler.MultiServerScheduler;
import scheduler.OnSleepScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;

public class DualDelayDozeExperiment extends DelayDozeExperiment {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	private ExponentialRandom lowDelayGen;
	
	protected static double meanLowDelayTime; 
	
	protected int lowDelayServers;

	public DualDelayDozeExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		// TODO Auto-generated constructor stub
		
		//highDelayGen = new ExponentialRandom(1 / expConfig.getMeanDelayTime(),
		//		4123);
		this.statsFileName = "dualdelay_stats";
		this.sleepStateMode = "dualdelay";
		lowDelayGen = new ExponentialRandom(1/ expConfig.getMeanLowDelayTime(), 4123);
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);

		argParser.addOption("meanldeltime", true, "mean low delay time");
		argParser.addOption("hdelservnum", true, "high delay server num");
		
		if(argParser.parse("meanldeltime") == null || argParser.parse("hdelservnum") == null) {
			Sim.fatalError("DualDelayDozeExperiment requires mean low delay time and high delay server num params");
		}

		meanLowDelayTime = Double.parseDouble(argParser.parse("meanldeltime"));
		mExpConfig.setMeanLowDelayTime(meanLowDelayTime);
		mExpConfig.setHighDelayServerNum(Integer.parseInt(argParser.parse("hdelservnum")));
		// mExpConfig.setWorkloadThreshold(threshold);
	}
	
	public double getNextLowDelayTime() {
		// use fixed delay time in our paper
		 return meanLowDelayTime;
		
		//return lowDelayGen.getNextDouble();
	}
	
	@Override
	public void setTaskScheduler(){
		taskScheduler = new DualDelayScheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
				msScheduler.getQueueHis()));
	}
	
	@Override
	public void initialServerType(){
		this.serverType = ServerType.DUAL_DELAY_SERVER;
	}
	
	@Override
	protected void formulateResults() {
		
		super.formulateResults();

		resultFieldValueMap.put(String.format("%-15s", "highTau"), String.format("%-15s",mExpConfig.getMeanDelayTime()));
		resultFieldValueMap.put(String.format("%-15s", "LowTau"), String.format("%-15.5f", mExpConfig.getMeanLowDelayTime()));
		resultFieldValueMap.put(String.format("%-15s", "activeServers"), String.format("%-15d", mExpConfig.getHighDelayServerNum()));
		
//		// used to log critical experiment result statistics
//		File file = new File(statsFileName);
//		FileWriter fw;
//
//		double speed = dataCenter.getExpConfig().getSpeed();
//
//		// write statistics to file
//		try {
//
//			if (!file.exists()) {
//				// use append mode
//				fw = new FileWriter(new File(statsFileName), true);
//				fw.write(String.format("%-15s", "date")
//						+ String.format("%-15s", "numOfJobs")
//						+ String.format("%-20s", "rou")
//						+ String.format("%-20s", "uBar")
//						+ String.format("%-20s", "speed")
//						+ String.format("%-20s", "sleepstateMode")
//						+ String.format("%-15s", "numOfServers")
//						+ String.format("%-15s", "setupTime")
//						+ String.format("%-15s", "highTau")
//						+ String.format("%-15s", "lowTau")
//						+ String.format("%-15s", "activeServers")
//						+ String.format("%-15s", "schePolicy")
//						+ String.format("%-15s", "executionTime")
//						+ String.format("%-20s", "averageServiceT")
//						+ String.format("%-20s", "averageLatency")
//						+ String.format("%-20s", "NomalizedLatency")
//						+ String.format("%-20s", "totalEnergy")
//						+ String.format("%-20s", "averagePower")
//						+ String.format("%-15s", "percentile")
//						+ String.format("%-15s", "percentile95")
//						+ String.format("%-15s", "percentile99")
//						);
//				fw.write("\n");
//			} else {
//				fw = new FileWriter(file, true);
//			}
//
//			fw.write(String.format("%-15s", fileCreatTime)
//					+ String.format("%-15d", mExpConfig.getNumOfJobs())
//					+ String.format("%-20.2f", mExpConfig.getRou())
//					+ String.format("%-20.5f", mExpConfig.getuBar())
//					+ String.format("%-20.2f", speed)
//					+ String.format("%-20s", sleepStateMode)
//					+ String.format("%-15d", mExpConfig.getServersToSchedule())
//					+ String.format("%-15.2f", meanSetupTime)
//					+ String.format("%-15.5f", mExpConfig.getMeanDelayTime())
//					+ String.format("%-15.5f", mExpConfig.getMeanLowDelayTime())
//					+ String.format("%-15d", mExpConfig.getLowDelayServerNum())
//					+ String.format("%-15s", mExpConfig.getTaskSchePolicy().name())
//					+ String.format("%-15.5f", this.simulationTime)
//					+ String.format("%-20.5f", this.getAverageServiceTime())
//					+ String.format("%-20.5f", averageLatencies.get(0))
//					+ String.format("%-20.5f", normalizedLatencies.get(0))
//					+ String.format("%-20.5f", totalEnergy)
//					+ String.format("%-20.5f", totalEnergy
//							/ this.simulationTime)
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->",
//									mExpConfig.getLatencyPercent() * 100)
//									+ String.format("%.2f",
//											percentileLatencies.get(0)))
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->", 95.0)
//									+ String.format("%.2f",
//											percentileLatency95s.get(0)))
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->", 99.0)
//									+ String.format("%.2f",
//											percentileLatency99s.get(0)))
//
//			);
//			fw.write("\n");
//
//			// fw.write(fileCreatTime + "\t" + mExpConfig.getNumOfJobs() + "\t"
//			// + mExpConfig.getRou() + "\t" + mExpConfig.getuBar() + "\t"
//			// + speed + "\t" + sleepStateMode + "\t"
//			// + mExpConfig.getServersToSchedule() + "\t" + meanSetupTime
//			// + "\t" + this.simulationTime + "\t"
//			// + this.getAverageServiceTime() + "\t" + averageLatency
//			// + "\t" + normalizedLatency + "\t" + energy + "\t" + energy
//			// / this.simulationTime);
//			// fw.write("\n");
//
//			fw.flush();
//			fw.close();
//
//			// dumpIdleDistributions();
//			// writeAllServerStats();
//
//			// this.dumpActualServerHis();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	
	

}
