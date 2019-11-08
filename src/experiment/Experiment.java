package experiment;

import infrastructure.DataCenter;
import infrastructure.LCSwitch;
import infrastructure.Server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import communication.FlowController;
import infrastructure.ShallowDeepServer;
import job.Job;
import job.Task;
import queue.BaseQueue;
import scheduler.ITaskScheduler;
import scheduler.SimpleTaskScheduler;
import stat.Statistic;
import stat.StatisticsCollection;
import utility.AsynLoggedVector;
import beans.TaskSLRecord;
import debug.Sim;
import event.AbstractEvent;
import event.AllJobsFinishEvent;
import event.Event;
import event.Event.EVENT_TYPE;
import event.EventQueue;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;
import experiment.ExperimentConfig.SimMode;
import utility.Pair;


public abstract class Experiment implements Serializable, Cloneable {

	/** The Serialization id. */
	private static final long serialVersionUID = 1L;

//	protected static class MaxMinPercentile {
//		protected int numOfRecords;
//		protected double max;
//		protected double min;
//
//		public MaxMinPercentile() {
//			this.numOfRecords = 0;
//			this.max = Double.MAX_VALUE;
//			this.min = Double.MIN_VALUE;
//		}
//
//		public void setNumOfRecords(int n) {
//			this.numOfRecords = n;
//		}
//
//		public int getNumOfRecord() {
//			return numOfRecords;
//		}
//
//		public void setMax(double max) {
//			this.max = max;
//		}
//
//		public double getMax() {
//			return this.max;
//		}
//
//		public void setMin(double min) {
//			this.min = min;
//		}
//
//		public double getMin() {
//			return this.min;
//		}
//	}

	/** The experiment's event queue. */
	private EventQueue eventQueue;

	/** The number of events that have been processed. */
	private long nEventsProccessed;

	/** The current time of the simulation. */
	private double currentTime;

	protected double executionTime;

	protected FileWriter aggLogs;

	// name of file for experiment power and performance result
	protected String statsFileName;

	/** the time to label log file */

	/**
	 * The datacenter for the experiment.
	 */
	protected DataCenter dataCenter;

	// modified by jingxin
	// now the experiment could process multiple jobs
	// fanyao added: only record task size and latency instead of
	// space-consuming vector
	protected Vector<Job> jobs;

	// protected double[] taskSizes;
	// protected double[] taskLatencies;

	

	/**
	 * The input to the experiment.
	 */
	protected ExperimentInput experimentInput;

	/**
	 * The output to the experiment.
	 */
	protected ExperimentOutput experimentOutput;

	/**
	 * The name of the experiment.
	 */
	private String experimentName;

	/**
	 * The limit (in number of events) on how many events can be processed.
	 */
	// private int eventLimit;

	public long totalEvents;

	/**
	 * A flag determining if this experiment should stop once it reaches steady
	 * state. Used to just run the characterization phase of the experiment (for
	 * the master).
	 */
	// private boolean stopAtSteadyState;

	/**
	 * A flag indicating the simulation should stop at the next possible step.
	 */
	protected boolean stop;

	protected long startTime;
	protected long stopTime;

	protected double simulationTime;
	protected String fileCreatTime;
	
	protected double totalEnergy = 0.0;
	protected double averagePower = 0.0;

	// experiment config
	protected ExperimentConfig mExpConfig;

	// task scheuler, needs to be instantiated by subclasses
	ITaskScheduler taskScheduler = null;

	/* num of jobs to simulate
	 * if run in fixed_time mode, this number is not know
	 * until the time has reached
	 * */
	protected int numOfJobs;

	protected int jobsFinished;

	/**
	 * type of server to create in datacenter
	 */
	protected ServerType serverType;
	
	//protected FlowController flowController;

	//protected boolean useRandomWorkload;

	/**
	 * containers for all registered asynchronously logged vectors
	 */
	protected Vector<AsynLoggedVector<?>> allAsynVectors;
	
