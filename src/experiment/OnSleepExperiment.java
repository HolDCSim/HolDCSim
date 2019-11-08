package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.IActivityUnit;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import loadpredictor.FakeLoadPredictor;


import debug.Sim;

import scheduler.MultiServerScheduler;
import scheduler.OnSleepScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;

import experiment.ExperimentConfig.ServerType;

/**
 * Onsleep experiment, which uses sleep state rather than off state 
 * @author fanyao
 *
 */
public class OnSleepExperiment extends MultiServerExperiment {

	protected static double meanSetupTime;

	//private OnSleepScheduler onoffScheduler;

	protected ExponentialRandom setupTimeGen;
	/**
	 * fanyao
	 */
	private static final long serialVersionUID = 1L;

	public OnSleepExperiment(String theExperimentName, ExponentialRandom aRandom,
			ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);

		if(Sim.RANDOM_SEEDING){
			setupTimeGen = new ExponentialRandom(1 / meanSetupTime, System.currentTimeMillis());
		}
		else{
			setupTimeGen = new ExponentialRandom(1 / meanSetupTime, 30000);
		}
		
		this.statsFileName = "ONSLEEP_stats";
		this.sleepStateMode = "on_sleep";

	}

	public double getNextSetupTime() {
		// for our comparison
		// return meanSetupTime;
		return meanSetupTime;
		//return setupTimeGen.getNextDouble();
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("meansetuptime", true, "mean setup time");

		if(argParser.parse("meansetuptime") == null) {
			Sim.fatalError("OnSleepExperiment requires mean setup time param");
		}

		meanSetupTime = Double.parseDouble(argParser.parse("meansetuptime"));
		// mExpConfig.setWorkloadThreshold(threshold);
	}

	@Override
	public void initialize() {
		super.initialize();
		Vector<Server> servers = dataCenter.getServers();

		// for original delayoff implementation
		/*******************************************************
		 * onoff2Scheduler = (OnOff2Scheduler) taskScheduler;
		 *******************************************************/

		//onoffScheduler = (OnSleepScheduler) taskScheduler;

		for (Server aServer : servers) {
			UniprocessorServer sServer = (UniprocessorServer) aServer;
			// FIXME: current the deep sleep state is hard coded to 5
			sServer.setNextFixedSleepState(5);
		}
	}

	// @Override
	// protected void setTaskScheduler() {
	// taskScheduler = new OnOffScheduler(this);
	// }

	@Override
	protected void setTaskScheduler() {
		// OnOff2Scheduler is not original delayoff experiment implementation
		/**********************************************
		 * taskScheduler = new OnOff2Scheduler(this);
		 **********************************************/
		taskScheduler = new OnSleepScheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
				msScheduler.getQueueHis()));
	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.ON_SLEEP_SERVER;
	}

	@Override
	protected void formulateResults() {
		super.formulateResults();
		
		resultFieldValueMap.put(String.format("%-15s", "setupTime"), String.format("%-15.2f", meanSetupTime));
		resultFieldValueMap.put(String.format("%-15s", "delayoffTime"), String.format("%-15.5f", mExpConfig.getMeanDelayTime()));
		
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
//						+ String.format("%-15s", "delayoffTime")
//						+ String.format("%-15s", "executionTime")
//						+ String.format("%-20s", "averageServiceT")
//						+ String.format("%-20s", "averageLatency")
//						+ String.format("%-20s", "NomalizedLatency")
//						+ String.format("%-20s", "totalEnergy")
//						+ String.format("%-20s", "averagePower")
//						+ String.format("%-15s", "percentile"));
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
//									+ String.format("%.2f", percentileLatencies.get(0))));
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

	@Override
	protected void writeAllServerStats() {
		File file = new File(statsFileName + "_details");
		FileWriter fw;

		// Core theCore = server.getCore();
		// write statistics to file
		try {

			// use append mode
			fw = new FileWriter(file, true);
			fw.write("************************************************************************\n");
			fw.write(String.format("%-20s", "date")
					+ String.format("%-20s", "numOfJobs")
					+ String.format("%-20s", "rou")
					+ String.format("%-20s", "uBar")
					+ String.format("%-20s", "speed")
					+ String.format("%-20s", "sleepstateMode")
					+ String.format("%-20s", "numOfServers")
					+ String.format("%-20s", "setupTime")
					+ String.format("%-20s", "executionTime")
					+ String.format("%-20s", "averageServiceT")
					+ String.format("%-20s", "averageLatency")
					+ String.format("%-20s", "nomalizedLatency")
					+ String.format("%-20s", "totalEnergy"));
			fw.write("\n");

			fw.write(String.format("%-20s", fileCreatTime)
					+ String.format("%-20d", mExpConfig.getNumOfJobs())
					+ String.format("%-20.2f", mExpConfig.getRou())
					+ String.format("%-20.5f", mExpConfig.getuBar())
					+ String.format("%-20s", mExpConfig.getSpeed())
					+ String.format("%-20s", sleepStateMode)
					+ String.format("%-20d", mExpConfig.getServersToSchedule())
					+ String.format("%-20.2f", meanSetupTime)
					+ String.format("%-20.5f", this.simulationTime)
					+ String.format("%-20.5f", this.getAverageServiceTime())
					+ String.format("%-20.5f", averageLatencies.get(0))
					+ String.format("%-20.5f", normalizedLatencies.get(0))
					+ String.format("%-20.5f", totalEnergy));
			fw.write("\n");

			fw.write(String.format("%-20s", "serverNo")
					+ String.format("%-20s", "finishedTasks")
					+ String.format("%-20s", "serviceTime")
					+ String.format("%-20s", "idleTime")
					+ String.format("%-20s", "wakeupTime")
					+ String.format("%-20s", "energy"));
			fw.write("\n");

			Vector<Server> servers = this.getDataCenter().getServers();
			for (int i = 0; i < mExpConfig.getServersToSchedule(); i++) {
				UniprocessorServer sServer = (UniprocessorServer) servers.get(i);

				AbstractSleepController abController = sServer.getSleepController();
				IActivityUnit activityUnit = sServer.getActivityUnit();
				fw.write(String.format("%-20d", sServer.getNodeId())
						+ String.format("%-20d", activityUnit.getFinishedTasks())
						+ String.format("%-20.5f", abController.serviceTime)
						+ String.format("%-20.5f", abController.idleTime)
						+ String.format("%-20.5f", abController.wakeupTime)
						+ String.format("%-20.5f", sServer.energy));

				fw.write("\n");
			}

			fw.flush();
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
