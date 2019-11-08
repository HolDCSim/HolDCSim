package experiment;

import infrastructure.DataCenter;
import infrastructure.UniprocessorServer;
import stochastic.ExponentialRandom;
import utility.Pair;
import utility.ServerPowerMeter;
import workload.MixedWorkloadGen;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;
import java.io.*;
import job.Task;

public class SleepScaleExperiment extends SingletonJobExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the single server in data center for experiment
	private UniprocessorServer mServer;
	
	/************************************************************/
	// statistics for single sleep state experiment single server
	/**
	 * aggregate time when processor is active this would also include the sleep state
	 * wakeup time
	 */
	private double activeTime;

	/**
	 * aggregate time when the processor is in low power state
	 */
	private double idleTime;

	/**
	 * aggregate time when the processor is actually processing jobs
	 */
	private double serviceTime;

	/**
	 * aggregate low power state wakeup time
	 */
	private double wakeupTime;

	/**
	 * aggregate low power state wakeup time
	 */
	private double jobProcessingTime;

	/**
	 * the number of time processor in idle period
	 */
	private int idlePeriods;

	/*******************************************************/
	
	/**************************************************************/
	// statistics for multiple sleepstates experiments single server
	private double C0S0Time = 0.0;
	private double C1S0Time = 0.0;
	private double C3S0Time = 0.0;
	private double C6S0Time = 0.0;
	private double C6S3Time = 0.0;

	private int C0S0Count = 0;
	private int C1S0Count = 0;
	private int C3S0Count = 0;
	private int C6S0Count = 0;
	private int C6S3Count = 0;

	/**************************************************************/

	public SleepScaleExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
		
		this.activeTime = 0.0;
		this.idleTime = 0.0;
		this.serviceTime = 0.0;
		this.wakeupTime = 0.0;
		// TODO Auto-generated constructor stub
	}

	public  double getWakeupTime() {
		return mServer.getWakeupTime();
	}

	public void setSingleServer(UniprocessorServer server) {
		this.mServer = server;
	}

	@Override
	public void setDataCenter(DataCenter dc) {
		super.setDataCenter(dc);
		this.mServer = (UniprocessorServer) dc.getServers().get(0);
	}

	public void updateServieTimeStats(Task task) {
		// TODO Auto-generated method stub
		this.serviceTime += task.getFinishTime() - task.getStartExecutionTime();
		
	}
	@Override
	protected void initialServerType() {
		this.serverType = ServerType.UNIPROCESSOR_SERVER;
	}
	
	public double dumpIdleDistributions() {
		// fanyao added: print out idle distribution
		File idleDisFile = new File("idle_distribution");
		double totalIdleTime = 0.0;
		try {
			FileWriter idleFileWriter = new FileWriter(idleDisFile);
			for (IdleRecord idleRecord : mServer.getIdleDistribution()) {
				idleFileWriter.write(Double.toString(idleRecord.startIdleTime));
				idleFileWriter.write("\t");
				idleFileWriter.write(Double.toString(idleRecord.duration));
				idleFileWriter.write("\n");

				// accumulate total idle time
				totalIdleTime += idleRecord.duration;
			}

			idleFileWriter.flush();
			idleFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return totalIdleTime;
	}
	
	@Override
	public void dumpEnergyPerfStats() {

		// each experiment calculates its own power and energy
		double power = 0.0;
		double energy = 0.0;

		/** statistics for nonRamdom experiment **/
		// calculate energy and power
		double wakeupEnergy = 0.0;
		double idleEnergy = 0.0;
		double productiveEnergy = 0.0;

		// if (!useMultipleSS) {
		/* double tempActiveTime = 0.0; */
		// now calculate the mean response time for all the jobs

		// used to log critical experiment result statistics
		File file = new File(statsFileName);
		FileWriter fw;

		/**************************************************************************
		//this.serviceTime should be accumulated in TaskFinishedEvent
		// double tempServiceTime = 0.0;
		for (TaskSLRecord slRecord : taskSizeLatencies) {
			Vector<Task> tasksInJob = job.getAllTasks();
			if (tasksInJob == null) {
				Sim.fatalError("err, no tasks in one job");
			}

			// fanyao comments: calculate the total service time
			Task task = tasksInJob.get(0);

			// tempServiceTime += task.getSize()
			// / dataCenter.getExpConfig().getSpeed();
			this.serviceTime += task.getFinishTime()
					- task.getStartExecutionTime();

		}
		**************************************************************************/
		// System.out.println(tempServiceTime + "\t" + this.serviceTime);

		// the time before the first jobs arrives is also considered as
		// active, so it needs to be added
		this.idlePeriods = mServer.getIdleDistribution().size() - 1;
		this.wakeupTime = this.getWakeupTime();
		this.activeTime = this.serviceTime + wakeupTime;
		this.idleTime = this.simulationTime - this.activeTime;
		// this.wakeupTime = idlePeriods
		// * Core.sleepStateWakeups[exp.getSleepState() - 1];
		this.jobProcessingTime = activeTime - wakeupTime;

		System.out.println(String.format("%-20s", "idle time") + idleTime);
		System.out.println(String.format("%-20s", "active time") + activeTime);

		// use to denote a fixed sleepstate or multiple sleep state

		// // statistics for single sleep state experiment
		// if (!useMultipleSS) {
		//
		// double speed = dataCenter.getExpConfig().getSpeed();
		// int sleepState = dataCenter.getExpConfig().getSleepState();
		// sleepStateMode = Integer.toString(sleepState);
		//
		// wakeupEnergy = getWakeupTime()
		// * PowerCalculator.calculateActivePower(speed);
		// idleEnergy = idleTime
		// * PowerCalculator.calculateLowPower(speed, sleepState);
		// productiveEnergy = jobProcessingTime
		// * PowerCalculator.calculateActivePower(speed);
		//
		// energy = activeTime * PowerCalculator.calculateActivePower(speed)
		// + idleTime
		// * PowerCalculator.calculateLowPower(speed, sleepState);
		// power = energy / simulationTime;
		//
		// }

		// additional statistics for experiment with multiple sleep states
		// else {

		double speed = dataCenter.getExpConfig().getSpeed();
		ExperimentConfig expConfig = dataCenter.getExpConfig();

		int fixedSleepState = dataCenter.getExpConfig().getInitialSleepState();

		if (!useMultipleSS)
			sleepStateMode = Integer.toString(fixedSleepState);
		else
			sleepStateMode = "mul";

		double[] sleepStateTimes = dataCenter.getSSTimeArray();
		int[] sleepStateCounts = dataCenter.getSSCountsArray();

		C0S0Time = sleepStateTimes[0];
		C1S0Time = sleepStateTimes[1];
		C3S0Time = sleepStateTimes[2];
		C6S0Time = sleepStateTimes[3];
		C6S3Time = sleepStateTimes[4];

		C0S0Count = sleepStateCounts[0];
		C1S0Count = sleepStateCounts[1];
		C3S0Count = sleepStateCounts[2];
		C6S0Count = sleepStateCounts[3];
		C6S3Count = sleepStateCounts[4];

		// when we use multiple sleepstates
		System.out.println("COSOTime	" + C0S0Time);
		System.out.println("C1SOTime	" + C1S0Time);
		System.out.println("C3SOTime	" + C3S0Time);
		System.out.println("C6SOTime	" + C6S0Time);
		System.out.println("C6S3Time	" + C6S3Time);

		// how many times in each states
		System.out.println("COSOCount	" + C0S0Count);
		System.out.println("C1SOCount	" + C1S0Count);
		System.out.println("C3SOCount	" + C3S0Count);
		System.out.println("C6SOCount	" + C6S0Count);
		System.out.println("C6S3Count	" + C6S3Count);

		dumpIdleDistributions();
		// this.activeTime = this.simulationTime - this.idleTime;

		// System.out.println("time in active " + activeTime);
		// System.out.println("time in idle " + idleTime);
		// System.out.println("time in idle again" + tidleTime);

		power = 0.0;
		// double speed = exp.getSpeed();
		wakeupEnergy = this.wakeupTime
				* ServerPowerMeter.calculateActivePower(expConfig, speed);
		idleEnergy = sleepStateTimes[0]
				* ServerPowerMeter.calculateLowPower(expConfig, speed, 1)
				+ sleepStateTimes[1]
				* ServerPowerMeter.calculateLowPower(expConfig, speed, 2)
				+ sleepStateTimes[2]
				* ServerPowerMeter.calculateLowPower(expConfig, speed, 3)
				+ sleepStateTimes[3]
				* ServerPowerMeter.calculateLowPower(expConfig, speed, 4)
				+ sleepStateTimes[4]
				* ServerPowerMeter.calculateLowPower(expConfig, speed, 5);

		productiveEnergy = (this.activeTime - this.getWakeupTime())
				* ServerPowerMeter.calculateActivePower(expConfig, speed);
		energy = productiveEnergy + idleEnergy + wakeupEnergy;

		power = energy / this.simulationTime;
		System.out.println("average power: " + power);
		// }

		// write statistics to file
		try {

			if (!file.exists()) {
				// use append mode
				fw = new FileWriter(new File(statsFileName), true);
				fw.write("date" + String.format("%15s", "rou")
						+ String.format("%15s", "uBar")
						+ String.format("%15s", "speed")
						+ String.format("%15s", "sleepstate")
						+ String.format("%20s", "averageServiceT")
						+ String.format("%15s", "executiontime")
						+ String.format("%15s", "wakeupTime")
						+ String.format("%20s", "jobProcessingTime")
						+ String.format("%15s", "activeTime")
						+ String.format("%15s", "idleTime")
						+ String.format("%15s", "idlePeriods")
						+ String.format("%20s", "averageLatency")
						+ String.format("%15s", "totalEnergy")
						+ String.format("%15s", "idleEnergy")
						+ String.format("%15s", "wakeupEnergy")
						+ String.format("%15s", "productiveEnergy")
						+ String.format("%15s", "C0S0Time")
						+ String.format("%15s", "C1S0Time")
						+ String.format("%15s", "C3S0Time")
						+ String.format("%15s", "C6S0Time")
						+ String.format("%15s", "C6S3Time"));
				fw.write("\n");
			} else {
				fw = new FileWriter(file, true);
			}

			// fw.write(fileCreatTime + "\t" + exp.getRou() + "\t"
			// + exp.getuBar() + "\t" + exp.getSpeed() + "\t"
			// + exp.getSleepState() + "\t" + this.simulationTime
			// + "\t" + this.wakeupTime + "\t"
			// + this.jobProcessingTime + "\t" + activeTime + "\t"
			// + idleTime + "\t" + idlePeriods + "\t"
			// + averageLatency + "\t" + normalizedLatency + "\t"
			// + energy + "\t" + power);

			if (!(mWorkloadGen instanceof MixedWorkloadGen)) {

				fw.write(fileCreatTime + "\t" + mExpConfig.getRou() + "\t"
						+ mExpConfig.getuBar() + "\t" + mExpConfig.getSpeed()
						+ "\t" + sleepStateMode + "\t"
						+ this.getAverageServiceTime() + "\t"
						+ this.simulationTime + "\t" + this.getWakeupTime()
						+ "\t" + this.jobProcessingTime + "\t" + activeTime
						+ "\t" + idleTime + "\t" + idlePeriods + "\t"
						+ averageLatencies.get(0) + "\t" + energy + "\t" + idleEnergy
						+ "\t" + wakeupEnergy + "\t" + productiveEnergy + "\t"
						+ C0S0Time + "\t" + C1S0Time + "\t" + C3S0Time + "\t"
						+ C6S0Time + "\t" + C6S3Time);
			}

			else {
				fw.write(fileCreatTime + "\t" + mExpConfig.getRou() + "\t"
						+ "mixed"
						+ ((MixedWorkloadGen) mWorkloadGen).getProportion()
						+ "\t" + mExpConfig.getSpeed() + "\t" + sleepStateMode
						+ "\t" + this.getAverageServiceTime() + "\t"
						+ this.simulationTime + "\t" + this.getWakeupTime()
						+ "\t" + this.jobProcessingTime + "\t" + activeTime
						+ "\t" + idleTime + "\t" + idlePeriods + "\t"
						+ averageLatencies.get(0) + "\t" + energy + "\t" + idleEnergy
						+ "\t" + wakeupEnergy + "\t" + productiveEnergy + "\t"
						+ C0S0Time + "\t" + C1S0Time + "\t" + C3S0Time + "\t"
						+ C6S0Time + "\t" + C6S3Time);
			}

			fw.write("\n");
			fw.flush();
			fw.close();

			// write idle distributions to file
			// dumpIdleDistributions();
			dumpJobArrDist();
			dumpJobCmpDist();
			// dumpQueuedJobsStats();
			// dumpServiceTimeDist();
			// dump mmpp on/off history
			dumpMMPPHistory();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// double energyTest =
		// ((SingleCoreServer)dataCenter.getServers().get(0)).getEnergy();

		System.out.println(String.format("%-20s", "average latency")
				+ averageLatencies.get(0));
		System.out.println(String.format("%-20s", "normalized latency")
				+ normalizedLatencies.get(0));
		System.out
				.println(String.format("%-20s", "idle periods") + idlePeriods);
		System.out.println(String.format("%-20s", "energy") + energy);
		System.out.println(String.format("%-20s", "power") + power);

	}
	
	public void dumpQueuedJobsStats() {
		// file format: column 1: time, column 2: number of jobs in queue
		try {
			FileWriter queuedJobsFile = new FileWriter(new File(
					"queued_jobs_stats"));
			for (Pair<Double, Integer> pair : mServer.getQueuedJobStats()) {
				queuedJobsFile.write(Double.toString(pair.getFirst()) + "\t"
						+ Integer.toString(pair.getSecond()));
				queuedJobsFile.write("\n");
			}

			queuedJobsFile.flush();
			queuedJobsFile.close();

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
