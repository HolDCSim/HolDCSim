package experiment;

import infrastructure.DataCenter;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import constants.Constants;
import beans.TaskSLRecord;
import job.Job;
import job.Task;
import debug.Sim;
import scheduler.OneServerScheduler;
import stochastic.ExponentialRandom;
import utility.Pair;
import utility.ServerPowerMeter;
import workload.AbstractWorkloadGen;
import workload.MMPPWorkloadGen;
import workload.MixedWorkloadGen;
import workload.TraceWorkloadGen;
import event.ExperimentInput;
import event.ExperimentOutput;
import event.RJobArrivalEvent;
import experiment.ExperimentConfig.ServerType;

/**
 * @author fan SingletonJobExperiment considers singleton job and global queue 
 * for centralized control. Data center size related statistics not included in
 * this class.
 */
public abstract class SingletonJobExperiment extends Experiment {

	/**
	 * default serial number
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @author fan job arrival workloads
	 * 
	 */
	public enum JobWorkload {
		POISSON, MMPP, MIXED, MMPPFLU, TRACE, MMPPORAC
	}

	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/*variables migrated from Experiment class*/
	// queue related statistics
	public Vector<Float> jobArrivalsStats;
	public Vector<Float> jobCompletedStats;
	
	/**
	 * average size of finished jobs, actually this means average task size
	 * since this are all singleton-job
	 */
	protected double averageFinishedJobSize;
	
	public boolean doCollectJobArrivals;
	public boolean doCollectJobCompletes;
	
	//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	
	/**
	 * number jobs generated randomly
	 */
	protected int jobsGenerated;
	/**
	 * number of jobs that have been finished
	 */

	/******************************************************/

	// public Vector<Pair<Double, Integer>> queuedJobsStats;

	// public int curJobsInQueue;
	/******************************************************/

	// sleep states enforcement option
	protected boolean useMultipleSS;

	// job workload generator
	protected AbstractWorkloadGen mWorkloadGen;

	protected String sleepStateMode;
	
	public int numOfCores;
	
	public Constants.SleepUnit sleepUnit;
	
	protected TaskSLRecord[] taskSizeLatencies;
	
	protected Map<Integer, Double> averageLatencies;
	
	protected Map<Integer, Double> normalizedLatencies;
	
	protected Map<Integer, Double> percentileLatencys;
	
	protected Map<Integer, Double> percentileLatency50s;
	
	//protected Map<Integer, Double> percentileLatency90s;

	protected Map<Integer, Double> percentileLatency95s;

	protected Map<Integer, Double> percentileLatency99s;

	protected Map<Integer, Double> absAverageServiceTimes;
	
	/**90th percentile based on absolute latency in seconds*/
	protected Map<Integer, Double> absPercentileLatency90s;
	
	/**95th percentile based on absolute latency in seconds*/
	protected Map<Integer, Double> absPercentileLatency95s;

	
	// protected MaxMinPercentile lLatencyPercentile;
	// protected MaxMinPercentile rLatencyPercentile;

	// protected double latencyPercent;

	// public Job job;

	public SingletonJobExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		this.jobsGenerated = 0;
		this.jobsFinished = 0;
		this.averageFinishedJobSize = 0.0;
		this.statsFileName = "SS_stats";
		this.useMultipleSS = expConfig.isUseMultipleSS();
		
		this.percentileLatencys = new HashMap<Integer, Double>();
		this.percentileLatency50s = new HashMap<Integer, Double>();
		this.percentileLatency95s = new HashMap<Integer, Double>();
		this.percentileLatency99s = new HashMap<Integer, Double>();
		this.absPercentileLatency90s = new HashMap<Integer, Double>();
		this.absPercentileLatency95s = new HashMap<Integer, Double>();
		this.averageLatencies = new HashMap<Integer, Double>();

		this.normalizedLatencies = new HashMap<Integer, Double>();
		this.absAverageServiceTimes = new HashMap<Integer, Double>();

		//this.useRandomWorkload = true;

		//this.lLatencyPercentile = new MaxMinPercentile();
		//this.rLatencyPercentile = new MaxMinPercentile();

		// sleepscale experiment only use one single server
		// if (expConfig.getServerToSchedule() != 1) {
		// Sim.fatalError("fatal error: server to schedule should be 1");
		// }