	protected int warmupJobs = 0;
	
	/**time required for the simulation to run
	 * used in FIXED_TIME mode
	 */
	protected double simTime;
	
	// protected double overallDelay;
	protected boolean useGlobalQueue;

	/**
	 * Constructs a new experiment.
	 * 
	 * @param theExperimentName
	 *            - the name of the experiment
	 * @param theExperimentInput
	 *            - inputs to the experiment
	 * @param thExperimentOutput
	 *            - outputs of the experiment
	 */
	public Experiment(final String theExperimentName,
			final ExperimentInput theExperimentInput,
			final ExperimentOutput thExperimentOutput,
			ExperimentConfig expConfig, ArgumentParser argParser) {

		this.stop = false;
		this.currentTime = 0.0d;
		// this.eventLimit = 0;
		this.experimentName = theExperimentName;
		this.experimentInput = theExperimentInput;
		this.experimentOutput = thExperimentOutput;
		this.mExpConfig = expConfig;
		this.eventQueue = new EventQueue();
		// this.stopAtSteadyState = false;
		this.totalEvents = 0;
		// this.absAverageServiceTime = 0.0;

		this.jobs = new Vector<Job>();
		// this.jobs = null;

		this.statsFileName = "base_stats";
		this.allAsynVectors = new Vector<AsynLoggedVector<?>>();


		DateFormat simple = new SimpleDateFormat("MM_dd_HH_mm");
		String sDate = simple.format(ExperimentOutput.expDate);
		fileCreatTime = sDate;
		// this.fileCreatTime = null;

		initialServerType();
		parseCmdParams(argParser);
	}
	

	/**
	 * Initializes the experiment so it is ready to run. This entails priming
	 * every server with an initial arrival event.
	 */
	public void initialize() {
		// this.dataCenter = this.experimentInput.getDataCenter();
		// Vector<Server> servers = dataCenter.getServers();
		// // Make sure all the arrival processes have begun
		// Iterator<Server> iterator = servers.iterator();
		// while (iterator.hasNext()) {
		// Server server = iterator.next();
		// server.createNewArrival(0.0);
		// }

		// default way to initialize jobs
		// all jobs generated at the same time

		// set task scheduler

		// modified by jingxin//
		// generate a number of jobs, stored in an array
		numOfJobs = mExpConfig.getNumOfJobs();
		System.out.println("Number of jobs: " + numOfJobs);
		warmupJobs = mExpConfig.initialJobsToSkip();
		System.out.println("Number of warm up jobs: " + warmupJobs);
		//int numOfTasks = mExpConfig.getNumOfTasks();

		// after numOfJobs has been set by parseCmdParams()
		// jobArrivalsStats = new double[numOfJobs];
		// jobCompletedStats = new double[numOfJobs];
		if (Sim.RANDOM_SEEDING) {
			Sim.warning("Warning: this experiment uses random seeding");
		} else {	//3rd log, fixed seeding
			Sim.warning("Warning: this experiment uses fixed seeding");
		}
	}


