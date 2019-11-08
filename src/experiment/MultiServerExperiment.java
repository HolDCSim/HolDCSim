package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.IActivityUnit;
import infrastructure.LCSwitch;
import infrastructure.Server;
import infrastructure.ShallowDeepServer;
import infrastructure.UniprocessorServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import loadpredictor.FakeLoadPredictor;
import utility.Pair;

import queue.BaseQueue;

import debug.Sim;

import scheduler.MultiServerScheduler;
import stochastic.ExponentialRandom;
import utility.AsynLoggedVector;
import workload.*;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;
import experiment.ExperimentConfig.SimMode;

/**
 * Simple multi-server alwayson strategy by default experiment
 * @author fanyao
 *
 */
public class MultiServerExperiment extends SingletonJobExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected BaseQueue globalQueue;

	private boolean dumpActualServerHis;

	private static String actualServerHisName = "actual_server_his.txt";

	protected Vector<Pair<Float, Integer>> actualActiveServerHis;
	
	
	/**
	 * map that store result output fields and values 
	 */	
	protected Map<String, String> resultFieldValueMap;

	public MultiServerExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);

		// this should be done in initialize when asyn_log is set
		// this.actualActiveServerHis = new Vector<Pair<Double, Integer>>();
		this.statsFileName = "MS_stats";
		this.sleepStateMode = "shallow";
		
		resultFieldValueMap = new LinkedHashMap<String, String>();
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean useGlobalQueue() {
		return true;
	}

	@Override
	public int getGlobalQueueSize() {
		return globalQueue.size();
	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.UNIPROCESSOR_SERVER;
	}

	@Override
	public BaseQueue getGlobalQueue() {
		return globalQueue;
	}

	@Override
	protected void setTaskScheduler() {
		taskScheduler = new MultiServerScheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
				msScheduler.getQueueHis()));
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("numserv", true, "num of servers");
		
		if(argParser.parse("numserv") == null) {
			Sim.fatalError("MultiServerExperiment requires num of servers param");
		}

		int numberOfServers = Integer.parseInt(argParser.parse("numserv"));
		mExpConfig.setServersToSchedule(numberOfServers);
		mExpConfig.setInitialJobsToSkip(numberOfServers * mExpConfig.initialJobsToSkip());
		
		//update number of jobs if it is real trace job generator