		// interArrivalGen = new ExponentialRandom(lamdba, 10000);
		// serviceTimeGen = new ExponentialRandom(u, 20000);
		// mWorkloadGen = new SSWorkloadGen(lamdba, u);
		// mWorkloadGen = new MMPPWorkloadGen(lamdba, u);
		// mWorkloadGen = new MixedWorkloadGen(rou);

		// // parse the args passed from command line
		// parseCmdParams(tokens);

		// mServer.setIdleDistribution(new Vector<IdleRecord>());
		/*
		 * if (expConfig.isSeedFixed()) { interArrivalGen = new
		 * ExponentialRandom(lamdba, expConfig.getSsSeed()); serviceTimeGen =
		 * new ExponentialRandom(u, expConfig.getSsSeed() + 10000);
		 * 
		 * // interArrivalGen = new ExponentialRandom(lamdba, 10000); //
		 * serviceTimeGen = new ExponentialRandom(u, 20000); }
		 * 
		 * else{ interArrivalGen = new ExponentialRandom(lamdba,
		 * System.currentTimeMillis()); serviceTimeGen = new
		 * ExponentialRandom(u, System.currentTimeMillis() + 100); }
		 */

	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {

		super.parseCmdParams(argParser);
		
		argParser.addOption("rou", true, "rou");
		argParser.addOption("ubar", true, "uBar");
		argParser.addOption("speed", true, "speed");
		argParser.addOption("sleepstate", true, "sleep state");
		argParser.addOption("sleepunitparam", true, "sleep unit param");
		argParser.addOption("numcores", true, "number of cores");

		if(argParser.parse("rou") == null || argParser.parse("ubar") == null || argParser.parse("speed") == null || argParser.parse("sleepstate") == null || argParser.parse("sleepunitparam") == null || argParser.parse("numcores") == null) {
			Sim.fatalError("SingletonJobExperiment requires rou, uBar, speed, sleep state, sleep unit param, and number of cores params");
		}

		// utilization rate
		double rou = Double.parseDouble(argParser.parse("rou"));

		// service rate
		double uBar = Double.parseDouble(argParser.parse("ubar"));

		// speed
		double speed = Double.parseDouble(argParser.parse("speed"));

		// sleep states : 1 -> C0S0; 2-> C1S0 ; 3-> C3S0 ; 4-> C6S0; 5-> C6S3
		int sleepState = Integer.parseInt(argParser.parse("sleepstate"));
		
		//now parse the 5th param: SleepUnit and 6th param: numOfCores
		int sleepUnitParam = Integer.parseInt(argParser.parse("sleepunitparam"));
		if(sleepUnitParam == 0){
			sleepUnit = Constants.SleepUnit.CORE_LEVEL;
		}
		else if(sleepUnitParam == 1){
			sleepUnit = Constants.SleepUnit.SOCKET_LEVEL;
		} else if(sleepUnitParam == 2){
			sleepUnit = Constants.SleepUnit.NO_SLEEP;
		}
		else{
			Sim.fatalError("Param for sleepunit not valid: " + sleepUnitParam);
			System.exit(0);
		}
		
		numOfCores = Integer.parseInt(argParser.parse("numcores"));
		if(numOfCores <=0 ){
			Sim.fatalError("numOfCores less than 0");
			System.exit(0);
		}
		
		if(numOfCores !=1 && sleepUnit == Constants.SleepUnit.CORE_LEVEL){
			Sim.fatalError("num of cores should be 1 when using core level sleep");
			System.exit(0);
		}
		
		mExpConfig.setNumOfCores(numOfCores);
		
		if (sleepState < 1 || sleepState > 7) {
			Sim.fatalError("sleepstate invalid");
		}

		/************************************************
		//FIXME: come back to this paramter settings later (added pendingThreshold, too long and mis parsed)
		// collect taos from command line params
		if (tokens.getRemainingParams() >= 6) {
			double[] waitings = new double[6];
			for (int i = 0; i < waitings.length; i++) {
				waitings[i] = Double.parseDouble(tokens.popToken());
				mExpConfig.setTaos(waitings);
			}
		}
        ***************************************/
		// ////////////////////////////////////////////////////////////////////
		// set the parameters from command line
		// should be set before generate job workload generator
		mExpConfig.setInitialSleepState(sleepState);
		mExpConfig.setSpeed(speed);
		mExpConfig.setRou(rou);
		mExpConfig.setuBar(uBar);

	}