	protected void graphServerResults() {

		//Sim.debug(2, "Graphing Results\n");
		String fileName =  this.fileCreatTime + ".txt";
		String fileName2 = "server_graph" + this.fileCreatTime + ".txt";
		String fileName3 = "server_traces" + this.fileCreatTime + ".txt";
		String fileName4 = "switch_graph" + this.fileCreatTime + ".txt";
		String fileName5 = "switch_traces" + this.fileCreatTime + ".txt";
		String fileName6 = "average_states" + this.fileCreatTime + ".txt";
		Sim.log_no_n("whichFiles.txt", fileName+","+fileName2+","+fileName3+","+fileName4+","+fileName5+","+fileName6);

		Vector<Pair<Double, Integer>> Active = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> C1S1 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> C3S1 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> C6S1 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> C6S3 = new Vector<Pair<Double, Integer>>();


		Vector<Pair<Double, Integer>> Switch_0 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> Switch_1 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> Switch_2 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> Switch_3 = new Vector<Pair<Double, Integer>>();
		Vector<Pair<Double, Integer>> Switch_4 = new Vector<Pair<Double, Integer>>();

		int samples = this.mExpConfig.getSamplingRate();
		double sample_size = this.simulationTime/samples;
		double rate = (double)1/(double)samples;
		System.out.println("samples: "+samples);
		System.out.println("rate:"+rate);
		System.out.println("simtime:"+this.simulationTime);

		Vector<Server> servers = this.getDataCenter().getServers();
		for (int i = 0; i < mExpConfig.getServersToSchedule(); i++) {
			ShallowDeepServer sServer = (ShallowDeepServer) servers.get(i);
			Sim.log(fileName, "SSTrace:Server#" + sServer.getNodeId()+":"+  sServer.getSSTrace());
		}

		Vector<LCSwitch> switches = this.getDataCenter().getLCSwitches();
		for( LCSwitch swtch : switches) {
			Sim.log(fileName,  "SSTrace:Switch#" + swtch.getNodeId() + ":" + swtch.getSSTrace());

		}
		double time = 0;
		for(int i = 0; i<samples; i++) {
			time = time + (sample_size);
			//System.out.println(i + "/" + this.simulationTime*samples);
			Pair<Double, Integer> pair0 = new Pair<Double, Integer>();
			// TODO FIX this shit for the R&D; each vector needs its own unique pair!
			pair0.setFirst(time);
			pair0.setSecond(0);
			Pair<Double, Integer> pair1 = new Pair<Double, Integer>();
			pair1.setFirst(time);
			pair1.setSecond(0);
			Pair<Double, Integer> pair2 = new Pair<Double, Integer>();
			pair2.setFirst(time);
			pair2.setSecond(0);
			Pair<Double, Integer> pair3 = new Pair<Double, Integer>();
			pair3.setFirst(time);
			pair3.setSecond(0);
			Pair<Double, Integer> pair4 = new Pair<Double, Integer>();
			pair4.setFirst(time);
			pair4.setSecond(0);
			Pair<Double, Integer> pair5 = new Pair<Double, Integer>();
			pair5.setFirst(time);
			pair5.setSecond(0);
			Pair<Double, Integer> pair6 = new Pair<Double, Integer>();
			pair6.setFirst(time);
			pair6.setSecond(0);
			Pair<Double, Integer> pair7 = new Pair<Double, Integer>();
			pair7.setFirst(time);
			pair7.setSecond(0);
			Pair<Double, Integer> pair8 = new Pair<Double, Integer>();
			pair8.setFirst(time);
			pair8.setSecond(0);
			Pair<Double, Integer> pair9 = new Pair<Double, Integer>();
			pair9.setFirst(time);
			pair9.setSecond(0);
			Active.add(pair0);
			C1S1.add(pair1);
			C3S1.add(pair2);
			C6S1.add(pair3);
			C6S3.add(pair4);
			Switch_0.add(pair5);
			Switch_1.add(pair6);
			Switch_2.add(pair7);
			Switch_3.add(pair8);
			Switch_4.add(pair9);

			for (int k = 0; k < mExpConfig.getServersToSchedule(); k++) {
				ShallowDeepServer sServer = (ShallowDeepServer) servers.get(k);


				Vector<Pair<Double, Integer>> vect = new Vector<Pair<Double,Integer>>();
				vect = sServer.getSSTrace();


				int j = 1;
				while(j<=vect.size()) {

					if(vect.get(vect.size()-j).getFirst() <= time) {
						break;
					}
					//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
					j = j+1;
				}

				Pair<Double, Integer> graph_pair = new Pair<Double, Integer>();
				graph_pair.setFirst(time);
				graph_pair.setSecond(vect.get(vect.size() - j).getSecond());
				sServer.SSGraph.add(graph_pair);
			}
			for( LCSwitch swtch : switches) {

				Vector<Pair<Double, Integer>> switch_vect = new Vector<Pair<Double,Integer>>();
				switch_vect = swtch.getSSTrace();

				Pair<Double, Integer> switch_pair = new Pair<Double, Integer>();
				switch_pair.setFirst(time);
				switch_pair.setSecond(0);
				swtch.SSGraph.add(switch_pair);

				int j = 1;
				while(j< switch_vect.size()) {

					if(switch_vect.get(switch_vect.size()-j).getFirst() <= time) {
						break;
					}
					//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
					j = j+1;
				}
				swtch.SSGraph.get(i).setSecond(switch_vect.get(switch_vect.size() - j).getSecond());
			}
			for (int k = 0; k < mExpConfig.getServersToSchedule(); k++) {
				ShallowDeepServer sServer = (ShallowDeepServer) servers.get(k);


				Vector<Pair<Double, Integer>> vect = new Vector<Pair<Double,Integer>>();
				vect = sServer.getSSTrace();
				int j = 1;
				while(j<=vect.size()) {

					if(vect.get(vect.size()-j).getFirst() <= time) {
						break;
					}
					//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
					j = j+1;
				}
				System.out.println("Server:"+k+" SS:"+sServer.SSGraph.get(i).getSecond());
				switch(sServer.SSGraph.get(i).getSecond()) {
					case 0:
						Active.get(i).setSecond(Active.get(i).getSecond() + 1);
						break;
					case 1:
						C1S1.get(i).setSecond(C1S1.get(i).getSecond() + 1);
						break;
					case 2:
						C3S1.get(i).setSecond(C3S1.get(i).getSecond() + 1);
						break;
					case 4:
						C6S1.get(i).setSecond(C6S1.get(i).getSecond() + 1);
						break;
					case 5:
						C6S3.get(i).setSecond(C6S3.get(i).getSecond() + 1);
						break;
				}

			}
			for (LCSwitch swtch : switches) {


				Vector<Pair<Double, Integer>> vect = new Vector<Pair<Double,Integer>>();
				vect = swtch.getSSTrace();
				int j = 1;
				while(j<vect.size()) {

					if(vect.get(vect.size()-j).getFirst() <= time) {
						break;
					}
					//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
					j = j+1;
				}
				switch(vect.get(vect.size() - j).getSecond()) {
					case 0:
						Switch_0.get(i).setSecond(Switch_0.get(i).getSecond() + 1);
						break;
					case 1:
						Switch_1.get(i).setSecond(Switch_1.get(i).getSecond() + 1);
						break;
					case 2:
						Switch_2.get(i).setSecond(Switch_2.get(i).getSecond() + 1);
						break;
					case 3:
						Switch_3.get(i).setSecond(Switch_3.get(i).getSecond() + 1);
						break;
					case 4:
						Switch_4.get(i).setSecond(Switch_4.get(i).getSecond() + 1);
						break;
				}

			}
		}

		System.out.print("\nWriting traces\n");
		Sim.log_no_n(fileName, "\n");
		Sim.log_no_n(fileName, "Times: ");
		for(int i = 0;i<Active.size(); i++) {
			System.out.println("Writing " + fileName2);
			Sim.log_no_n(fileName2, Active.get(i).getFirst() + ",");
			Sim.log_no_n(fileName2, Active.get(i).getSecond() + ",");
			Sim.log_no_n(fileName2, C1S1.get(i).getSecond() + ",");
			Sim.log_no_n(fileName2, C3S1.get(i).getSecond() + ",");
			Sim.log_no_n(fileName2, C6S1.get(i).getSecond() + ",");
			Sim.log_no_n(fileName2, ""+C6S3.get(i).getSecond() );
			Sim.log_no_n(fileName2, "\n");

		}
		for(int i = 0;i<Active.size(); i++) {
			System.out.println("Writing " + fileName4);
			Sim.log_no_n(fileName4, Switch_0.get(i).getFirst() + ",");
			Sim.log_no_n(fileName4, Switch_0.get(i).getSecond() + ",");
			Sim.log_no_n(fileName4, Switch_1.get(i).getSecond() + ",");
			Sim.log_no_n(fileName4, Switch_2.get(i).getSecond() + ",");
			Sim.log_no_n(fileName4, Switch_3.get(i).getSecond() + ",");
			Sim.log_no_n(fileName4, ""+Switch_4.get(i).getSecond() );
			Sim.log_no_n(fileName4, "\n");

		}
		for(int i=0; i<Active.size(); i++)
		{
			ShallowDeepServer serv = (ShallowDeepServer) servers.get(0);
			Sim.log_no_n(fileName3, serv.SSGraph.get(i).getFirst() + ",");

			for (int k = 0; k < mExpConfig.getServersToSchedule(); k++) {
				ShallowDeepServer sServer = (ShallowDeepServer) servers.get(k);
				Sim.log_no_n(fileName3, sServer.SSGraph.get(i).getSecond() + ",");
			}
			Sim.log_no_n(fileName3, "\n");
		}
		for(int i=0; i<Active.size(); i++)
		{
			ShallowDeepServer serv = (ShallowDeepServer) servers.get(0);
			Sim.log_no_n(fileName5, serv.SSGraph.get(i).getFirst() + ",");

			for(LCSwitch swtch : switches) {
				Sim.log_no_n(fileName5, swtch.SSGraph.get(i).getSecond() + ",");
			}
			Sim.log_no_n(fileName5, "\n");
		}
		Sim.log(fileName6, "Average Sleep States:");
		for(LCSwitch swtch : switches) {
			Sim.log_no_n(fileName6, swtch.getAverageSleepState(this.simulationTime)+",");

		}
		Sim.log_no_n(fileName6, "\n");
		for (int k = 0; k < mExpConfig.getServersToSchedule(); k++) {
			ShallowDeepServer sServer = (ShallowDeepServer) servers.get(k);
			Sim.log_no_n(fileName6,""+sServer.getAverageSleepState(this.simulationTime)+",");


		}

		Sim.log_no_n(fileName6, "\n");
		topo_animation();
	}

