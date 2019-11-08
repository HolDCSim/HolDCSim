package experiment;

import infrastructure.LPISwitch;
import infrastructure.Port;

import java.util.Vector;

import job.Job;
import job.Task;
import debug.Sim;
import utility.StringTokens;
import workload.AbstractWorkloadGen;
import event.ExperimentInput;
import event.ExperimentOutput;
import event.RJobArrivalEvent;
import experiment.ExperimentConfig.ServerType;
//import scheduler.CompactTaskScheduler;
import scheduler.SimpleTaskScheduler;
import workload.TraceWorkloadGen;

import java.util.*;

import communication.FlowController;
import communication.TaskDependencyGenerator;
import constants.Constants;


/**
 * @author jingxin
 * base class for network based experiment (task to task communication)
 */
public class NetworkExperiment extends Experiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//private static final int NS_EXP_PARAMS = 4;
	
	public int numOfCores;
	
	/**
	 * @author jingxin job arrival workloads
	 * 
	 */
	public enum JobWorkload {
		POISSON
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
	
	// job workload generator
	protected AbstractWorkloadGen mWorkloadGen;
		
		
	
	protected FlowController flowController;

	public NetworkExperiment(String theExperimentName,
			ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, theExperimentInput, thExperimentOutput,
				expConfig, argParser);
		System.out.println("in network experiment, statement after super");
		this.jobsGenerated = 0;
		//this.jobsFinished = 0;
		//this.statsFileName = "Network_stats";	//used for writing results into files
		
		this.flowController = new FlowController(this);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("rou", true, "rou");
		argParser.addOption("ubar", true, "uBar");
		argParser.addOption("speed", true, "speed");
		argParser.addOption("b", true, "B");
		argParser.addOption("numcores", true, "number of cores");
		
		if(argParser.parse("rou") == null || argParser.parse("ubar") == null || argParser.parse("speed") == null || argParser.parse("b") == null || argParser.parse("numcores") == null) {
			Sim.fatalError("NetworkExperiment requires rou, uBar, speed, B, and number of cores params");
		}
		
		// utilization rate
		double rou = Double.parseDouble(argParser.parse("rou"));
		System.out.println("rou: "+rou);
		// service rate
		double uBar = Double.parseDouble(argParser.parse("uBar"));
		System.out.println("uBar: "+uBar);
		// speed
		double speed = Double.parseDouble(argParser.parse("speed"));
		System.out.println("speed: "+speed);
		//batch the packets to B before waking up ports
		int B = Integer.parseInt(argParser.parse("b"));
		System.out.println("B: "+B);
		numOfCores = Integer.parseInt(argParser.parse("numcores"));
		System.out.println("numofcores: "+numOfCores);
		if(numOfCores <=0 ){
			Sim.fatalError("numOfCores less than 0");
			System.exit(0);
		}
		mExpConfig.setNumOfCores(numOfCores);
		
		mExpConfig.setSpeed(speed);
		mExpConfig.setRou(rou);
		mExpConfig.setuBar(uBar);
		mExpConfig.setLPIB(B);
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
			
			int numOfTasks = mExpConfig.getNumOfTasks();

			// generate random numOfTasks tasks with random Task size
			Vector<Task> tasks = new Vector<Task>();
			System.out.println("number of tasks: " + numOfTasks + " for job "
					+ job.getJobId());
			
			// create all tasks and add them to each job
			for (int i = 0; i < numOfTasks; i++) {
				double taskSize = mExpConfig.getTaskSize();
				Task task = new Task(taskSize, job);
				tasks.add(task);
			}
			job.setAllTasks(tasks);

			// create task dependency within each job
			if (mExpConfig.isSingleWorkload()) {
				Map<Task, Vector<Task>> dependencyGraph = TaskDependencyGenerator
						.generateTaskDependencyGraph(tasks,
								mExpConfig.getAllWorkload());

				// prints out task dependency graph
				System.out.println("The following is task dependency graph\n");
				if (dependencyGraph == null)
					System.out
							.println("warining, the dependency graph is empty!\n");
				else {
					for (Map.Entry<Task, Vector<Task>> entry : dependencyGraph
							.entrySet()) {
						Vector<Task> vTask = entry.getValue();
						String sTask = new String();

						for (Task tmp : vTask) {
							sTask = sTask + " " + Long.toString(tmp.getTaskId());
						}

						sTask = "[" + sTask + " ]";
						System.out.println(entry.getKey().getTaskId() + " <--- " + sTask + "\n");
					}
				}

				job.setTaskDependency(dependencyGraph);
			}
				
			/*******************************************************
			// no longer needs to record job
			jobs.add(job);
			*******************************************************/

			// add job arrivals to job arrival statistics
			if (doCollectJobArrivals)
				jobArrivalsStats.add((float)time);

			RJobArrivalEvent jobArrival = new RJobArrivalEvent(time, this, job);
			this.addEvent(jobArrival);
			
		}

	}
	
	@Override
	protected void setTaskScheduler() {
		taskScheduler = new SimpleTaskScheduler(this);
		//taskScheduler = new CompactTaskScheduler(this);
//		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
//		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler,
//				msScheduler.getQueueHis()));
	}
	
	@Override
	public void initialize() {

		super.initialize();
		System.out.println("Total number of jobs:" + numOfJobs);
		this.setJobWorkloadGen(mExpConfig.getJobWorkloadGen());
		
		//update number of jobs based on traceworkload gen
		if (mWorkloadGen instanceof TraceWorkloadGen) {
			mExpConfig.setNumOfJobs(((TraceWorkloadGen) mWorkloadGen).getTotalNumOfJobs());
			numOfJobs = mExpConfig.getNumOfJobs();
		}
		
		//begin to have multiple multi-task job 
		int collectableJobs = numOfJobs - warmupJobs;
		System.out.println(warmupJobs);
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
		
		//set task scheduler before genDependency
		setTaskScheduler();
				
		this.getNextJob(0.0);
	}
	
	@Override
	public void buildCommunications(job.Task task,
			Vector<job.Task> dependingTaskList, double time) {
		// TODO Auto-generated method stub
		this.flowController.buildCommunications(task, dependingTaskList, time);
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
		this.serverType = ServerType.NETWORK_SERVER;
	}

	@Override
	protected double getAverageServiceTime() {
		// TODO Auto-generated method stub
		Sim.fatalError("method not implemented");
		return 0;
	}

	@Override
	protected void dumpEnergyPerfStats() {
		int numofSwitches = this.dataCenter.getNumOfSwitches();
		Vector<LPISwitch> switches = this.getDataCenter().getSwitches();
		int TurnedOffSwitches = 0;
		System.out.println("\n");
		for(int i=0; i< numofSwitches; i++){
			LPISwitch sSwitch = (LPISwitch) switches.get(i);	
			double aEnergy = 0;
			
			for(int p=1; p<=sSwitch.getnumOfLPIPorts(); p++){
				Port aport = sSwitch.getPortById(p);
				if((aport.getLPIstartTime()>aport.getLPIendTime())||(aport.getLPIendTime()==0)){
					aport.setLPIendTime(this.simulationTime);
					aport.updatePortLPITime();
				}
				
				double LPITime = aport.getPortLPITime();
				double ActTime = this.simulationTime-LPITime;
				if(ActTime==0){
					aEnergy+=0;
					//aEnergy +=LPITime*(Constants.POWER_LPI_PORT)+ActTime*Constants.POWER_ACTIVE_PORT;
				}else{
					aEnergy +=LPITime*(Constants.POWER_LPI_PORT)+ActTime*Constants.POWER_ACTIVE_PORT;
				}
			}
			if(aEnergy!=0){
				totalEnergy += aEnergy+this.simulationTime*Constants.POWER_SWITCH_BASE;
			}else{
				TurnedOffSwitches++;
			}

			Sim.debug(2, "Energy " + aEnergy);
			
			Sim.debug(2, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
		Sim.debug(1, "TurnedOffSwitches  " + TurnedOffSwitches);
		/*// check timing
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
		}*/
		
		//formulateResults();
		
		//serializeResult();
			
				
	}
	
	@Override
	public void dumpStatistics() {
	    super.dumpStatistics();
		
		/* simulation prints */
		Sim.debug(1, "Execution time " + this.simulationTime);
		//Sim.debug(1, "Normalized latency " + normalizedLatencies.toString());
		Sim.debug(1, "Total Energy  " + totalEnergy);

		double execTime = (stopTime - startTime) / 1000.0;
		executionTime = execTime;
		System.out.println("\n The experiment took " + execTime + " seconds to run");

	}

	@Override
	public void collectLatencyStats() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTaskStats(Task finishedTask) {
		// TODO Auto-generated method stub
		
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

	@Override
	public String printStateofServers() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
