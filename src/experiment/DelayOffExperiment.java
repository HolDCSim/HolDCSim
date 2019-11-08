package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.DelayOffController;
import infrastructure.IActivityUnit;
import infrastructure.OnOffServer;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import debug.Sim;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;
import scheduler.OnOffScheduler;

public class DelayOffExperiment extends OnOffExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ExponentialRandom delayTimeGen;

	protected static double meanDelayTime;

	public DelayOffExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
		// TODO Auto-generated constructor stub
		this.statsFileName = "delayoff_stats";
		this.sleepStateMode = "delayoff";

		if (Sim.RANDOM_SEEDING) {
			delayTimeGen = new ExponentialRandom(1 / expConfig.getMeanDelayTime(),
					System.currentTimeMillis());
		} else {
			delayTimeGen = new ExponentialRandom(1 / expConfig.getMeanDelayTime(), 40000);
		}

	}
	
	@Override
	protected void formulateResults() {
		super.formulateResults();
		
		resultFieldValueMap.put(String.format("%-15s", "schePolicy"), String.format("%-15s", mExpConfig.getTaskSchePolicy().name()));
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("meandeltime", true, "mean delay time");

		if(argParser.parse("meandeltime") == null) {
			Sim.fatalError("DelayOffExperiment requires mean delay time param");
		}

		meanDelayTime = Double.parseDouble(argParser.parse("meandeltime"));
		mExpConfig.setMeanDelayTime(meanDelayTime);
		// mExpConfig.setWorkloadThreshold(threshold);
	}

	public double getNextDelayTime() {
		// for our comparison
		// return meanDelayTime;
		return delayTimeGen.getNextDouble();
	}

	@Override
	public void initialize() {
		super.initialize();

		this.setUseMultipleSS(true);

		Vector<Server> servers = dataCenter.getServers();

		for (Server aServer : servers) {
			UniprocessorServer sServer = (UniprocessorServer) aServer;
			// set the next sleep state to 1 since delayoff uses multiple sleep
			// state
			sServer.setNextFixedSleepState(1);
			AbstractSleepController abController = sServer.getSleepController();
			//Core aCore = sServer.getCore();
			abController.setUseMultipleSleepState(true);
		}
	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.DELAY_OFF_SERVER;
	}

	@Override
	public void shutoffTranServer(OnOffServer aServer, double time) {
		OnOffServer theServer = ((OnOffScheduler) taskScheduler)
				.shutoffTranServer((OnOffServer) aServer, time);
		DelayOffController doController = (DelayOffController) theServer.getSleepController();
		doController.resetDelayOffTime();
	}

	@Override
	public void writeAllServerStats() {
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
					+ String.format("%-20s", "delayToOff")
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
					+ String.format("%-20.2f", mExpConfig.getMeanDelayTime())
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