	public void topo_animation() {

		String fileName = "anim_" + this.fileCreatTime + ".txt";

		Sim.log_no_n("whichFiles.txt", "," + fileName);
		int frames = this.mExpConfig.getFrames();
		double simTime = this.simulationTime;
		
		double time_window = simTime/(double)frames;
		System.out.println("SimTime: " + simTime);
		System.out.println("time_window:" + time_window);
		
		double iteration_time;
		
		Vector<LCSwitch> switches = this.getDataCenter().getLCSwitches();
		Vector<Server> servers = this.getDataCenter().getServers();
		
		for (int i=0; i<frames; i++) {
	
			iteration_time = time_window*i;
			Sim.log_no_n(fileName, (iteration_time) + ",");

			for( LCSwitch swtch : switches) {
				Sim.log_no_n(fileName,  swtch.getAverageSleepStateOverTime(iteration_time, iteration_time + time_window)/4 + ",");

			}
			
			for (int k = 0; k < mExpConfig.getServersToSchedule(); k++) {
				ShallowDeepServer sServer = (ShallowDeepServer) servers.get(k);
				Sim.log_no_n(fileName, sServer.getAverageSleepStateOverTime(iteration_time, iteration_time + time_window)/5 + ",");
			}
/*
			for(switches) {
				Sim.log_no_n(fileName, switches.getSleepStateByTime(iteration_time) );
			}
			for(servers) {
				Sim.log_no_n(fileName, server.getSleepStateByTime(iteration_time));
			}
	*/		
			Sim.log_no_n(fileName, "\n");
			//for(Link aLink : mExpConfig.)
		
			
		}
	
		
		
		
	}
	public void flushStatistics() {
		StatisticsCollection statCollection = this.experimentOutput.getStats();
		statCollection.flushStatistics();

	}