//		AbstractWorkloadGen tempGen = mExpConfig.getJobWorkloadGen();
//		if (tempGen instanceof TraceWorkloadGen) {
//			mExpConfig.setNumOfJobs(((TraceWorkloadGen) tempGen).getTotalNumOfJobs());
//		} else {
//			if(mExpConfig.getSimMode() == SimMode.FIXED_JOBS)
//				mExpConfig.setNumOfJobs(numberOfServers * mExpConfig.getNumOfJobs());
//			else
//				mExpConfig.setJobNumOnTime();
//		}
			if(mExpConfig.getSimMode() == SimMode.FIXED_JOBS)
				mExpConfig.setNumOfJobs(numberOfServers * mExpConfig.getNumOfJobs());
			else
				mExpConfig.setJobNumOnTime();
		
		// mExpConfig.setWorkloadThreshold(threshold);
	}

	@Override
	public void dumpEnergyPerfStats() {
		
		String fileName = this.fileCreatTime + ".txt";
		// first dump the number of jobs executed for each server
		int numOfServers = this.getExpConfig().getServersToSchedule();
		Vector<Server> servers = this.getDataCenter().getServers();

		/*  Handled in Colon Seperated List below
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			Sim.log(fileName, "Server :" + sServer.getNodeId() + " finished "
					+ sServer.getFinishedTasks() + " tasks");
		}
		*/
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			double aEnergy = sServer.generateEnergyAndPerfStats();
			AbstractSleepController abController = sServer.getSleepController();

			Sim.debug(2, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			Sim.debug(2,
					"server : " + abController.getNodeId() + "\nServiceTime idle 	C6S3 	wakeupTime  wakeupCounts\n "
							+ String.format("%-10.2f", abController.serviceTime) + " "
							+ String.format("%-10.2f", abController.C0S0Time) + " "
							+ String.format("%-10.2f", abController.C6S3Time) + " "
							+ String.format("%-10.2f", abController.wakeupTime)
							+ String.format("%-5d", abController.C6S3Count));

			//collectedIdleTime 0 means this core is always idle
			if (abController.collectedIdleTime == 0.0)
				abController.collectedIdleTime = this.simulationTime;
			//  ServiceTime:idle:C6S3:wakeupTime:wakeupCounts:TempIdle:Energy
			Sim.log(fileName,
					"server : " + abController.getNodeId()   + ":" 							
							+ String.format("%d", sServer.getFinishedTasks()) + ":"
							+ String.format("%.2f", abController.serviceTime) + ":"
							+ String.format("%.2f", abController.C0S0Time) + ":"
							+ String.format("%.2f", abController.C6S3Time) + ":"
							+ String.format("%.2f", abController.wakeupTime)+ ":"
							+ String.format("%d", abController.C6S3Count) + ":"
							+ String.format("%.5f", abController.collectedIdleTime) + ":"
							+ String.format("%.5f", aEnergy)
			);

			//collectedIdleTime 0 means this core is always idle
//			Sim.log(fileName, "TempIdle " + String.format("%10.5f", abController.collectedIdleTime));

//			Sim.log(fileName, "Energy " + aEnergy);
			totalEnergy += aEnergy;
			//Sim.log(fileName, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		}

		// Sim.debug(1, "\nstatistic passed timing checking !\n");

		// check timing
		boolean statsChecking = true;
		if (mExpConfig.doTimingCheck()) {
			for (int i = 0; i < numOfServers; i++) {
				UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
				if (!sServer.timingCheck()) {
					statsChecking = false;
					break;
				}
			}
		}

		if (statsChecking) {
			Sim.warning("\nstats passed timing check!!!\n");
			// System.exit(-1);
		} else {
			Sim.warning("\nstats did not pass checking, data are not written to file\n");
		}

		/*
		 * //temp code for testing purpose if (mExpConfig.doTimingCheck()) { for
		 * (int i = 0; i < numOfServers; i++) { SingleCoreServer sServer =
		 * (SingleCoreServer) servers.get(i); if (sServer.timingCheck()) {
		 * statsChecking = false; //break; } } }
		 * 
		 * if (statsChecking) { Sim.warning(
		 * "\nstats did not pass checking, data are not written to file\n"); //
		 * System.exit(-1); } else {
		 * 
		 * Sim.warning("\nstats passed timing check!!!\n"); }
		 */

		formulateResults();
		
		serializeResult();

		/* dump idle distribution */
		if (mExpConfig.doCollectIdleDistribution()) {
			dumpIdleDistributions();
		}
		/* dump all server details power and perf */
		if (mExpConfig.doCollectServerDetails()) {
			writeAllServerStats();
		}

		/* dump global queue history */
		if (mExpConfig.doCollectQueueHis()) {
			((MultiServerScheduler) taskScheduler).dumpQueuingHis();
		}

		/* dump active servers history */
		if (mExpConfig.doCollectActServerHis()) {
			this.dumpActualServerHis();
		}
		//System.out.print("doCollectEnergyDistribution:" + mExpConfig.doCollectEnergyDistribution());

		/* dump state durations */

		if (mExpConfig.doCollectStateDurations()) {
			this.dumpDCStateDurations();
		}
		
		/* dump energy distributions */
		if(mExpConfig.doCollectEnergyDistribution()){
			this.dumpEnergyDistributions();
		}

		/* dump job arrivals */
		if (mExpConfig.doCollectJobArrivals()) {
			this.dumpJobArrDist();
		}

		/* dump job finishes */
		if (mExpConfig.doCollectJobCompletes()) {
			this.dumpJobCmpDist();
		}

		// prints number of servers for mixed workload
		if (mWorkloadGen instanceof MixedWorkloadEZGen) {
			Sim.log(fileName,"number of google jobs: "
					+ ((MixedWorkloadEZGen) mWorkloadGen).numOfGoogleJobs);
			Sim.log(fileName,"number of DNS jobs: "
					+ ((MixedWorkloadEZGen) mWorkloadGen).numOfDNSJobs);
		}

	}
	
	protected void dumpEnergyDistributions(){
		int numOfServers = this.getExpConfig().getServersToSchedule();
		Vector<Server> servers = this.getDataCenter().getServers();
		Map<Integer, Double> dcStateDurations = null;
		
		double C0S0IdleEnergy = 0.0;
		double C1S0IdleEnergy = 0.0;
		double C3S0IdleEnergy = 0.0;
		double C6S0IdleEnergy = 0.0;
		double C6S3IdleEnergy = 0.0;

		double C0S0WakeupEnergy = 0.0;
		double C1S0WakeupEnergy = 0.0;
		double C3S0WakeupEnergy = 0.0;
		double C6S0WakeupEnergy = 0.0;
		double C6S3WakeupEnergy = 0.0;
		
		double productiveEnergy = 0.0;
		
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer uServer = (UniprocessorServer)(servers.get(i));
			 C0S0IdleEnergy += uServer.C0S0IdleEnergy;
			 C1S0IdleEnergy += uServer.C1S0IdleEnergy;
			 C3S0IdleEnergy += uServer.C3S0IdleEnergy;
			 C6S0IdleEnergy += uServer.C6S0IdleEnergy;
			 C6S3IdleEnergy += uServer.C6S3IdleEnergy;
			
			 C0S0WakeupEnergy += uServer.C0S0WakeupEnergy;
			 C1S0WakeupEnergy += uServer.C1S0WakeupEnergy;
			 C3S0WakeupEnergy += uServer.C3S0WakeupEnergy;
			 C6S0WakeupEnergy += uServer.C6S0WakeupEnergy;
			 C6S3WakeupEnergy += uServer.C6S3WakeupEnergy;
			
			productiveEnergy += uServer.productiveEnergy;
		}

		// fanyao added: print out idle distribution
		File eDistributionFile = new File("energy_breakdown");
		String fileName = this.fileCreatTime + ".txt";
		try {
			
			//use append mode
			FileWriter eDistributionFileWriter = null;
			if(!eDistributionFile.exists()){
				
				eDistributionFileWriter = new FileWriter(eDistributionFile, true);
			//temp code, only favor for alwayson, delaydoze and dualdelay experiments
				eDistributionFileWriter.write(String.format("%-20s", "totalEnergy"));
				eDistributionFileWriter.write(String.format("%-20s", "C0S0IdleEnergy"));
				eDistributionFileWriter.write(String.format("%-20s","C6S0IdleEnergy"));
				eDistributionFileWriter.write(String.format("%-20s","C6S0WakeupEnergy"));
				eDistributionFileWriter.write(String.format("%-20s","C6S3IdleEnergy"));
				eDistributionFileWriter.write(String.format("%-20s","C6S3WakeupEnergy"));
				eDistributionFileWriter.write(String.format("%-20s","productiveEnergy"));
				eDistributionFileWriter.write("\n");
			}else{	
			    eDistributionFileWriter = new FileWriter(eDistributionFile, true);
			}
			    eDistributionFileWriter.write(String.format("%-20.0f", totalEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", C0S0IdleEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", C6S0IdleEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", C6S0WakeupEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", C6S3IdleEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", C6S3WakeupEnergy));
				eDistributionFileWriter.write(String.format("%-20.0f", productiveEnergy));
				eDistributionFileWriter.write("\n");
				
				Sim.log(fileName, "EnergyDistribution:" +
						String.format("%.0f", totalEnergy) + ":" +
						String.format("%.0f", C0S0IdleEnergy) + ":" + 
						String.format("%.0f", C6S0IdleEnergy) + ":" +
						String.format("%.0f", C6S0WakeupEnergy) + ":" +
						String.format("%.0f", C6S3IdleEnergy) + ":" +
						String.format("%.0f", C6S3WakeupEnergy) + ":" + 
						String.format("%.0f", productiveEnergy) + ":"
						);
			

			eDistributionFileWriter.flush();
			eDistributionFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected void dumpDCStateDurations() {
		int numOfServers = this.getExpConfig().getServersToSchedule();
		Vector<Server> servers = this.getDataCenter().getServers();
		Map<Integer, Double> dcStateDurations = null;
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			Map<Integer, Double> serverStateDuration = sServer
					.getStateDurations();
			if (dcStateDurations == null) {
				dcStateDurations = serverStateDuration;
			} else {
				for (Map.Entry<Integer, Double> oneState : serverStateDuration.entrySet()) {
					int key = oneState.getKey();
					double duration = oneState.getValue();
					dcStateDurations.put(key, dcStateDurations.get(key)+ duration);
				}
			}
		}

		// fanyao added: print out idle distribution
		File durationFile = new File("stateDuration");
		String fileName = this.fileCreatTime + ".txt";

		try {
			FileWriter durationFileWriter = new FileWriter(durationFile);
			Sim.log_no_n(fileName, "\nStateDuration:");
			for (Map.Entry<Integer, Double> oneState : dcStateDurations
					.entrySet()) {

				String keyName = "";

				switch (oneState.getKey()) {
				case 0:
					keyName = "service_time";
					break;
				case 1:
					keyName = "C0S0";
					break;
				case 2:
					keyName = "C1S0";
					break;
				case 3:
					keyName = "C3S0";
					break;
				case 4:
					keyName = "C6S0";
					break;
				case 5:
					keyName = "C6S3";
					break;
				case 6:
					keyName = "off_time";
					break;
				case -1:
					keyName = "wakeup_time";
					break;
				default:

				}
				durationFileWriter.write(String.format("%-15s", keyName));
				durationFileWriter.write(String.format("%-20.5f",
						oneState.getValue()));
				Sim.log_no_n(fileName,  String.format("%.5f",
						oneState.getValue()) + ":");
				durationFileWriter.write("\n");
			}
			durationFileWriter.flush();
			durationFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected void serializeResult(){
		System.out.print("Serialize Results\n");
		File file = new File(statsFileName);
		FileWriter fw;
		String fileName = this.fileCreatTime + ".txt";
	
		 //write statistics to file
		try {

			if (!file.exists()) {
				// use append mode
				fw = new FileWriter(new File(statsFileName), true);
				
				String fieldString = "";
				
				for(Map.Entry<String, String> entry : resultFieldValueMap.entrySet()){
					fieldString += entry.getKey();
				}
				Sim.log(fileName, fieldString);
				fw.write(fieldString);
				fw.write("\n");
			} else {
				fw = new FileWriter(file, true);
			}
			
			String valueString = "";
			for(Map.Entry<String, String> entry : resultFieldValueMap.entrySet()){
				valueString += entry.getValue();
			}
			Sim.log(fileName, "SerialResults " + valueString);
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
	protected void formulateResults() {
		
		resultFieldValueMap.put(String.format("%-15s", "date"), String.format("%-15s", fileCreatTime));
		resultFieldValueMap.put(String.format("%-10s", "numOfJobs"), String.format("%-10d", mExpConfig.getNumOfJobs()));
		resultFieldValueMap.put(String.format("%-10s", "rou"), String.format("%-10.2f", mExpConfig.getRou()));
		resultFieldValueMap.put(String.format("%-10s", "uBar"), String.format("%-10.5f", mExpConfig.getuBar()));
		resultFieldValueMap.put(String.format("%-15s", "arrival"), String.format("%-15s", mExpConfig.getJobWorkload().name()+ "_" + mExpConfig.getServiceTimeDist().name()));
		resultFieldValueMap.put(String.format("%-10s", "speed"), String.format("%-10.2f", mExpConfig.getSpeed()));
		resultFieldValueMap.put(String.format("%-15s", "numOfServers"), String.format("%-15d", mExpConfig.getServersToSchedule()));
		resultFieldValueMap.put(String.format("%-20s", "executionTime(s)"), String.format("%-20.2f", this.simulationTime));
		resultFieldValueMap.put(String.format("%-20s", "averageServiceT(s)"), String.format("%-20.5f", this.getAverageServiceTime()));
		resultFieldValueMap.put(String.format("%-20s", "averageLatency(s)"), String.format("%-20.5f", averageLatencies.get(0)));
		resultFieldValueMap.put(String.format("%-20s", "NomalizedLatency"), String.format("%-20.2f", normalizedLatencies.get(0)));
		resultFieldValueMap.put(String.format("%-15s", "totalEnergy(J)"), String.format("%-15.0f", totalEnergy));
		resultFieldValueMap.put(String.format("%-20s", "averagePower(W)"), String.format("%-20.0f", totalEnergy / this.simulationTime));
//		resultFieldValueMap.put(String.format("%-15s", "percentile50"), String.format("%-15s", String.format("%3.1f%%->", 50.0)
//				+ String.format("%.2f", percentileLatency50s.get(0))));
//		resultFieldValueMap.put(String.format("%-20s", "percentile" + mExpConfig.getLatencyPercent()*100), String.format("%-20s",String.format("%3.1f%%->", mExpConfig.getLatencyPercent() * 100)
//			    + String.format("%.2f", percentileLatencys.get(0))));
//		resultFieldValueMap.put(String.format("%-20s", "percentile95"), String.format("%-20s", String.format("%3.1f%%->", 95.0)
//				+ String.format("%.2f", percentileLatency95s.get(0))));
//		resultFieldValueMap.put(String.format("%-20s", "percentile99"), String.format("%-20s", String.format("%3.1f%%->", 99.0)
//				+ String.format("%.2f", percentileLatency99s.get(0))));
//		resultFieldValueMap.put(String.format("%-22s", "abspercentile90th(s)"), String.format("%-22s", String.format("%3.1f%%->", 90.0)
//				+ String.format("%.5f", absPercentileLatency90s.get(0))));
//		resultFieldValueMap.put(String.format("%-22s", "abspercentile95th(s)"), String.format("%-22s", String.format("%3.1f%%->", 95.0)
//				+ String.format("%.5f", absPercentileLatency95s.get(0))));
		resultFieldValueMap.put(String.format("%-15s", "percentile50"), String.format("%-15s", String.format("%.2f", percentileLatency50s.get(0))));
		resultFieldValueMap.put(String.format("%-20s", "percentile" + mExpConfig.getLatencyPercent()*100), String.format("%-20.2f", percentileLatencys.get(0)));
		resultFieldValueMap.put(String.format("%-20s", "percentile95"), String.format("%-20.2f", percentileLatency95s.get(0)));
		resultFieldValueMap.put(String.format("%-20s", "percentile99"), String.format("%-20.2f", percentileLatency99s.get(0)));
		resultFieldValueMap.put(String.format("%-22s", "abspercentile90th(s)"), String.format("%-22.2f", absPercentileLatency90s.get(0)));
		resultFieldValueMap.put(String.format("%-22s", "abspercentile95th(s)"), String.format("%-22.2f", absPercentileLatency95s.get(0)));
		resultFieldValueMap.put(String.format("%-15s", "sleepstateMode"), String.format("%-15s", sleepStateMode));
		
		

		// used to log critical experiment result statistics
//		File file = new File(statsFileName);
//		FileWriter fw;
//		double speed = dataCenter.getExpConfig().getSpeed();

		
//		// write statistics to file
//		try {
//
//			if (!file.exists()) {
//				// use append mode
//				fw = new FileWriter(new File(statsFileName), true);
//				fw.write(String.format("%-10s", "date")
//						+ String.format("%-15s", "numOfJobs")
//						+ String.format("%-15s", "rou")
//						+ String.format("%-15s", "uBar")
//						+ String.format("%-15s", "speed")
//						+ String.format("%-15s", "sleepstateMode")
//						+ String.format("%-15s", "numOfServers")
//						+ String.format("%-15s", "executionTime")
//						+ String.format("%-20s", "averageServiceT")
//						+ String.format("%-20s", "averageLatency")
//						+ String.format("%-20s", "NomalizedLatency")
//						+ String.format("%-15s", "totalEnergy")
//						+ String.format("%-15s", "averagePower")
//						+ String.format("%-15s", "percentile")
//						+ String.format("%-15s", "percentile95")
//						+ String.format("%-15s", "percentile99"));
//				fw.write("\n");
//			} else {
//				fw = new FileWriter(file, true);
//			}
//
//			fw.write(String.format("%-15s", fileCreatTime)
//					+ String.format("%-15d", mExpConfig.getNumOfJobs())
//					+ String.format("%-15.2f", mExpConfig.getRou())
//					+ String.format("%-15.2f", mExpConfig.getuBar())
//					+ String.format("%-15.2f", speed)
//					+ String.format("%-15s", sleepStateMode)
//					+ String.format("%-15d", mExpConfig.getServersToSchedule())
//					+ String.format("%-15.2f", this.simulationTime)
//					+ String.format("%-20.5f", this.getAverageServiceTime())
//					+ String.format("%-20.5f", averageLatencies.get(0))
//					+ String.format("%-20.5f", normalizedLatencies.get(0))
//					+ String.format("%-15.2f", totalEnergy)
//					+ String.format("%-15.2f", totalEnergy
//							/ this.simulationTime)
//					+ String.format("%-15s",String.format("%3.1f%%->", mExpConfig.getLatencyPercent() * 100)
//						    + String.format("%.2f", percentileLatencies.get(0)))
//					+ String.format("%-15s", String.format("%3.1f%%->", 95.0)
//							+ String.format("%.2f", percentileLatency95s.get(0)))
//					+ String.format("%-15s", String.format("%3.1f%%->", 99.0)
//							+ String.format("%.2f", percentileLatency99s.get(0))));
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


	
	/**
	 * Writes statistics about the experiment and statistics on each of the servers
	 * to a new log file
	 */
	protected void writeAllServerStats() {

		System.out.print("dump All Server details: " + mExpConfig.doCollectServerDetails());
		File file = new File(this.statsFileName + "_detail");
		FileWriter fw;

		String fileName = this.fileCreatTime + ".txt";
		// Core theCore = server.getCore();
		// write statistics to file
		try {

			// use append mode
			fw = new FileWriter(file, true);
			fw.write("************************************************************************\n");
			Sim.log(fileName, String.format("%-20s", "date")
					+ String.format("%-20s", "numOfJobs")
					+ String.format("%-20s", "rou")
					+ String.format("%-20s", "uBar")
					+ String.format("%-20s", "speed")
					+ String.format("%-20s", "sleepstateMode")
					+ String.format("%-20s", "numOfServers")
					+ String.format("%-20s", "executionTime")
					+ String.format("%-20s", "averageServiceT")
					+ String.format("%-20s", "averageLatency")
					+ String.format("%-20s", "nomalizedLatency")
					+ String.format("%-20s", "totalEnergy")
					+ String.format("%-20s", "averagePower"));
			fw.write(String.format("%-20s", "date")
					+ String.format("%-20s", "numOfJobs")
					+ String.format("%-20s", "rou")
					+ String.format("%-20s", "uBar")
					+ String.format("%-20s", "speed")
					+ String.format("%-20s", "sleepstateMode")
					+ String.format("%-20s", "numOfServers")
					+ String.format("%-20s", "executionTime")
					+ String.format("%-20s", "averageServiceT")
					+ String.format("%-20s", "averageLatency")
					+ String.format("%-20s", "nomalizedLatency")
					+ String.format("%-20s", "totalEnergy")
					+ String.format("%-20s", "averagePower"));
			fw.write("\n");

			Sim.log(fileName,
					String.format("%-20s", fileCreatTime)
					+ String.format("%-20d", mExpConfig.getNumOfJobs())
					+ String.format("%-20.2f", mExpConfig.getRou())
					+ String.format("%-20.5f", mExpConfig.getuBar())
					+ String.format("%-20s", mExpConfig.getSpeed())
					+ String.format("%-20s", "shallow")
					+ String.format("%-20d", mExpConfig.getServersToSchedule())
					+ String.format("%-20.5f", this.simulationTime)
					+ String.format("%-20.5f", this.getAverageServiceTime())
					+ String.format("%-20.5f", averageLatencies.get(0))
					+ String.format("%-20.5f", normalizedLatencies.get(0))
					+ String.format("%-20.5f", totalEnergy)
					+ String.format("%-20.5f", totalEnergy
							/ this.simulationTime));
			fw.write(String.format("%-20s", fileCreatTime)
					+ String.format("%-20d", mExpConfig.getNumOfJobs())
					+ String.format("%-20.2f", mExpConfig.getRou())
					+ String.format("%-20.5f", mExpConfig.getuBar())
					+ String.format("%-20s", mExpConfig.getSpeed())
					+ String.format("%-20s", "shallow")
					+ String.format("%-20d", mExpConfig.getServersToSchedule())
					+ String.format("%-20.5f", this.simulationTime)
					+ String.format("%-20.5f", this.getAverageServiceTime())
					+ String.format("%-20.5f", averageLatencies.get(0))
					+ String.format("%-20.5f", normalizedLatencies.get(0))
					+ String.format("%-20.5f", totalEnergy)
					+ String.format("%-20.5f", totalEnergy
							/ this.simulationTime));
			fw.write("\n");

			Sim.log(fileName,
					String.format("%-20s", "serverNo")
					+ String.format("%-20s", "finishedTasks")
					+ String.format("%-20s", "serviceTime")
					+ String.format("%-20s", "idleTime")
					+ String.format("%-20s", "wakeupTime")
					+ String.format("%-20s", "energy"));
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
			graphServerResults();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public double dumpIdleDistributions() {
		// fanyao added: print out idle distribution
		File idleDisFile = new File("idle_distribution");
		double totalIdleTime = 0.0;
		try {
			FileWriter idleFileWriter = new FileWriter(idleDisFile);
			Vector<UniprocessorServer> allServers = ((MultiServerScheduler) taskScheduler)
					.getControlledServers();

			idleFileWriter.write(String.format("%-10s", "startTime")
					+ String.format("%-10s", "endTime")
					+ String.format("%-10s", "sleepState\n"));
			for (UniprocessorServer aServer : allServers) {
				idleFileWriter
						.write("server id: " + aServer.getNodeId() + "\n");
				for (IdleRecord idleRecord : aServer.getIdleDistribution()) {
					idleFileWriter.write(String.format("%-10.5f",
							idleRecord.startIdleTime));
					idleFileWriter.write("\t");
					idleFileWriter.write(String.format("%-10.5f",
							idleRecord.duration + idleRecord.startIdleTime));
					idleFileWriter.write("\t");
					idleFileWriter.write(String.format("%d",
							idleRecord.sleepstate));

					idleFileWriter.write("\n");

					// accumulate total idle time
					totalIdleTime += idleRecord.duration;
				}

				idleFileWriter.write("\n");

			}

			idleFileWriter.flush();
			idleFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return totalIdleTime;
	}

	@Override
	public void initialize() {
		super.initialize();
		globalQueue = ((MultiServerScheduler) taskScheduler).getGlobalQueue();
		dumpActualServerHis = mExpConfig.doCollectActServerHis();

		if (mExpConfig.isAsynLogging()) {
			// this.actualActiveServerHis = new AsynLoggedVector(Pair<Double,
			// Integer>)();
			this.actualActiveServerHis = new AsynLoggedVector<Pair<Float, Integer>>(
					actualServerHisName);
			this.registerLoggedVectors((AsynLoggedVector<?>) actualActiveServerHis);
		} else {
			this.actualActiveServerHis = new Vector<Pair<Float, Integer>>();
		}

		// in multi-server experiment, all the servers are always set to idle

		updateServerHis(0.0, 0);

	}

	public void updateServerHis(double time, int delta) {
		// if no need to dump actual server history, do not collect them
		if (!dumpActualServerHis)
			return;

		Pair<Float, Integer> newHis = new Pair<Float, Integer>();

		if (actualActiveServerHis.size() == 0) {
			newHis.setFirst((float) time);
			newHis.setSecond(delta);

		} else {
			Pair<Float, Integer> lastHis = actualActiveServerHis.lastElement();
			newHis.setFirst((float) time);
			newHis.setSecond(lastHis.getSecond() + delta);
		}

		actualActiveServerHis.add(newHis);
	}

	public void dumpActualServerHis() {

		// no dump needed, in order to save space

		File queueFile = new File(actualServerHisName);
		
		try {
			FileWriter fw = new FileWriter(queueFile, false);
			BufferedWriter bw = new BufferedWriter(fw);
			// bw.write(String.format("%-15s", "time")
			// + String.format("%-15s", "actual_server_num"));
			// bw.newLine();

			// write history to file
			for (Pair<Float, Integer> aPair : actualActiveServerHis) {
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

	@Override
	public String printStateofServers() {
		// TODO Auto-generated method stub
		return null;
	}

}