	public void configureServerSleepState() {
		// initialized the single sleepstate here
		Vector<Server> servers = dataCenter.getServers();

		// set multiple states
		useMultipleSS = mExpConfig.isUseMultipleSS();
		for (Server aServer : servers) {
			UniprocessorServer sServer = (UniprocessorServer) aServer;
			if (useMultipleSS)
				sServer.setUseMultipleSleepStates(true);
			else
				sServer.setNextFixedSleepState(mExpConfig.getInitialSleepState());
		}
	}

	@Override
	// creat job arrivals based on possion process
	public void getNextJob(double time) {

		jobsGenerated++;
		if (jobsGenerated > numOfJobs) {
			return;
			// AllJobsFinishEvent finishEvent = new AllJobsFinishEvent(time,
			// this);
			// this.addEvent(finishEvent);
		}

		// need to generate a new job
		else {
			double nextTime = mWorkloadGen.getNextInterArrival(time);
			double taskSize = mWorkloadGen.getNextServiceTime();
			int jobType = mWorkloadGen.getJobType();
			// double nextTime = 1.0;
			// double taskSize = 10;

			time = time + nextTime;

			Job job = new Job(dataCenter);
			job.setExperiment(this);
			job.setJobType(jobType);

			Vector<Task> tasks = new Vector<Task>();
			Task singleTask = new Task(taskSize, job);
			//singleTask.setJobType(jobType);
			singleTask.setTaskGenTime(time);
			tasks.add(singleTask);

			job.setAllTasks(tasks);
			job.setTaskDependency(null);
			
			/*******************************************************
			// no longer needs to record job
			jobs.add(job);
			*******************************************************/

			// add job arrivals to job arrival statistics
			// jobArrivalsStats[jobsGenerated - 1] = time;
			if (doCollectJobArrivals)
				jobArrivalsStats.add((float)time);
			// System.out.println(" ==> job " + job.getJobId()
			// + " interarrive at " + time + " && task size " + taskSize);

			// //fanyao comments: calcuate the total service time
			// this.serviceTime += taskSize;

			RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
			this.addEvent(jobArrival);
		}

	}

	@Override
	public void initialize() {
		// setup server sleepstate mode
		super.initialize();
		
		// get the latency percentage
		// this.latencyPercent = mExpConfig.getLatencyPercent();
		configureServerSleepState();

		// configure job workload 
		this.setJobWorkloadGen(mExpConfig.getJobWorkloadGen());
		
		//update number of jobs based on traceworkload gen
		if (mWorkloadGen instanceof TraceWorkloadGen) {
			mExpConfig.setNumOfJobs(((TraceWorkloadGen) mWorkloadGen).getTotalNumOfJobs());
			numOfJobs = mExpConfig.getNumOfJobs();
		}
		
		//begin to have multiple single-task job 
		int collectableJobs = numOfJobs - warmupJobs;
		if(collectableJobs <= 0){
			Sim.fatalError("total number of jobs less than warm up jobs!");
		}
			
		taskSizeLatencies = new TaskSLRecord[numOfJobs - warmupJobs];
		
		this.doCollectJobArrivals = mExpConfig.doCollectJobArrivals();
		this.doCollectJobCompletes = mExpConfig.doCollectJobCompletes();
		if (doCollectJobArrivals) {
			jobArrivalsStats = new Vector<Float>();
		}

		if (doCollectJobCompletes) {
			jobCompletedStats = new Vector<Float>();
		}
		

		// set global task scheduler
		// TODO Auto-generated method stub
		// ExperimentConfig.TaskSchedulerType schedulerType = mExpConfig
		// .getTaskSchedulerType();
		// if (schedulerType == null) {
		// Sim.fatalError("task scheduler not specified");
		// } else {
		// switch (mExpConfig.getTaskSchedulerType()) {
		// case SIMPLE:
		// taskScheduler = new SimpleTaskScheduler(this);
		// break;
		// case TEST_TRANS:
		// taskScheduler = new TestTransScheduler(this);
		// break;
		// case ONE_SERVER:
		// taskScheduler = new OneServerScheduler(this);
		// break;
		// case ENERGY_AWARE:
		// taskScheduler = new EnergyAwareScheduler(this);
		// break;
		// default:
		// Sim.fatalError("unknown type of scheduler!");
		//
		// }
		// }
		setTaskScheduler();
		// taskScheduler = new OneServerScheduler(this);
		// generate the first job
		this.getNextJob(0.0);
	}