	protected void parseCmdParams(ArgumentParser argParser) {
		argParser.addOption("jobspserv", true, "jobs per server");
		argParser.addOption("simtime", true, "sim time");
		
		// get number of jobs to run
		if(argParser.parse("jobspserv") == null || argParser.parse("simtime") == null) {
			Sim.fatalError("Experiment requires jobs per server and sim time params");
		}

		// set debug level here
		Sim.debugLevel = mExpConfig.getDebugLevel();
		//read number of jobs or time to be simulated
		int jobsPerServer = Integer.parseInt(argParser.parse("jobspserv"));
		mExpConfig.setNumOfJobs(jobsPerServer);
		if(jobsPerServer == -1){
			simTime = Double.parseDouble(argParser.parse("simtime"));
			mExpConfig.setSimMode(SimMode.FIXED_TIME);
			mExpConfig.setSimTime(simTime);
		}
		else{
			mExpConfig.setSimMode(SimMode.FIXED_JOBS);
		}
		
	}

	/**
	 * Gets the name of the experiment.
	 * 
	 * @return the name of the experiment
	 */
	public String getName() {
		return this.experimentName;
	}

	/**
	 * Gets the input to the experiment.
	 * 
	 * @return the input to the experiment
	 */
	public ExperimentInput getInput() {
		return this.experimentInput;
	}

