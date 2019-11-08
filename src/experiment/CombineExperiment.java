package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.LCSwitch;
import infrastructure.LPISwitch;
import infrastructure.Port;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.util.Map;
import java.util.Vector;

import job.Job;
import job.Task;
import communication.FlowController;
import communication.TaskDependencyGenerator;
import constants.Constants;
import loadpredictor.FakeLoadPredictor;
import debug.Sim;
import scheduler.DualDelayScheduler;
import scheduler.MultiServerScheduler;
import scheduler.ShallowDeepScheduler;
import stochastic.ExponentialRandom;
import workload.MixedWorkloadEZGen;
import event.AllJobsFinishEvent;
import event.ExperimentInput;
import event.ExperimentOutput;
import event.RJobArrivalEvent;

/**
 * Combine experiment
 * @author jingxin
 *
 */

public class CombineExperiment extends DualDelayDozeExperiment {
	
	private static final long serialVersionUID = 1L;
	
	protected FlowController flowController;

	public CombineExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
		this.statsFileName = "COM_stats";
		this.flowController = new FlowController(this);
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("lpitao", true, "LPI tao");
		argParser.addOption("b", true, "B");
		
		if(argParser.parse("lpitao") == null || argParser.parse("b") == null) {
			Sim.fatalError("CombineExperiment requires LPI tao and B params");
		}
		//LPItao
		double LPItao = Double.parseDouble(argParser.parse("lpitao"));
						
		//batch the packets to B before waking up ports
		int B = Integer.parseInt(argParser.parse("b"));
		//mExpConfig.setLPItao(LPItao);
		mExpConfig.setLPIB(B);
	}

	@Override
	public void setTaskScheduler(){
		taskScheduler = new DualDelayScheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
				msScheduler.getQueueHis()));
	}

	@Override
	public void initialize() {
		super.initialize();
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
			double nextTime = mWorkloadGen.getNextInterArrival(time);

			time = time + nextTime;

			Job job = new Job(dataCenter);
			job.setExperiment(this);
			int jobType = mWorkloadGen.getJobType();
			job.setJobType(jobType);
			
			int numOfTasks = mExpConfig.getNumOfTasks();

			// generate random numOfTasks tasks with random Task size
			Vector<Task> tasks = new Vector<Task>();
			
			// create all tasks and add them to each job
			for (int i = 0; i < numOfTasks; i++) {
				double taskSize = mWorkloadGen.getNextServiceTime();
				Task task = new Task(taskSize, job);
				tasks.add(task);
			}
			job.setAllTasks(tasks);

			// create task dependency within each job
			if (mExpConfig.isSingleWorkload()) {
				Map<Task, Vector<Task>> dependencyGraph = TaskDependencyGenerator
						.generateTaskDependencyGraph(tasks,
								mExpConfig.getAllWorkload());

				job.setTaskDependency(dependencyGraph);
			}

			// add job arrivals to job arrival statistics
			if (doCollectJobArrivals)	jobArrivalsStats.add((float)time);

			RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
			this.addEvent(jobArrival);	
		}
	}
	
	@Override
	public void buildCommunications(job.Task task, Vector<job.Task> dependingTaskList, double time) {
		this.flowController.buildCommunications(task, dependingTaskList, time);
	}
	
	public FlowController getFlowController(){
		return this.flowController;
	}
	@Override
	public void dumpEnergyPerfStats() {
		int numOfServers = this.getExpConfig().getServersToSchedule();
		Vector<Server> servers = this.getDataCenter().getServers();
		int numofSwitches = this.dataCenter.getNumOfSwitches();
		Vector<LCSwitch> switches = this.getDataCenter().getLCSwitches();

		System.out.println("\n");
		for (int i = 0; i < numOfServers; i++) {
			UniprocessorServer sServer = (UniprocessorServer) servers.get(i);
			Sim.debug(2, "Server :" + sServer.getNodeId() + " finished "
					+ sServer.getFinishedTasks() + " tasks");
		}

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
			Sim.debug(2, "TempIdle " + String.format("%10.5f", abController.collectedIdleTime));

			Sim.debug(2, "Energy " + aEnergy);
			totalEnergy += aEnergy;
			Sim.debug(2, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		}
		
		System.out.println("\n");
		for(int i=0; i< numofSwitches; i++){
			LCSwitch sSwitch = (LCSwitch) switches.get(i);
			double aEnergy = 0;
			
			for(int p=1; p<=sSwitch.getnumOfLPIPorts(); p++){
				Port aport = sSwitch.getPortById(p);
				if((aport.getLPIstartTime()>aport.getLPIendTime())||(aport.getLPIendTime()==0)){
					aport.setLPIendTime(this.simulationTime);
					aport.updatePortLPITime();
				}
				
				double LPITime = aport.getPortLPITime();
				double ActTime = this.simulationTime-LPITime;
				aEnergy +=LPITime*(Constants.POWER_LPI_PORT)+ActTime*Constants.POWER_ACTIVE_PORT;
			}

			Sim.debug(2, "Energy " + aEnergy);
			totalEnergy += aEnergy+this.simulationTime*Constants.POWER_SWITCH_BASE;
			Sim.debug(2, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
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
			System.out.println("number of google jobs: "
					+ ((MixedWorkloadEZGen) mWorkloadGen).numOfGoogleJobs);
			System.out.println("number of DNS jobs: "
					+ ((MixedWorkloadEZGen) mWorkloadGen).numOfDNSJobs);
		}

		/* dump workload prediction history */
		if (mExpConfig.dumpPrediction()) {
			((ShallowDeepScheduler) taskScheduler).dumpPredictionHis();
		}

		/* dump history of scheduled active servers*/
		if (mExpConfig.dumpSchServerHis()) {
			((ShallowDeepScheduler) taskScheduler).dumpSchServerHis();
		}
		
	}
	public void dumpStatistics() {
		if (mExpConfig.isAsynLogging()) {
			flushAsynedLogs();
		}
		// TODO Auto-generated method stub
		collectLatencyStats();
		dumpEnergyPerfStats();
		
		/* simulation prints */
		Sim.debug(1, "Execution time " + this.simulationTime);
		//Sim.debug(1, "Normalized latency " + normalizedLatencies.toString());
		Sim.debug(1, "Total Energy  " + totalEnergy);

		double execTime = (stopTime - startTime) / 1000.0;
		executionTime = execTime;
		System.out.println("\n The experiment took " + execTime + " seconds to run");

	}
	@Override
	public void jobFinished(Job job, double time) {
		jobsFinished++;
		if (jobsFinished % 10000 == 0 && jobsFinished != 0) {
				Sim.debug(2,"current progress "
						  + String.format("%6.4f", (float) jobsFinished / numOfJobs));
		}
		if (jobsFinished == numOfJobs) {
			AllJobsFinishEvent finishEvent = new AllJobsFinishEvent(time, this);
			this.addEvent(finishEvent);
		}
		// add to job completed statistics
		if (doCollectJobCompletes)
			jobCompletedStats.add((float)time);
		
		double jobMakeSpan= job.getFinishTime() - job.getStartTime();
		
		averageFinishedJobSize = ((averageFinishedJobSize * (jobsFinished - 1)) + jobMakeSpan) / jobsFinished;
		Sim.debug(5, "Average finished job size " + averageFinishedJobSize);
		// Task lastFinishedTask = job.getAllTasks().get(0);
		// double normalizedLatency =
		// lastFinishedTask.getSize()/(lastFinishedTask.getFinishTime() -
		// lastFinishedTask.getArrivalTime());
		// if(normalizedLatency < lLatencyPercentile.max){
		//
		// }

	}

}