	// protected void generateTaskScheduler() {
	// // TODO Auto-generated method stub
	// ExperimentConfig.TaskSchedulerType schedulerType = mExpConfig
	// .getTaskSchedulerType();
	// if (schedulerType == null) {
	// Sim.fatalError("task scheduler not specified");
	// } else {
	// switch (mExpConfig.getTaskSchedulerType()) {
	// case SIMPLE:
	// taskScheduler = new SimpleTaskScheduler(this);
	// break;
	// case TEST_TRANS:
	// taskScheduler = new TestTransScheduler(this);
	// break;
	// case ONE_SERVER:
	// taskScheduler = new OneServerScheduler(this);
	// break;
	// case ENERGY_AWARE:
	// taskScheduler = new EnergyAwareScheduler(this);
	// break;
	// default:
	// Sim.fatalError("unknown type of scheduler!");
	//
	// }
	// }
	// }

//	@Override
//	protected void setTaskScheduler() {
//		// TODO Auto-generated method stub
//		taskScheduler = new OneServerScheduler(this);
//	}


	protected void dumpMMPPHistory() {
		// dump mmpp on/off history
		/*****************************************************************/
		try {
			if (mWorkloadGen instanceof MMPPWorkloadGen) {
				ArrayList<Pair<Double, Integer>> mmppHis = ((MMPPWorkloadGen) mWorkloadGen)
						.getMMPPHis();

				File mmppHisFile = new File("mmpp_his");
				FileWriter mmppHisWriter = new FileWriter(mmppHisFile);

				for (Pair<Double, Integer> pair : mmppHis) {
					mmppHisWriter.write(Double.toString(pair.getFirst()));
					mmppHisWriter.write("\t");
					mmppHisWriter.write(Integer.toString(pair.getSecond()));
					mmppHisWriter.write("\n");
				}

				mmppHisWriter.flush();
				mmppHisWriter.close();

			}
			else{
				Sim.warning("current workload is not MMPP, not MMPP history dumped");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*****************************************************************/

	}

	public boolean isUseMultipleSS() {
		return useMultipleSS;
	}

	public void setUseMultipleSS(boolean useMultipleSS) {
		this.useMultipleSS = useMultipleSS;
	}

	public void dumpAbsServiceTimeDist() {
		
		if(taskSizeLatencies == null){
			Sim.warning("service time sequeuences not collected");
			return;
		}
		/*************************************************/
		// dump the service time statistics
		try {
			FileWriter servDisFile = new FileWriter(new File("service_time"));
			for (TaskSLRecord slRecord : taskSizeLatencies) {
				// servDisFile.write(Double.toString(job.getAllTasks().get(0)
				// .getSize()));
				servDisFile.write(Double.toString(slRecord.taskSize));
				servDisFile.write("\n");

			}

			servDisFile.flush();
			servDisFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		/****************************************************/
	}

//	public void dumpJobArrDist() {
//		// file format: column 1: time, column 2: automatically increments
//		if (jobArrivalsStats == null)
//			return;
//		try {
//			FileWriter jobArrDisFile = new FileWriter(new File(
//					"job_arrivals_stats"));
//			for (float time : jobArrivalsStats) {
//				jobArrDisFile.write(Double.toString(time));
//				jobArrDisFile.write("\n");
//			}
//
//			jobArrDisFile.flush();
//			jobArrDisFile.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void dumpJobCmpDist() {
//		// file format: column 1: time, column 2: automatically increments
//		if (jobCompletedStats == null)
//			return;
//		try {
//			FileWriter jobCmpDisFile = new FileWriter(new File(
//					"job_completed_stats"));
//			for (float time : jobCompletedStats) {
//				jobCmpDisFile.write(Double.toString(time));
//				jobCmpDisFile.write("\n");
//			}
//
//			jobCmpDisFile.flush();
//			jobCmpDisFile.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}


	public static class IdleRecord {
		public static double INITIAL = -1.0;
		public double startIdleTime = 0.0;
		public double duration = -1.0;
		public int sleepstate = -1;
	}

	public void setJobWorkloadGen(AbstractWorkloadGen abGen) {
		this.mWorkloadGen = abGen;
	}

	// @Override
	// public void setDataCenter(DataCenter dc) {
	// this.dataCenter = dc;
	//
	// }

	@Override
	public void jobFinished(Job job, double time) {
		super.jobFinished(job, time);
		
		// add to job completed statistics
		if (doCollectJobCompletes)
			jobCompletedStats.add((float)time);
		
		Task singleTask = job.getAllTasks().get(0);
		averageFinishedJobSize = ((averageFinishedJobSize * (jobsFinished - 1)) + singleTask
				.getSize()) / jobsFinished;
		Sim.debug(5, "Average finished job size " + averageFinishedJobSize);
		// Task lastFinishedTask = job.getAllTasks().get(0);
		// double normalizedLatency =
		// lastFinishedTask.getSize()/(lastFinishedTask.getFinishTime() -
		// lastFinishedTask.getArrivalTime());
		// if(normalizedLatency < lLatencyPercentile.max){
		//
		// }

	}
	
	public void collectLatencyStats() {

		// average latency and absolute latency
		// double overallDelay = 0.0;
		// Vector<Task> allTasks = new Vector<Task>();

		// print out total execution time
		// System.out.println(String.format("%-20s", "execution time")
		// + simulationTime);

		// statistics that are common to all experiments
		//
		// for (Job job : jobs) {
		// Vector<Task> tasksInJob = job.getAllTasks();
		// if (tasksInJob == null) {
		// System.err.println("err, no tasks in one job");
		// System.exit(0);
		// }
		// // add up to get overall latency
		// Task task = tasksInJob.get(0);
		// allTasks.add(task);
		// overallDelay += (task.getFinishTime() - task.getArrivalTime());
		// }
		Map<Integer, Vector<TaskSLRecord>> slRecordByTypes = new HashMap<Integer, Vector<TaskSLRecord>>();
		for (TaskSLRecord slRecord : taskSizeLatencies) {
			int jobType = slRecord.jobType;
			Vector<TaskSLRecord> slRecords = null;
			if ((slRecords = slRecordByTypes.get(jobType)) == null) {
				slRecords = new Vector<TaskSLRecord>();
				slRecordByTypes.put(jobType, slRecords);
			}
			slRecords.add(slRecord);

		}

		for (Map.Entry<Integer, Double> oneEntry : averageLatencies.entrySet()) {
			int jobType = oneEntry.getKey();
			int jobsByType = slRecordByTypes.get(jobType).size();
			oneEntry.setValue(oneEntry.getValue() / jobsByType);
			normalizedLatencies.put(oneEntry.getKey(), oneEntry.getValue());
		}

		for (Map.Entry<Integer, Double> oneEntry : absAverageServiceTimes
				.entrySet()) {
			int jobType = oneEntry.getKey();
			// int jobType = oneEntry.getKey();
			int jobsByType = slRecordByTypes.get(jobType).size();
			oneEntry.setValue(oneEntry.getValue() / jobsByType);
			double latency = normalizedLatencies.get(jobType);
			normalizedLatencies.put(jobType, latency / oneEntry.getValue());
		}

		// normalizedLatency = averageLatency
		// / dataCenter.getExpConfig().getuBar();
		// normalizedLatency = averageLatency / absAverageServiceTime;

		// test how long it takes to sort all the tasks
		double beginTime = System.currentTimeMillis();

		for (Map.Entry<Integer, Vector<TaskSLRecord>> recordsByType : slRecordByTypes
				.entrySet()) {
			int jobType = recordsByType.getKey();
			Vector<TaskSLRecord> slRecords = recordsByType.getValue();

			Collections.sort(slRecords);
			int numOfRecords = slRecords.size();

			// user defined percentile
			int percentileIndex = (int) Math.floor(numOfRecords
					* mExpConfig.getLatencyPercent());
			TaskSLRecord percentileTask = slRecords.get(percentileIndex);
			percentileLatencys.put(jobType, (percentileTask.taskLatency)
					/ percentileTask.taskSize);

			// median percentile
			percentileIndex = (int) Math.floor(numOfRecords * 0.50);
			TaskSLRecord percentileTask50 = slRecords.get(percentileIndex);
			percentileLatency50s.put(jobType, (percentileTask50.taskLatency)
					/ percentileTask50.taskSize);
						
			// 95th percentile
			percentileIndex = (int) Math.floor(numOfRecords * 0.95);
			TaskSLRecord percentileTask95 = slRecords.get(percentileIndex);
			percentileLatency95s.put(jobType, (percentileTask95.taskLatency)
					/ percentileTask95.taskSize);

			// 99th percentile
			percentileIndex = (int) Math.floor(numOfRecords * 0.99);
			TaskSLRecord percentileTask99 = slRecords.get(percentileIndex);
			percentileLatency99s.put(jobType, (percentileTask99.taskLatency)
					/ percentileTask99.taskSize);
			
			//get absolute percentile 90 th
			if(mExpConfig.dumpAbsPercentile()){
				//create a custom comparator
				Comparator<TaskSLRecord> absComparator = new Comparator<TaskSLRecord>(){

					@Override
					public int compare(TaskSLRecord o1, TaskSLRecord o2) {
						// TODO Auto-generated method stub
						return Double.compare(o1.taskLatency, o2.taskLatency);
					}
					
					
				};
				
				Collections.sort(slRecords, absComparator);
				// 95th percentile
				percentileIndex = (int) Math.floor(numOfRecords * 0.9);
				TaskSLRecord absPercentileTask90 = slRecords.get(percentileIndex);
				absPercentileLatency90s.put(jobType, absPercentileTask90.taskLatency);
				
				percentileIndex = (int) Math.floor(numOfRecords * 0.95);
				TaskSLRecord absPercentileTask95 = slRecords.get(percentileIndex);
				absPercentileLatency95s.put(jobType, absPercentileTask95.taskLatency);
				
			}

		}

		double finishTime = System.currentTimeMillis();
		double sortTime = (finishTime - beginTime) / 1000;
		System.out.printf("it takes " + sortTime
				+ " seconds to sort the latency: " + taskSizeLatencies.length
				+ "\n");

	}
	
	public void updateTaskStats(Task finishedTask) {
		
		/**
		 * skip a number of initial finished jobs to warm up the system
		 */
		if(jobsFinished < warmupJobs)
			return;
		
		int jobType = finishedTask.getJobType();
		double taskSize = finishedTask.getSize();
		double delay = (finishedTask.getFinishTime() - finishedTask
				.getArrivalTime());

		if (averageLatencies.get(jobType) == null) {
			averageLatencies.put(jobType, delay);
		} else {
			double accumuLatency = averageLatencies.get(jobType);
			averageLatencies.put(jobType, accumuLatency + delay);
		}

		if (absAverageServiceTimes.get(jobType) == null) {
			absAverageServiceTimes.put(jobType, taskSize);
		} else {
			double accumuAbsService = absAverageServiceTimes.get(jobType);
			absAverageServiceTimes.put(jobType, accumuAbsService + taskSize);
		}

		// task id is ordered globally among all jobs
		TaskSLRecord slRecord = new TaskSLRecord(taskSize, delay, jobType,
				finishedTask.getTaskId());
		
		//taskSizeLatencies[(int) finishedTask.getTaskId() - 1] = slRecord;
		taskSizeLatencies[jobsFinished - warmupJobs] = slRecord;

	}
	
	@Override
	protected double getAverageServiceTime() {
		// double averageServiceTime = 0.0;
		// for (Job job : jobs) {
		// averageServiceTime += job.getAllTasks().get(0).getSize();
		// }

		// absServiceTime = absServiceTime / numOfJobs;
		// FIXME: needs to be removed
		return absAverageServiceTimes.get(0);
	}
	
	@Override
	public void dumpStatistics() {
	    super.dumpStatistics();
	
	    String fileName = this.fileCreatTime + ".txt";
	    
		/* simulation prints */
		Sim.log(fileName, "Execution time " + this.simulationTime);
		Sim.log(fileName, "Normalized latency " + normalizedLatencies.toString());
		Sim.log(fileName, "Total Energy  " + totalEnergy);

		double execTime = (stopTime - startTime) / 1000.0;
		executionTime = execTime;
		Sim.log(fileName,"\n The experiment took " + execTime + " seconds to run");

	}
	
	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	/*migrated from Experiment class*/
	public double getAvgFinishedJobSize() {
		return averageFinishedJobSize;
	}
	
	public void dumpJobArrDist() {
		// file format: column 1: time, column 2: automatically increments
		if (jobArrivalsStats == null)
			return;
		try {
			FileWriter jobArrDisFile = new FileWriter(new File(
					"job_arrivals_stats"));
			for (float time : jobArrivalsStats) {
				jobArrDisFile.write(Double.toString(time));
				jobArrDisFile.write("\n");
			}

			jobArrDisFile.flush();
			jobArrDisFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpJobCmpDist() {
		// file format: column 1: time, column 2: automatically increments
		if (jobCompletedStats == null)
			return;
		try {
			FileWriter jobCmpDisFile = new FileWriter(new File(
					"job_completed_stats"));
			for (float time : jobCompletedStats) {
				jobCmpDisFile.write(Double.toString(time));
				jobCmpDisFile.write("\n");
			}

			jobCmpDisFile.flush();
			jobCmpDisFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

}