	// fanyao added
	public void printFullHistory() {
		this.experimentOutput.printFullHistory();

	}

	public void flushAsynedLogs() {
		for (AsynLoggedVector<?> asynLog : allAsynVectors) {
			asynLog.finalFlush();
		}

		// join all the thread
		Sim.warning("now waiting for all thread to join");
		for (AsynLoggedVector<?> asynLog : allAsynVectors) {
			asynLog.joinThread();
		}
	}

	/**
	 * Gets the output of the experiment.
	 * 
	 * @return the output of the experiment
	 */
	public ExperimentOutput getOutput() {
		return this.experimentOutput;
	}

	/**
	 * Gets the statistics collection for the experiment.
	 * 
	 * @return the statistics collection for the experiment
	 */
	public StatisticsCollection getStats() {
		return this.experimentOutput.getStats();
	}

	/**
	 * Sets a limit on the number of events the experiment will process.
	 * 
	 * @param theEventLimit
	 *            - the limit in event on processed events
	 */
	// public void setEventLimit(final int theEventLimit) {
	// this.eventLimit = theEventLimit;
	// }

	/**
	 * Runs the experiment. The bulk of simulation happens in this.
	 */
	public void run() {

		// task is initialized by the job
		startTime = System.currentTimeMillis();

		this.nEventsProccessed     = 0;
		// Sim.printBanner();
		System.out.println("Starting simulation");
		while (!stop) {
			Event currentEvent = this.eventQueue.nextEvent();

			if (currentEvent == null) {
				System.err
						.println("event queue is empty before all job finished, error may occur, exeuction terminated");
				System.exit(0);
				// break;
			}
			this.currentTime = currentEvent.getTime();
			// fanyao added 10-25-2013
			// update statistics for all objects in data center
			// if (currentEvent.getEventType() == EVENT_TYPE.TASK_EVENT)
			// dataCenter.dumpPerformanceStatistics(currentEvent);
			// else
			if (mExpConfig.doCollectStats()) {
				this.dataCenter.updateStatistics((AbstractEvent) currentEvent);
			}

			// ///////////////////////////////////////////////

			currentEvent.process();
		}
	}

	public void setExperimentFinish(double time) {
		this.simulationTime = time;
		System.out.println("All jobs finished, simulation completed!\n");
		stopTime = System.currentTimeMillis();
		stop();

	}

	// TODO: complete the flush experimentout method
	// private void flushExperimentOut() {
	//
	// }

	/**
	 * Gets the number of events that have been simulated.
	 * 
	 * @return the number of events that have been simulated
	 */
	public long getNEventsSimulated() {
		return nEventsProccessed;
	}

