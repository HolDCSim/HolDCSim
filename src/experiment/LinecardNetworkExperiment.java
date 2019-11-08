package experiment;

import event.*;
import infrastructure.*;
import infrastructure.LineCard.LineCardPowerPolicy;
import infrastructure.Port.PortPowerPolicy;
import job.Job;
import job.Task;
import job.Task.TaskStatus;
import loadpredictor.AbstractLoadPredictor;
import loadpredictor.AllAveragePredictor;
import loadpredictor.InstantPredictor;
import loadpredictor.MixedInstantPredictor;
import loadpredictor.WindowedAveragePredictor;
import debug.Sim;
import workload.AbstractWorkloadGen;
import experiment.ExperimentConfig.ServerType;
import scheduler.LinecardNetworkScheduler;
import scheduler.ShallowDeepScheduler;
import scheduler.SimpleTaskScheduler;
import stochastic.ExponentialRandom;
import utility.ServerPowerMeter;
import workload.TraceWorkloadGen;

import java.util.*;

import beans.TaskSLRecord;
import communication.FlowController;
import communication.TaskDependencyGenerator;
import constants.Constants;


/**
 * @author jingxin
 * base class for network based experiment (task to task communication)
 */
public class LinecardNetworkExperiment extends ERoverExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//private static final int NS_EXP_PARAMS = 4; 
	
	private double totalEnergyNoLPI = 0.0;
	private double totalEnergyNoSleep = 0.0;
	private double totalEnergyNeither = 0.0;
	
	private double serverEnergy = 0.0;
	private double serverUtilization = 0.0;
	
	public int numOfCores;
	int linecardsPerSwitch = this.mExpConfig.getK() / mExpConfig.getPortsPerLineCard();
	
	/**
	 * @author jingxin job arrival workloads
	 * 
	 */
	public enum MultiTaskJobWorkload {
		POISSON, USER_SPECIFY
	}
	
	// queue related statistics
	public Vector<Float> jobArrivalsStats;
	public Vector<Float> jobCompletedStats;
		
	public boolean doCollectJobArrivals;
	public boolean doCollectJobCompletes;
	
	/**
	 * number jobs generated randomly
	 */
	protected int jobsGenerated;
	
	public int jobNumThreshold;	// threshold of scheduling computation (number of jobs)
	
	public double timerThreshold;	// threshold of scheduling computation (time period)
	
	public boolean doNumJobPeriodSchedule;
	public boolean doTimePeriodSchedule;
	
	private Server parentServer;
	private ArrayList<Server> childServer;
	private double timeoutTime;
	
	// job workload generator
	protected AbstractWorkloadGen mWorkloadGen;
		
	public Constants.SleepUnit sleepUnit;	
	
	protected FlowController flowController;
	public LinkedList<Job> jobspending;
	
	// use multiple sleep states or not
	protected boolean useMultipleSS;
	
	private boolean useGlobalQueue;
	
	private Vector<Double> jobSizeLatencies = new Vector<>();
	private double totalJobSizeLatencies = 0;

	public LinecardNetworkExperiment(String theExperimentName, ExponentialRandom aRandom, 
			ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		//System.out.println("in network experiment, statement after super");
		this.jobsGenerated = 0;
		//this.jobsFinished = 0;
		//this.statsFileName = "Network_stats";	//used for writing results into files
		
		this.flowController = new FlowController(this);
		// TODO Auto-generated constructor stub
		this.jobNumThreshold = 3;
		this.timerThreshold = 0.0;
		this.doNumJobPeriodSchedule = false;
		this.doTimePeriodSchedule = false;
		this.timeoutTime = 3.5;
		this.useMultipleSS = expConfig.isUseMultipleSS();
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("b", true, "B");
		argParser.addOption("servqueuethres", true, "server queue threshold");
		argParser.addOption("globalqueue", true, "use global queue");
		argParser.addOption("avgInterArrivalTime", true, "avg time in ms between job arrivals");
		
		if(argParser.parse("b") == null || argParser.parse("servqueuethres") == null || argParser.parse("globalqueue") == null) {
			Sim.fatalError("LinecardNetworkExperiment requires B, server queue threshold, and use global queue params");
		}
		
		System.out.println("Port LPI tao: " + mExpConfig.getLcWaitTime(0));
		Sim.log(this.fileCreatTime+".txt", "Port LPI tao: " + mExpConfig.getLcWaitTime(0) );
		
		
		//batch the packets to B before waking up ports
		int B = Integer.parseInt(argParser.parse("b"));
		Sim.log(this.fileCreatTime+".txt", "Port buffer size: "+B);
		System.out.println("Port buffer size: "+B);

		int serverQueueThreshold = Integer.parseInt(argParser.parse("servqueuethres"));
		
		useGlobalQueue = Integer.parseInt(argParser.parse("globalqueue")) == 1 ? true : false;
		Sim.log(this.fileCreatTime+".txt", "Use global queue: "+useGlobalQueue);
		System.out.println("Use global queue: "+useGlobalQueue);
		
		double avgInterArrivalTime = Double.parseDouble(argParser.parse("avgInterArrivalTime"));
		System.out.println("Avg inte-arrival time: " + avgInterArrivalTime);
		Sim.log(this.fileCreatTime+".txt", "Avg inter-arrival time: " + avgInterArrivalTime);
		
		mExpConfig.setLPIB(B);
		mExpConfig.setServerQueueThreshold(serverQueueThreshold);
		mExpConfig.setInterArrivalTime(avgInterArrivalTime);
	}
	
	public void followPreviousSchedule(Job job, double time) {
		ArrayList<Task> tasks = job.getParentTasksToSchedule();
		if (tasks.size() == 0 || tasks == null)
			System.err.println("No tasks to schedule at the beginning!");
		
		for (Task aTask : tasks) {
			Server assignedServer = aTask.getServer();
			if(assignedServer==null){
				assignedServer = parentServer;
			}
			//System.out.println("assignedserver"+assignedServer);
			/*
			 * could not find an instant server to service the request simply put
			 * it to global queue and mark the arrival time for the task
			 */
			if (assignedServer == null) {
				aTask.markArrival(time);
				//return;
				continue;
			}
			
			System.out.println("Time :" + time + " Job "
					+ job.getJobId() + " Task " + aTask.getTaskId()
					+ " arrvial event in server : " + assignedServer.getNodeId()
					+ " Task size :" + aTask.getSize());
			//instead of adding a TaskArrivalEvent, start processing task immediately
			assignedServer.scheduleTask(time, aTask);
			aTask.markArrival(time);
		}
		this.getNextJob(time);
	}
	
	public void setParentServer(Server server) {
		this.parentServer = server;
	}
	
	public void setChildServer(ArrayList<Server> server) {
		this.childServer = server;
	}
	
	public void setTimerStart(double time) {
		timeoutTime = time + timerThreshold;
	}
	
	@Override
	// creat job arrivals based on possion process
	public void getNextJob(double time) {

		jobsGenerated++;

		if (jobsGenerated > numOfJobs) {
			return;
		}

		// need to generate a new job
		else {
			
//			double nextTime = mExpConfig.getInterArrivalTime();
			double nextTime = mWorkloadGen.getNextInterArrival(time);
			double arriveTime = time;
			//double nextTime = 0.0;
			time = time + nextTime;
//			System.out.println("JobArrivalTime = " + time);

			Job job = new Job(dataCenter);
			this.jobspending.add(job);
			//HeuristicJob job = new HeuristicJob(dataCenter);
			job.setExperiment(this);
			
			int numOfTasks = mExpConfig.getNumOfTasks();

			// generate random numOfTasks tasks with random Task size
			Vector<Task> tasks = new Vector<Task>();
			//System.out.println("Number of tasks: " + numOfTasks + " for job "
			//		+ job.getJobId());
			
			// create all tasks and add them to each job
			for (int i = 0; i < numOfTasks; i++) {
				double taskSize = mWorkloadGen.getNextServiceTime();
				Task task = new Task(taskSize, job);
				tasks.add(task);
				task.setTaskStatus(TaskStatus.CREATED);
			}
			job.setAllTasks(tasks);

			// create task dependency within each job
			if (mExpConfig.isSingleWorkload()) {
				Map<Task, Vector<Task>> dependencyGraph = TaskDependencyGenerator
						.generateTaskDependencyGraph(tasks,
								mExpConfig.getAllWorkload());

				// prints out task dependency graph
//				System.out.println("The following is task dependency graph\n");
//				if (dependencyGraph == null)
//					System.out
//							.println("warining, the dependency graph is empty!\n");
//				else {
//					for (Map.Entry<Task, Vector<Task>> entry : dependencyGraph
//							.entrySet()) {
//						entry.getKey().setParentTask(true);
//						Vector<Task> vTask = entry.getValue();
//						entry.getKey().setChildTask(vTask);
//						String sTask = new String();
//
//						for (Task tmp : vTask) {
//							sTask = sTask + " " + Long.toString(tmp.getTaskId());
//							tmp.setParentTask(false);
//							tmp.setParentTask(entry.getKey());
//						}
//
//						sTask = "[" + sTask + " ]";
//						System.out.println(entry.getKey().getTaskId() + " <--- " + sTask + "\n");
//					}
//				}

				job.setTaskDependency(dependencyGraph);
				//System.out.println("hhhhhhhhhhh"+dependencyGraph);
				//System.out.println("hhhhhhhhhhh"+job);
			}
				
			/*******************************************************
			// no longer needs to record job
			jobs.add(job);
			*******************************************************/

			// add job arrivals to job arrival statistics
			if (doCollectJobArrivals)
				jobArrivalsStats.add((float)time);
			
			if (doNumJobPeriodSchedule) {
				if (job.getJobId() % jobNumThreshold == 1) {
					RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
					this.addEvent(jobArrival);
				} else {
					followPreviousSchedule(job, time);
				}
			} else if (doTimePeriodSchedule) {
				if (arriveTime >= timeoutTime) {
					setTimerStart(arriveTime);
					RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
					this.addEvent(jobArrival);
				} else {
					followPreviousSchedule(job, time);
				}
			} else {
				RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
				this.addEvent(jobArrival);
			}
		}

	}
	
	@Override
	protected void setTaskScheduler() {
		taskScheduler = new SimpleTaskScheduler(this);
		
		LinecardNetworkScheduler lnScheduler = new LinecardNetworkScheduler(this);

		double predictorType = mExpConfig.getPredictorType();

		AbstractLoadPredictor predictor = null;

		
		// if it's mixed workload, use mixedinstant predictor
		if (predictorType == -2) {
			predictor = new MixedInstantPredictor(lnScheduler,
					lnScheduler.getQueueHis(),
					lnScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), this);
		}
		
		else if (predictorType == -1) {
			predictor = new InstantPredictor(lnScheduler,
					lnScheduler.getQueueHis(),
					lnScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else if (predictorType == 0) {
			predictor = new AllAveragePredictor(lnScheduler,
					lnScheduler.getQueueHis(),
					lnScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else {
			predictor = new WindowedAveragePredictor(lnScheduler,
					lnScheduler.getQueueHis(),
					lnScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), predictorType);
		}

		taskScheduler = lnScheduler;
		lnScheduler.setQueuePredictor(predictor);
	}
	
	@Override
	public void initialize() {
		
		AbstractSleepController.sleepStateWaitings[ERoverStates.DEEPEST_SLEEP - 1] = mExpConfig.getSingleTao();
		jobspending = new LinkedList<Job>();
		super.initialize();
		System.out.println("Total number of jobs:" + numOfJobs);
		
		// Create a TaskSLRecord record for each task
		int tasksPerJob = mExpConfig.getNumOfTasks();
		taskSizeLatencies = new TaskSLRecord[(numOfJobs - warmupJobs) * tasksPerJob];
		
		if (useGlobalQueue) {
//			globalQueue = ((MultiTaskScheduler) taskScheduler).getGlobalQueue();
//			dumpActualServerHis = mExpConfig.doCollectActServerHis();
//
//			if (mExpConfig.isAsynLogging()) {
//				// this.actualActiveServerHis = new AsynLoggedVector(Pair<Double,
//				// Integer>)();
//				this.actualActiveServerHis = new AsynLoggedVector<Pair<Float, Integer>>(
//						actualServerHisName);
//				this.registerLoggedVectors((AsynLoggedVector<?>) actualActiveServerHis);
//			} else {
//				this.actualActiveServerHis = new Vector<Pair<Float, Integer>>();
//			}
//
//			// in multi-server experiment, all the servers are always set to idle
//
//			updateServerHis(0.0, 0);
			
		}
		
		configureServerSleepState();
		
		this.setJobWorkloadGen(mExpConfig.getJobWorkloadGen());
		
		//update number of jobs based on traceworkload gen
		if (mWorkloadGen instanceof TraceWorkloadGen) {
			mExpConfig.setNumOfJobs(((TraceWorkloadGen) mWorkloadGen).getTotalNumOfJobs());
			numOfJobs = mExpConfig.getNumOfJobs();
		}
		
		//begin to have multiple multi-task job 
		int collectableJobs = numOfJobs - warmupJobs;
		System.out.println("Number of Warm-up jobs:"+warmupJobs);
		if(collectableJobs <= 0){
			Sim.fatalError("total number of jobs less than warm up jobs!");
		}
		
		this.doCollectJobArrivals = mExpConfig.doCollectJobArrivals();
		this.doCollectJobCompletes = mExpConfig.doCollectJobCompletes();
		if (doCollectJobArrivals) {
			jobArrivalsStats = new Vector<Float>();
		}

		if (doCollectJobCompletes) {
			jobCompletedStats = new Vector<Float>();
		}
				
		if(mExpConfig.getLineCardPowerPolicy() == LineCardPowerPolicy.LINECARD_SLEEP) {
			// Set line card transition events
			Vector<LCSwitch> switches = this.getDataCenter().getLCSwitches();
			for(int i = 0; i < switches.size(); i++){
				LCSwitch sSwitch = (LCSwitch) switches.get(i);
				
				for(int j = 1; j <= sSwitch.getNumOfLineCards(); j++) {
					LineCard linecard = sSwitch.getLinecardById(j);
					LineCardTransitiontoSleepEvent lineCardTransitiontoSleepEvent = new LineCardTransitiontoSleepEvent(0.0, this, sSwitch, j, linecard.getSleepController());
					this.addEvent(lineCardTransitiontoSleepEvent);
					linecard.getSleepController().setTransitionEvent(lineCardTransitiontoSleepEvent);
				}
			}
		}
	}
	
//	@Override
//	public void buildCommunications(job.Task task,
//			Vector<job.Task> dependingTaskList, double time) {
//		// TODO Auto-generated method stub
//		this.flowController.buildCommunications(task, dependingTaskList, time);
//	}
	
	@Override
	public void buildCommunications(Task stask, Vector <Task> dtask, double time) {
		this.flowController.buildCommunications(stask, dtask, time);
	}
	
	public FlowController getFlowController(){
		return this.flowController;
	}

	@Override
	public double dumpIdleDistributions() {
		// TODO not implemented in network experiment
		return 0;
	}

	@Override
	protected void initialServerType() {
		// TODO Auto-generated method stub
		// this.serverType = ServerType.NETWORK_SERVER;
		this.serverType = ServerType.EROVER_SERVER;
	//	this.serverType = ServerType.DELAY_DOZE_SERVER2;
	}
	
	public String printStateofServers() {
		
		String output = new String();
		String acservers=new String();
		String Sleep1server= new String();
		String Sleep2server=new String();
		String Sleep3server=new String();
		String Sleep4server= new String();
		String Sleep5server=new String();
		int Jobs_in_queue=0;
		String Job_currently_running = new String();
		
		Jobs_in_queue=getDataCenter().getJobsInQueue();
		
		for (Server ser: getDataCenter().getServers()) {
			if (ser instanceof UniprocessorServer) {
		
				  switch(((UniprocessorServer) ser).getCurrentSleepState()) {
				  case	0: acservers+= "  " +ser.getNodeId() + "("+ser.getTasksInService()+")";
				  			break;
				  case	1: Sleep1server+="  "+ser.getNodeId();
		  			break;
				  case	2: Sleep2server+="  "+ser.getNodeId();
		  			break;
				  case	3: Sleep3server+="  "+ser.getNodeId();
		  			break;
				  case	4: Sleep4server+="  "+ser.getNodeId();
		  			break;
				  case	5: Sleep5server+="   "+ser.getNodeId();
		  			break;
				  			
				  }
				  
				  
				  
				  output= "Queue Size:"+ Jobs_in_queue+" Active: "+acservers + " 1: " + Sleep1server + " 2:" + Sleep2server + " 3:" + Sleep3server+" 4:"+Sleep4server + " 5:" + Sleep5server;
		
			}
		}
		
		
		
		return output;
		
	}

	// initialize the server sleep state
	public void configureServerSleepState () {
		Vector<Server> servers = dataCenter.getServers();
		
		useMultipleSS = mExpConfig.isUseMultipleSS();
		for (Server aServer: servers) {
			UniprocessorServer sServer = (UniprocessorServer) aServer;
			if (useMultipleSS) {
				sServer.setUseMultipleSleepStates(true);
			} else {
				//sServer.setNextFixedSleepState(mExpConfig.getInitialSleepState());
	 			AbstractSleepController sleepable = ((UniprocessorServer) aServer).getSleepController();

				int nextState = sleepable.generateNextSleepState(mExpConfig.getInitialSleepState());

				// if the core is about to enter s4, update the number of s4
				// servers
			//	sleepable.prepareForSleep(0);

//
//				double nextWaitingTime = sleepable
//						.getNextWaitingTime(mExpConfig.getInitialSleepState());
//				StartSleepEvent nextSleepEvent = new MSStartSleepEvent(this.getCurrentTime(), this, (EnergyServer) aServer,
//						nextState);
//				sleepable.setNextSleepEvent(nextSleepEvent,1);
//				this.addEvent(nextSleepEvent);

//				AbstractSleepController abController = sServer.getSleepController();
//				nextState = abController.generateNextSleepState(1);
//				double nextWaitingTime = abController.getNextWaitingTime(1);
//				StartSleepEvent nextSleepEvent = abController.generateSleepEvent(0.0 + nextWaitingTime, this, nextState);
//				Sim.debug(3, "@@@ time: " + (0.0 + nextWaitingTime)
//						+ "after begining next low powerstate " + nextState);
//				abController.setNextSleepEvent(nextSleepEvent, 1);
//				this.addEvent(nextSleepEvent);
			}
		}

	}

	@Override
	protected double getAverageServiceTime() {
		// TODO Auto-generated method stub
		Sim.fatalError("method not implemented");
		return 0;
	}

	@Override
	public void dumpEnergyPerfStats() {
		
		String fileName = this.fileCreatTime + ".txt";
		
		int numofSwitches = this.dataCenter.getNumOfSwitches();
		Vector<LCSwitch> switches = this.getDataCenter().getLCSwitches();
		int TurnedOffSwitches = 0;

		Sim.log(fileName, "Switches::");
		for(int i=0; i< numofSwitches; i++){
			LCSwitch sSwitch = (LCSwitch) switches.get(i);
			
			double lcEnergy = 0;
			double portEnergy = 0;
			
			double lcNoSleepEnergy = 0;
			double portNoLPIEnergy = 0;
			
			double lcActivePower = 0;
			double lcLPI1Power = 0;
			double lcLPI2Power = 0;
			double lcLPI3Power = 0;
			
			if(sSwitch.getType() == LCSwitch.Type.CORE) {
				lcActivePower = mExpConfig.getCoreLcActivePower();
				lcLPI1Power = mExpConfig.getCoreLcLPI1Power();
				lcLPI2Power = mExpConfig.getCoreLcLPI2Power();
				lcLPI3Power = mExpConfig.getCoreLcLPI3Power();
			}
			else if(sSwitch.getType() == LCSwitch.Type.AGGREGATE) {
				lcActivePower = mExpConfig.getAggregateLcActivePower();
				lcLPI1Power = mExpConfig.getAggregateLcLPI1Power();
				lcLPI2Power = mExpConfig.getAggregateLcLPI2Power();
				lcLPI3Power = mExpConfig.getAggregateLcLPI3Power();
			}
			else if(sSwitch.getType() == LCSwitch.Type.EDGE) {
				lcActivePower = mExpConfig.getEdgeLcActivePower();
				lcLPI1Power = mExpConfig.getEdgeLcLPI1Power();
				lcLPI2Power = mExpConfig.getEdgeLcLPI2Power();
				lcLPI3Power = mExpConfig.getEdgeLcLPI3Power();
			}
			else {
				Sim.fatalError("Unrecognized switch type");
			}
			double sleepLC=0;
			// Iterate through switch's line cards
			Vector<LineCard> lineCards = sSwitch.getLineCards();
			for(int p = 0; p < sSwitch.getNumOfLineCards(); p++){
				SwitchSleepController switchSleepController = lineCards.get(p).getSleepController();

				// Account for time spent in line card's final state
				switchSleepController.updateTimeOfFinalState();
				
				double activeTime = switchSleepController.L0Time;
				double LPI1Time = switchSleepController.L1Time;
				double LPI2Time = switchSleepController.L3Time;
				double LPI3Time = switchSleepController.L5Time;
				double offTime = switchSleepController.L6Time;
				
				System.out.println("Switch " + (i + 1) + " LC " + (p + 1) + ": activeTime = " + activeTime + ", LPI1Time = " + LPI1Time + ", LPI2Time = " + LPI2Time + ", LPI3Time = " + LPI3Time + ", offTime = " + offTime);
				Sim.log(fileName, "\nswitch:" + (i + 1) + "." + (p + 1) + ":" + 
						String.format("%.2f", activeTime) + ":" + 
						String.format("%.2f", LPI1Time) + ":" + 
						String.format("%.2f", LPI2Time) + ":" + 
						String.format("%.2f", LPI3Time) + ":" + 
						String.format("%.2f", offTime));
				lcEnergy += activeTime * lcActivePower + LPI1Time * lcLPI1Power + LPI2Time * lcLPI2Power + LPI3Time * lcLPI3Power;
				lcNoSleepEnergy += this.simulationTime * lcActivePower;
				
				if (activeTime == 0) {
					sleepLC++;
				}
			}
			
			for (int j = 1; j <= mExpConfig.getK(); j++) {
				Port port = sSwitch.getPortById(j);
				if((port.getLPIstartTime()>port.getLPIendTime() || port.getLPIendTime() == 0) && mExpConfig.getPortPowerPolicy() == PortPowerPolicy.Port_LPI){
					if(mExpConfig.getPortPowerPolicy() == PortPowerPolicy.Port_LPI) {
						port.setLPIendTime(this.simulationTime);
						port.updatePortLPITime();	
					}
				}
				
				double LPITime = port.getPortLPITime();
				double PortActTime = this.simulationTime-LPITime;
				portEnergy +=LPITime*(Constants.POWER_LPI_PORT)+PortActTime*Constants.POWER_ACTIVE_PORT;
				
				portNoLPIEnergy += this.simulationTime*Constants.POWER_ACTIVE_PORT;
			}
			
			if (sleepLC == linecardsPerSwitch) {
				TurnedOffSwitches++;
				System.out.println("All the linecards in switch#"+(i+1)+" are in Sleep State the whole time during the simulation.");
				//Sim.log(fileName, "All the linecards in switch#"+(i+1)+" are in Sleep State the whole time during the simulation.");
			}
			
			totalEnergy += lcEnergy+portEnergy+this.simulationTime*Constants.POWER_SWITCH_BASE;
			totalEnergyNoSleep += lcNoSleepEnergy + portEnergy + this.simulationTime*Constants.POWER_SWITCH_BASE;
			totalEnergyNoLPI += lcEnergy + portNoLPIEnergy + this.simulationTime*Constants.POWER_SWITCH_BASE;
			totalEnergyNeither += lcNoSleepEnergy + portNoLPIEnergy + this.simulationTime*Constants.POWER_SWITCH_BASE;
			Sim.debug(1, "Energy consumption of switch#"+(i+1) +": " +(lcEnergy+portEnergy+Constants.POWER_SWITCH_BASE));
			//Sim.log_no_n(fileName, ":" + (lcEnergy+portEnergy+Constants.POWER_SWITCH_BASE)); // 2
			
			//Sim.log(fileName,"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"); // 2
		}
		Sim.log(fileName,"Not operating switches: " + TurnedOffSwitches); // 1
		Sim.debug(1, "Not operating switches: " + TurnedOffSwitches);
		
		// Calculate server energy
		int numOfServers = this.getExpConfig().getServersToSchedule();
		Vector<Server> servers = this.getDataCenter().getServers();

		double totalServiceTime = 0;
		Sim.log(fileName,"Servers::");
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			double aEnergy = sServer.generateEnergyAndPerfStats();
			AbstractSleepController abController = sServer.getSleepController();
			
			//collectedIdleTime 0 means this core is always idle
			if (abController.collectedIdleTime == 0.0)
				abController.collectedIdleTime = this.simulationTime;
			
			totalServiceTime += abController.serviceTime;
			System.out.println("Server " + (i + 1) + " service time: " + abController.serviceTime + " Idle time: " + abController.idleTime + "  "+ abController.C0S0Time + " " + abController.C1S0Count + " " + abController.C3S0Time +" " + abController.C6S0Time + " "+ abController.C6S3Time );
			//Sim.log(fileName,"Server " + (i + 1) + " service time: " + abController.serviceTime + " " + abController.C0S0Time + " " + abController.C1S0Count + " " + abController.C3S0Time +" " + abController.C6S0Time + " "+ abController.C6S3Time);
			

			serverEnergy += aEnergy;
		}
		
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			double aEnergy = sServer.generateEnergyAndPerfStats();
			AbstractSleepController abController = sServer.getSleepController();
			
			if (abController.collectedIdleTime == 0.0)
				abController.collectedIdleTime = this.simulationTime;	
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
		}
		double avgServiceTime = totalServiceTime / numOfServers;
		serverUtilization = avgServiceTime / simulationTime * 100;
	}
	
	@Override
	public void dumpStatistics() {
	    super.dumpStatistics();
		
 
	    
		String fileName = this.fileCreatTime + ".txt";
		/* simulation prints */
//		Sim.log(fileName, 
//				String.format("%s", "lc_network:")
//				String.format(", arg1)
//				
//				
//				
//				);

        Sim.debug(1, "Execution time: " + this.simulationTime);
        Sim.debug(1, "Server Utilization: " + Math.round(serverUtilization) + "%");
        Sim.debug(1, "Network Energy: " + totalEnergy);
        Sim.debug(1, "Server Energy: " + serverEnergy);
        Sim.debug(1, "Total Energy: " + (totalEnergy + serverEnergy));
        Sim.debug(1, "Network percent: " + totalEnergy/(totalEnergy + serverEnergy));
        Sim.debug(1, "Server percent: " + serverEnergy/(totalEnergy + serverEnergy));
        Sim.debug(1, "If servers are never turned off: " + (ServerPowerMeter.calculateActivePower(mExpConfig, mExpConfig.getSpeed()) * this.simulationTime * this.dataCenter.getExpConfig().getServersToSchedule()));
        Sim.debug(1, "Network energy if all the linecards remain active but port LPI exists, energy consumption: " + totalEnergyNoSleep);
        Sim.debug(1, "Network energy if all linecard has sleep state but all the ports remain active, energy consumption: " + totalEnergyNoLPI);
        Sim.debug(1, "Network energy if all the linecards and ports remain active, energy consumption: " + totalEnergyNeither);

		
		Sim.log(fileName, "Execution time: " + this.simulationTime);
		Sim.log(fileName, "Server Utilization: " + Math.round(serverUtilization) + "%");
		Sim.log(fileName, "Network Energy: " + totalEnergy);
		Sim.log(fileName, "Server Energy: " + serverEnergy);
		Sim.log(fileName, "Total Energy: " + (totalEnergy + serverEnergy));
		Sim.log(fileName, "Network percent: " + totalEnergy/(totalEnergy + serverEnergy));
		Sim.log(fileName, "Server percent: " + serverEnergy/(totalEnergy + serverEnergy));
		Sim.log(fileName, "If servers are never turned off: " + (ServerPowerMeter.calculateActivePower(mExpConfig, mExpConfig.getSpeed()) * this.simulationTime * this.dataCenter.getExpConfig().getServersToSchedule()));
		Sim.log(fileName, "Network energy if all the linecards remain active but port LPI exists, energy consumption: " + totalEnergyNoSleep);
		Sim.log(fileName, "Network energy if all linecard has sleep state but all the ports remain active, energy consumption: " + totalEnergyNoLPI);
		Sim.log(fileName, "Network energy if all the linecards and ports remain active, energy consumption: " + totalEnergyNeither);
//		Sim.debug(1, "Network Power: " + totalEnergy/(this.simulationTime*20));
//		Sim.debug(1, "Server Power: " + serverEnergy/(this.simulationTime*16));
//		Sim.debug(1, "Server Power: " + 92*this.simulationTime*this.dataCenter.getExpConfig().getServersToSchedule()/(this.simulationTime*16));
		double execTime = (stopTime - startTime) / 1000.0;
		executionTime = execTime;
		Sim.log(fileName,"\nThe experiment took " + execTime + " seconds to run");
		System.out.println("\n The experiment took " + execTime + " seconds to run");
	
		// TODO figure out what these should really say
		System.out.println("\n The experiment took " + (totalEnergyNeither - totalEnergy)/totalEnergyNeither + " seconds to run");
		System.out.println("\n The experiment took " + (totalEnergyNeither - totalEnergyNoSleep)/totalEnergyNeither + " seconds to run");
		System.out.println("\n The experiment took " + (totalEnergyNeither - totalEnergyNoLPI)/totalEnergyNeither + " seconds to run");
		//System.out.println("\n The experiment took " + (totalEnergyNeither - totalEnergy)/totalEnergyNeither + " seconds to run");

		graphServerResults();
	}

	@Override
	public void collectLatencyStats() {
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

		for (Map.Entry<Integer, Double> oneEntry : absAverageServiceTimes.entrySet()) {
			int jobType = oneEntry.getKey();
			int jobsByType = slRecordByTypes.get(jobType).size();
			oneEntry.setValue(oneEntry.getValue() / jobsByType);
			double latency = normalizedLatencies.get(jobType);
			normalizedLatencies.put(jobType, latency / oneEntry.getValue());
		}

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
			System.out.println("Normalized latency: " + percentileLatency50s.get(jobType));

			// 95th percentile
			percentileIndex = (int) Math.floor(numOfRecords * 0.95);
			TaskSLRecord percentileTask95 = slRecords.get(percentileIndex);
			percentileLatency95s.put(jobType, (percentileTask95.taskLatency)
					/ (percentileTask95.taskSize + (getExpConfig().getAvgFlowSize()/ getExpConfig().getEdgeSwitchBW())));
			System.out.println("95 percentile normalized latency: " + percentileLatency95s.get(jobType));

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
		
		// Defined new method to collect latency stats for multi-job experiments
		collectJobLatencyStats();
		System.out.println();
	}

	@Override
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
		
		taskSizeLatencies[(int) finishedTask.getTaskId() - 1] = slRecord;
	}
	
	public static class IdleRecord {
		public static double INITIAL = -1.0;
		public double startIdleTime = 0.0;
		public double duration = -1.0;
		public int sleepstate = -1;
	}
	
	public void setJobWorkloadGen(AbstractWorkloadGen abGen) {
		this.mWorkloadGen = abGen;
	}
	
	public void collectJobLatencyStats() {
		// It is assumed that all jobs are same type in network experiment
		int numJobs = jobSizeLatencies.size();
	
		String fileName = this.fileCreatTime + ".txt";
		double avgJobSizeLatency = totalJobSizeLatencies / numJobs;
		Sim.log(fileName, "Avg Latency: " + avgJobSizeLatency);
		System.out.println("Avg Latency: " + avgJobSizeLatency);
		
		// Test how long it takes to sort all the jobs
		double beginTime = System.currentTimeMillis();
		Collections.sort(jobSizeLatencies);
		
		// User-defined percentile
		int percentileIndex = (int) Math.floor(numJobs
				* mExpConfig.getLatencyPercent());
		percentileLatencys.put(0, jobSizeLatencies.get(percentileIndex));
		
		// Median percentile
		percentileIndex = (int)Math.floor(numJobs * 0.5);
		percentileLatency50s.put(0, jobSizeLatencies.get(percentileIndex));
		
		// 95th percentile
		percentileIndex = (int)Math.floor(numJobs * 0.95);
		percentileLatency95s.put(0, jobSizeLatencies.get(percentileIndex));
		Sim.log(fileName, "95 percentile latency: " + percentileLatency95s.get(0));
		System.out.println("95 percentile latency: " + percentileLatency95s.get(0));
		
		// 95th percentile
		percentileIndex = (int)Math.floor(numJobs * 0.99);
		percentileLatency99s.put(0, jobSizeLatencies.get(percentileIndex));
		
		double finishTime = System.currentTimeMillis();
		double sortTime = (finishTime - beginTime) / 1000;
		Sim.log(fileName, "It takes " + sortTime + " seconds to sort the latency of " + numJobs + " jobs");
		System.out.println("It takes " + sortTime + " seconds to sort the latency of " + numJobs + " jobs");
	}
	
	@Override
	public void jobFinished(Job job, double time) {
		super.jobFinished(job, time);
		double jobLatency = job.getFinishTime() - job.getStartTime();
		jobSizeLatencies.add(jobLatency);
		totalJobSizeLatencies += jobLatency;
	}
	
	public void topo_animation(){
		super.topo_animation();
		String fileName = "link_anim"+ this.fileCreatTime + ".txt";
		for(Link aLink: this.flowController.cachedLinks.values()) {
			Sim.log(fileName, aLink.hashCode()+":"+aLink.UtilTrace);
		}
		//System.out.println("cachedLinks:" + this.flowController.cachedLinks);
	}
}
