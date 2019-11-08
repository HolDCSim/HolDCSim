package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.IActivityUnit;
import infrastructure.LCSwitch;
import infrastructure.ShallowDeepServer;
import infrastructure.Server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import loadpredictor.AbstractLoadPredictor;
import loadpredictor.AllAveragePredictor;
import loadpredictor.InstantPredictor;
import loadpredictor.WindowedAveragePredictor;



//import queue.MSGlobalQueue;

import debug.Sim;

import scheduler.ShallowDeepScheduler;
import stochastic.ExponentialRandom;
import utility.Pair;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;

public class ShallowDeepExperiment extends MultiServerExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ShallowDeepExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
		this.statsFileName = "EE_stats";
		this.sleepStateMode = "shallow-deep";
		// TODO Auto-generated constructor stub
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("ts", true, "ts");
		argParser.addOption("tw", true, "tw");
		argParser.addOption("predtype", true, "predictor type");

		if(argParser.parse("ts") == null || argParser.parse("tw") == null || argParser.parse("predtype") == null) {
			Sim.fatalError("ShallowDeepExperiment requires ts, tw, and predictor type params");
		}

		ShallowDeepScheduler.Ts = Double.parseDouble(argParser.parse("ts"));
		ShallowDeepScheduler.Tw = Double.parseDouble(argParser.parse("tw"));
		
		if (ShallowDeepScheduler.Ts > ShallowDeepScheduler.Tw) {
			Sim.warning("Ts is greater than Tw, return");
			System.exit(0);
		}
		double predictorType = Double.parseDouble(argParser.parse("predtype"));
		mExpConfig.setPredictorType(predictorType);

	}

	@Override
	public void initialize() {
		super.initialize();
		
		//set the deep state according to sim.config settings
		//ShallowDeepStates.DEEP = mExpConfig.getDeepState();

		if (globalQueue == null) {
			Sim.fatalError("Fatal errror: global queue is null for EnergyAwareExperiment");
		}

		// int numOfServers = this.getExpConfig().getServersToSchedule();
		// Vector<Server> servers = this.getDataCenter().getServers();
		//
		// ShallowDeepScheduler eeScheduler = (ShallowDeepScheduler)
		// taskScheduler;
		// for (int i = 0; i < numOfServers; i++) {
		// EnergyAwareServer sServer = (EnergyAwareServer) servers.get(i);
		// sServer.setShallowSleepState(eeScheduler.getShallowSleepState());
		// sServer.setDeepSleepState(eeScheduler.getDeepSleepState());
		// }

	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.SHALLOW_DEEP_SERVER;
	}

	@Override
	protected void setTaskScheduler() {
		ShallowDeepScheduler eeScheduler = new ShallowDeepScheduler(this);
		taskScheduler = eeScheduler;
		double predictorType = mExpConfig.getPredictorType();

		AbstractLoadPredictor predictor = null;
		if (predictorType == -1) {
			predictor = new InstantPredictor(eeScheduler, eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(), mExpConfig.dumpPrediction());
		}

		else if (predictorType == 0) {
			predictor = new AllAveragePredictor(eeScheduler, eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),mExpConfig.dumpPrediction());
		}

		else {
			predictor = new WindowedAveragePredictor(eeScheduler, eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(), mExpConfig.dumpPrediction(), predictorType);
		}

		eeScheduler.setQueuePredictor(predictor);
	}


	@Override
	protected void writeAllServerStats() {
		File file = new File(statsFileName + "_details");
		FileWriter fw;
		String fileName = this.fileCreatTime + ".txt";

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
					+ String.format("%-20s", "Ts")
					+ String.format("%-20s", "Tw")
					+ String.format("%-20s", "executionTime")
					+ String.format("%-20s", "averageServiceT")
					+ String.format("%-20s", "averageLatency")
					+ String.format("%-20s", "nomalizedLatency")
					+ String.format("%-20s", "totalEnergy"));
			fw.write("\n");

			Sim.log(fileName, "allServerStat:" + 
					String.format("%s", fileCreatTime) + ":"
					+ String.format("%d", mExpConfig.getNumOfJobs())+ ":"
					+ String.format("%.2f", mExpConfig.getRou()) + ":"
					+ String.format("%.5f", mExpConfig.getuBar()) + ":"
					+ String.format("%s", mExpConfig.getSpeed()) + ":"
					+ String.format("%s", sleepStateMode) + ":"
					+ String.format("%d", mExpConfig.getServersToSchedule()) + ":"
					+ String.format("%.2f", ShallowDeepScheduler.Ts) + ":"
					+ String.format("%.2f", ShallowDeepScheduler.Tw) + ":"
					+ String.format("%.5f", this.simulationTime) + ":"
					+ String.format("%.5f", this.getAverageServiceTime()) + ":"
					+ String.format("%.5f", averageLatencies.get(0)) + ":"
					+ String.format("%.5f", normalizedLatencies.get(0)) + ":"
					+ String.format("%.5f", totalEnergy) + ":");
			fw.write(String.format("%-20s", fileCreatTime)
					+ String.format("%-20d", mExpConfig.getNumOfJobs())
					+ String.format("%-20.2f", mExpConfig.getRou())
					+ String.format("%-20.5f", mExpConfig.getuBar())
					+ String.format("%-20s", mExpConfig.getSpeed())
					+ String.format("%-20s", sleepStateMode)
					+ String.format("%-20d", mExpConfig.getServersToSchedule())
					+ String.format("%-20.2f", ShallowDeepScheduler.Ts)
					+ String.format("%-20.2f", ShallowDeepScheduler.Tw)
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
			
			Sim.log(fileName, "!!full_server_table");
			for (int i = 0; i < mExpConfig.getServersToSchedule(); i++) {
				ShallowDeepServer sServer = (ShallowDeepServer) servers.get(i);

				AbstractSleepController abController = sServer.getSleepController();
				IActivityUnit activityUnit = sServer.getActivityUnit();
				Sim.log(fileName, "full_serv:"  +
						String.format("%d", sServer.getNodeId()) + ":"
						+ String.format("%d", activityUnit.getFinishedTasks()) + ":"
						+ String.format("%.5f", abController.serviceTime) + ":"
						+ String.format("%.5f", abController.idleTime) + ":"
						+ String.format("%.5f", abController.wakeupTime) + ":"
						+ String.format("%.5f", sServer.energy) + ":");
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
			Sim.debug(2, "graphServerResults() \n");
			graphServerResults();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	@Override
	protected void formulateResults() {
		
		super.formulateResults();
		
		String sPredictorType = null;
		double predictorType = mExpConfig.getPredictorType();
		if (predictorType == -1) {
			sPredictorType = "inst";
		} else if (predictorType == 0) {
			sPredictorType = "(0,t)";
		} else if (predictorType > 0) {
			sPredictorType = "windowed-"
					+ String.format("%-5.2f", predictorType);
		}

		else {
			Sim.fatalError("unrecognized predictor type");
		}
		
		
		resultFieldValueMap.put(String.format("%-15s", "Ts"), String.format("%-15.2f", ShallowDeepScheduler.Ts));
		resultFieldValueMap.put(String.format("%-15s", "Tw"), String.format("%-15.2f", ShallowDeepScheduler.Tw));
		resultFieldValueMap.put(String.format("%-15s", "QueuePredictor"), String.format("%-15s", sPredictorType));
		
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
//						+ String.format("%-15s", "rou")
//						+ String.format("%-15s", "uBar")
//						+ String.format("%-15s", "speed")
//						+ String.format("%-15s", "sleepstateMode")
//						+ String.format("%-15s", "numOfServers")
//						+ String.format("%-15s", "Ts")
//						+ String.format("%-15s", "Tw")
//						+ String.format("%-15s", "QueuePredictor")
//						+ String.format("%-15s", "executionTime")
//						+ String.format("%-20s", "averageServiceT")
//						+ String.format("%-20s", "averageLatency")
//						+ String.format("%-20s", "NomalizedLatency")
//						+ String.format("%-15s", "totalEnergy")
//						+ String.format("%-15s", "averagePower")
//						+ String.format("%-15s", "percentile"));
//				fw.write("\n");
//			} else {
//				fw = new FileWriter(file, true);
//			}
//
//			String sPredictorType = null;
//			double predictorType = mExpConfig.getPredictorType();
//			if (predictorType == -1) {
//				sPredictorType = "inst";
//			} else if (predictorType == 0) {
//				sPredictorType = "(0,t)";
//			} else if (predictorType > 0) {
//				sPredictorType = "windowed-"
//						+ String.format("%-5.2f", predictorType);
//			}
//
//			else {
//				Sim.fatalError("unrecognized predictor type");
//			}
//
//			fw.write(String.format("%-15s", fileCreatTime)
//					+ String.format("%-15d", mExpConfig.getNumOfJobs())
//					+ String.format("%-15.2f", mExpConfig.getRou())
//					+ String.format("%-15.5f", mExpConfig.getuBar())
//					+ String.format("%-15.2f", speed)
//					+ String.format("%-15s", sleepStateMode)
//					+ String.format("%-15d", mExpConfig.getServersToSchedule())
//					+ String.format("%-15.2f", ShallowDeepScheduler.Ts)
//					+ String.format("%-15.2f", ShallowDeepScheduler.Tw)
//					+ String.format("%-15s", sPredictorType)
//					+ String.format("%-15.2f", this.simulationTime)
//					+ String.format("%-20.5f", this.getAverageServiceTime())
//					+ String.format("%-20.5f", averageLatencies.get(0))
//					+ String.format("%-20.5f", normalizedLatencies.get(0))
//					+ String.format("%-15.2f", totalEnergy)
//					+ String.format("%-15.2f", totalEnergy
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
//			// + mExpConfig.getServersToSchedule() + "\t" + 0 + "\t" + 0
//			// + "\t" + this.simulationTime + "\t"
//			// + this.getAverageServiceTime() + "\t" + averageLatency
//			// + "\t" + normalizedLatency + "\t" + energy + "\t" + energy
//			// / this.simulationTime);
//			// fw.write("\n");
//
//			fw.flush();
//			fw.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	@Override
	public void dumpEnergyPerfStats() {
		super.dumpEnergyPerfStats();

		/* dump workload prediction history */
		if (mExpConfig.dumpPrediction()) {
			((ShallowDeepScheduler) taskScheduler).dumpPredictionHis();
		}

		/* dump history of scheduled active servers*/
		if (mExpConfig.dumpSchServerHis()) {
			((ShallowDeepScheduler) taskScheduler).dumpSchServerHis();
		}
		
	}
	

	

}