	/**
	 * Adds an event to the experiment's event queue.
	 * 
	 * @param event
	 *            - the event to add
	 */
	public void addEvent(final Event event) {
		this.eventQueue.addEvent(event);
	}

	/**
	 * Cancels an event so that it no longer occurs.
	 * 
	 * @param event
	 *            - the event to cancel
	 */
	public void cancelEvent(final Event event) {
		if(event == null)
			return;
		this.eventQueue.cancelEvent(event);
	}

	/**
	 * Get the current time of the simulation.
	 * 
	 * @return the current time of the simulation
	 */
	public double getCurrentTime() {
		return this.currentTime;
	}

	/**
	 * Runs the experiment to steady state, but no further.
	 */
	public void runToSteadyState() {
		Iterator<Statistic> stats = this.getStats().getAllStats();
		while (stats.hasNext()) {
			Statistic stat = stats.next();
			stat.setJustBins(true);
		}
		// this.stopAtSteadyState = true;
		this.run();
	}

	/**
	 * Stops the simulation.
	 */
	public synchronized void stop() {
		this.stop = true;
	}

	public void jobFinished(Job job, double time) {
		// TODO Auto-generated method stub
		jobsFinished++;
		
		if(numOfJobs < 10000) {
			if(jobsFinished % 100 == 0 && jobsFinished != 0) {
				Sim.debug(2,"current progress "
						  + String.format("%6.4f", (float) jobsFinished / numOfJobs));
			}
			
		}
		else {
			if (jobsFinished % 10000 == 0 && jobsFinished != 0) {
					Sim.debug(2,"current progress "
							  + String.format("%6.4f", (float) jobsFinished / numOfJobs));
			}
		}
		
	

		if (jobsFinished == numOfJobs) {
			AllJobsFinishEvent finishEvent = new AllJobsFinishEvent(time, this);
			this.addEvent(finishEvent);
			
		}

	}

	public ServerType getServerType() {
		return this.serverType;
	}

	public void registerLoggedVectors(AsynLoggedVector<?> loggedVector) {
		this.allAsynVectors.add(loggedVector);
	}

	protected void dumpAggStatistics() {

		try {
			// fileCreatTime = "agg_statistics_" + sDate;
			aggLogs = new FileWriter(
					new File("agg_statistics_" + fileCreatTime), true);
			aggLogs.write("#job/task statistics\n");
			for (Job job : jobs) {
				aggLogs.write(Long.toString(job.getJobId()));
				aggLogs.write("\n");

				for (Task task : job.getAllTasks()) {
					aggLogs.write(Long.toString(task.getTaskId()));
					aggLogs.write("\t");
					aggLogs.write(String.format("%.15f", task.getArrivalTime()));
					aggLogs.write("\t");
					aggLogs.write(String.format("%.15f", task.getStartTime()));
					aggLogs.write("\t");
					aggLogs.write(String.format("%.15f",
							task.getComputationFinishTime()));
					aggLogs.write("\t");
					aggLogs.write(String.format("%.15f", task.getFinishTime()));
					aggLogs.write("\n");
				}
			}

			// public enum EVENT_TYPE{
			// ALL_EVENT
			// SWITCH_EVENT,
			// SERVER_EVENT,
			// JOB_EVENT,
			// TASK_EVENT,
			// PACKET_EVENT,
			// PORT_EVENT,
			// UNKNOWN
			// }
			aggLogs.write("\n");
			aggLogs.write("#number of events executed\n");
			// for (Map.Entry<EVENT_TYPE, Long> entry :
			// dataCenter.getEventsMap()
			// .entrySet()) {
			// switch (entry.getKey()) {
			// case SERVER_EVENT:
			// aggLogs.write("SERVER_EVENT: " + entry.getValue());
			// aggLogs.write("\n");
			// break;
			// case SWITCH_EVENT:
			// aggLogs.write("SWITCH_EVENT: " + entry.getValue());
			// aggLogs.write("\n");
			// break;
			//
			// case JOB_EVENT:
			// aggLogs.write("JOB_EVENT: " + entry.getValue());
			// aggLogs.write("\n");
			// break;
			//
			// case TASK_EVENT:
			// aggLogs.write("TASK_EVENT: " + entry.getValue());
			// aggLogs.write("\n");
			// break;
			//
			// case PACKET_EVENT:
			// aggLogs.write("PACKET_EVENT:" + entry.getValue());
			// aggLogs.write("\n");
			// break;
			//
			// case PORT_EVENT:
			// aggLogs.write("PORT_EVENT: " + entry.getValue());
			// aggLogs.write("\n");
			// break;
			//
			// default:
			// aggLogs.write("UNKNOWN: " + entry.getValue());
			// aggLogs.write("\n");
			// }
			// }

			Map<EVENT_TYPE, Long> eventsMap = dataCenter.getEventsMap();
			SortedSet<EVENT_TYPE> keySet = new TreeSet<EVENT_TYPE>(dataCenter
					.getEventsMap().keySet());
			for (EVENT_TYPE aEventEnum : keySet) {
				aggLogs.write(String.format("%-15s", aEventEnum));
				aggLogs.write(String.format("%-10s", eventsMap.get(aEventEnum)));
				aggLogs.write("\n");
			}

			// print simulation time
			aggLogs.write("\n");
			aggLogs.write("#number of events executed\n");
			aggLogs.write(String.format("%.3f", executionTime));
			aggLogs.flush();
			aggLogs.close();

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	public void dumpStatistics() {
		if (mExpConfig.isAsynLogging()) {
			flushAsynedLogs();
		}
		// TODO Auto-generated method stub
		collectLatencyStats();
		dumpEnergyPerfStats();

	}

	public  ArrayList<Long> remainingJobs = new ArrayList<Long>();

	// creat job arrivals based on possion process
	public void getNextJob(double time) {

	}

	public double getSimulationTime() {
		return simulationTime;
	}

	public void setExperimentConfig(ExperimentConfig expConfig) {
		this.mExpConfig = expConfig;
	}

	public ExperimentConfig getExperimentConfig() {
		return this.mExpConfig;
	}

	public void setDataCenter(DataCenter dc) {
		this.dataCenter = dc;
	}

	public Vector<Job> getJobs() {
		return jobs;
	}

	public Server dispatchTask(Task task, double time, boolean isDependingTask) {
		// TODO Auto-generated method stub
		remainingJobs.add(task.getTaskId());
		return taskScheduler.scheduleTask(task, time, isDependingTask);

	}

	public DataCenter getDataCenter() {
		return dataCenter;
	}

	public ExperimentConfig getExpConfig() {
		return this.mExpConfig;
	}

	public ITaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	public boolean useGlobalQueue() {
		// TODO Auto-generated method stub
		return useGlobalQueue;
	}

	public int getGlobalQueueSize() {
		return 0;
	}

	public BaseQueue getGlobalQueue() {
		return null;
	}
	
//	public void buildCommunications(job.Task task,
//			Vector<job.Task> dependingTaskList, double time) {
//		// TODO Auto-generated method stub
//		
//	}
	
	public void buildCommunications(Task stask,
			Task dTask, double time) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * default task scheduler
	 */
	protected void setTaskScheduler() {
		// TODO Auto-generated method stub
		taskScheduler = new SimpleTaskScheduler(this);

	}

	public int getFinishedJobs() {
		return jobsFinished;
	}
	
	protected abstract void dumpEnergyPerfStats();
	public abstract void collectLatencyStats();
	
	//abstract interfaces to be implemented by subclasses
	public abstract double dumpIdleDistributions();
	protected abstract  double getAverageServiceTime();
	/**
	 * only called internally
	 */
	protected abstract void initialServerType();
	public abstract void updateTaskStats(Task finishedTask);

	
	public void buildCommunications(job.Task task,
			Vector<job.Task> dependingTaskList, double time) {
		// TODO Auto-generated method stub
		
	}


	public abstract String printStateofServers();

}
