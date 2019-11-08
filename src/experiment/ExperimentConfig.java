package experiment;

import java.io.*;
import java.util.*;

import communication.TaskDependencyGenerator.NetworkLoad;
import constants.Constants;
import constants.Constants.*;
import debug.Sim;

import experiment.LinecardNetworkExperiment.MultiTaskJobWorkload;
import experiment.SingletonJobExperiment.JobWorkload;
import infrastructure.LineCard.LineCardPowerPolicy;
import infrastructure.Port.PortPowerPolicy;
import topology.Topology.NetworkRoutingAlgorithm;
import workload.AbstractWorkloadGen;
import workload.MMPPFluentWorkloadGen;
import workload.MMPPOracleGen;
import workload.MMPPWorkloadGen;
import workload.MixedWorkloadEZGen;
import workload.SSWorkloadGen;
import workload.TraceWorkloadGen;

public class ExperimentConfig {

	public int getSwitchDeepestSleepState() {
		return switchDeepestSleepState;
	}

	public void setSwitchDeepestSleepState(int switchDeepestSleepState) {
		this.switchDeepestSleepState = switchDeepestSleepState;
	}

	public enum SimMode{
		FIXED_JOBS, FIXED_TIME
	}

	public enum Routing {
		SSPR, SAPR, ECMP, NON_OPTIMAL_MULTI_PATH,
	}
	
	public enum TaskSchedulerPolicy{
		FIRST_PICK, RANDOM_AVAIL
	}

	public enum ServerType {
		NETWORK_SERVER, UNIPROCESSOR_SERVER, 
		SHALLOW_DEEP_SERVER, ON_OFF_SERVER, 
		DELAY_OFF_SERVER, ON_SLEEP_SERVER, 
		DELAY_DOZE_SERVER, DUAL_DELAY_SERVER, 
		DELAY_DOZE_SERVER2, //delaydoze server with 2 sleep state 
		EROVER_SERVER, EROVER_MULTICORE, UNKNOWN, DUAL_EROVER_SERVER
	}
	
	public enum ServiceTimeDistType{
		EXP, UNIF
	}

	public static class RandomRange {
		private static boolean debug = true;
		public static long seed;
		private int beginPoint;
		private int endPoint;
		private static Random rd;

		public RandomRange() {

		}

		public void initial(long _seed) {
			seed = _seed;
			if (!debug) {
				rd = new Random(System.currentTimeMillis());
			} else {
				// for debugging purpose, ensure generated sequecen are the same
				// for
				// two different runs
				if (seed != -1) {
					System.out.println("the seed is set to " + seed);
					rd = new Random(seed);
				}
				rd = new Random(123456789);
			}
		}

		public void setBeginPoint(int begin) {
			this.beginPoint = begin;
		}

		public void setEndPoint(int end) {
			this.endPoint = end;
		}

		public RandomRange(int start, int stop) {
			this.beginPoint = start;
			this.endPoint = stop;
		}

		public int getIntConfig() {

			if (beginPoint >= endPoint) {
				System.out.println("begin point larger than end point\n");
				System.exit(0);
			}

			return rd.nextInt(endPoint - beginPoint + 1) + beginPoint;
		}

		public double getDoubleConfig() {
			if (beginPoint >= endPoint) {
				System.out.println("begin point larger than end point\n");
				System.exit(0);
			}
			Random rd = new Random(System.currentTimeMillis());
			return rd.nextInt((endPoint - beginPoint + 1) * 1000) / 1000.0
					+ beginPoint;
		}
	}

	private String configName;

	public String getConfigName() {
		return configName;
	}

	public enum TaskSchedulerType {
		SIMPLE, TEST_TRANS, ONE_SERVER, ENERGY_AWARE
	}

	private TopoName mTopo;
	private HashMap<String, String> topoParams;
	private int k;
	private int numOfSockets;
	private int coresPerSocket;
	/*
	 * Server power values, used to calculate the power usage of servers
	 * in each of the different sleep idle states and during transition 
	 * to active state
	 */
	private double server_active_static_power;
	private double server_active_dynamic_power;
	private double server_c0s0_static_power;
	private double server_c0s0_dynamic_power;
	private double server_c1s0_static_power;
	private double server_c1s0_dynamic_power;
	private double server_c3s0_static_power;
	private double server_c3s0_dynamic_power;
	private double server_c6s0_static_power;
	private double server_c6s0_dynamic_power;
	private double server_c6s3_static_power;
	private double server_c6s3_dynamic_power;

	private double c0s0_wakeup_time;
	private double c1s0_wakeup_time;
	private double c3s0_wakeup_time;
	private double c6s0_wakeup_time;
	private double c6s3_wakeup_time;
	
	private double active_waiting_time;
	private double c0s0_waiting_time;
	private double c1s0_waiting_time;
	private double c3s0_waiting_time;
	private double c6s0_waiting_time;
	private double c6s3_waiting_time;

	private double serverSPower;
	private double serverPPower;
	private String processorType;
//	private double switchSPower;
//	private double lcSPower;
	
	private NetworkRoutingAlgorithm networkRoutingAlgorithm;
	
	// Switch bandwidth settings
	private double coreSwitchBW;
	private double aggregateSwitchBW;
	private double edgeSwitchBW;

	private int switchDeepestSleepState;

	public int getServerDeepestSleepState() {
		return serverDeepestSleepState;
	}

	public void setServerDeepestSleepState(int serverDeepestSleepState) {
		this.serverDeepestSleepState = serverDeepestSleepState;
	}

	private int serverDeepestSleepState;
	
	// Switch power policies
	private LineCardPowerPolicy lineCardPowerPolicy;
	private PortPowerPolicy portPowerPolicy;
	
	// Switch power settings
	private double coreLcActivePower;
	private double coreLcLPI1Power;
	private double coreLcLPI2Power;
	private double coreLcLPI3Power;
	
	private double aggregateLcActivePower;
	private double aggregateLcLPI1Power;
	private double aggregateLcLPI2Power;
	private double aggregateLcLPI3Power;
	
	private double edgeLcActivePower;
	private double edgeLcLPI1Power;
	private double edgeLcLPI2Power;
	private double edgeLcLPI3Power;
	
	// Line card power settings
	private double lcActiveToLPI1Time;
	private double lcLPI1ToLPI2Time;
	private double lcLPI2ToLPI3Time;
	private double lcLPI3ToOffTime;
	
	private double ServerActiveTo1Time;
	private double ServerActiveTo4Time;
	private double ServerActiveTo5Time;
	
	private double switchTransDelay;
	private int portsPerLineCard;
	private Vector<Long> portRates;
	private Vector<Double> portPower;
	private int portBuffSize;
	private int numOfJobs; // fixed, non-zero number, -1, randomly generated
							// during runtime
	private RandomRange jobNumRange;
	private JobArrival jobArrival;
	private int numOfTasks; //
	private double taskSize;
	private RandomRange taskSizeRange;
	private RandomRange taskNumRange;
	private RandomRange packetNumRange;
	private Vector<NetworkLoad> networkLoads;
	private boolean isWorkloadSingle;
	private NetworkLoad allWorkload;
	private int packetNum;

	private Routing routing;
	private long seed;
	private boolean verbose;

	private PortRate portRate;
	private TaskSchedulerType taskSchedulerType;

	private int sleepState;
	private double speed;
	private double rou;
	private double uBar;
	private double u;
	private double lambda;
	private int LPIB; // number of tasks in queue before port wakes up

	private boolean isSeedFixed;
	private long ssSeed;

	// the Qos p
	private double pb;
	private double enforcePeriod;

	// use multiple states ?
	private boolean useMultipleSS;

	// waiting time for processor to enter sleep state
	private double[] taos;
	
	//set only one tao for transitioning from shallow state to deep state
	private double singleTao;
	private JobWorkload jobWorkload;
	private MultiTaskJobWorkload multiTaskJobWorkload;

	// flag to indicate whether to print full history
	private boolean printFullHis;

	// flag to indiciate whether to print aggregation statistics
	private boolean printAggStats;

	// multiple-servers experiment parameters
	/**
	 * number of servers to schedule jobs
	 */
	private int serversToSch = 0;
	private int shallowState = 1;
	private int deepState = 5;
	// private int workloadThreshold;

	private double meanDelayTime;
	
	private double meanLowDelayTime;

	private int debugLevel;

	private boolean preloadPaths;

	private boolean collectStats;

	// logging mode, asynchronous logging or not
	private boolean asynLogging;

	/**
	 * -1 : instantPredictor 0: AllTimeAverage positive: windowed predictor with
	 * the number as the window length
	 */
	private double predictorType;

	private boolean doTimingCheck;

	private boolean dumpPrediction;

	/**
	 * the required percentage of latency percentile
	 */
	private double latencyPercent;

	private boolean dumpSchServerHis;
	private boolean dumpActServerHis;
	private boolean dumpQueueHis;
	private boolean dumpAllServerDetails;
	private boolean dumpIdleDistribution;
	private boolean circularIdleStats;

	private boolean dumpJobArrivals;
	private boolean dumpJobCompletes;

	private boolean dumpStateDurations;
	
	private boolean dumpEnergyDistributions;

	// mixture for mixed workload
	private double mixture;

	// workload burstiness
	private double aRatio = 0.0;
	private double cycle = 0.0;
	private double tRatio = 0.0;
	private double s4Provision = 0.0;
	
	//default number of cores per socket is 1
	private int numOfCores = 1;
	private SleepUnit sleepUnit;
	private int lowDelayServerNum;
	
	
	private boolean absPercentile90th;
	
	private TaskSchedulerPolicy schePolicy;
	
	private ServiceTimeDistType serviceDist;
	
	private int skipInitialJobs = 0;
	
	private AbstractWorkloadGen abWorkloadGen = null;
	
	private String traceFileName;
	
	private double simTime;
	
	private SimMode simMode;
	
	private int pendingThreshold;
	private int serverQueueThreshold;
	
	private Experiment mExperiment;
	private double meanDelayTime2;
	
	/**packet based routing or flow based routing*/
	private boolean packetRouting = false;
	private double flowSize;
	
	// Average inter-arrival time. Used to assist in setting different server utilizations experiments w/ for multi-task jobs and task communication
	private double interArrivalTime;
	
	private int jobQoS;
	
	private int samplingRate;
	private int frame_size;
	private int link_animation;

	public ExperimentConfig(String configFile) {

		// set all defaults
		mTopo = TopoName.FAT_TREE;
		topoParams = new HashMap<String, String>();
		numOfSockets = 1;
		coresPerSocket = 4;
		serverSPower = 50.0;
		serverPPower = 95.0;
		processorType = "generic";
		
		server_active_static_power = 130;
		server_active_dynamic_power = 120;
		server_c0s0_static_power = 60.5;
		server_c0s0_dynamic_power = 75.0;
		server_c1s0_static_power = 60.5;
		server_c1s0_dynamic_power = 47.0;
		server_c3s0_static_power = 60.5;
		server_c3s0_dynamic_power = 22.0;
		server_c6s0_static_power = 60.5;
		server_c6s0_dynamic_power = 15.0;
		server_c6s3_static_power = 13.1;
		server_c6s3_dynamic_power = 15.0;
	

		c0s0_wakeup_time = 0.0;
		c1s0_wakeup_time = 1e-5;
		c3s0_wakeup_time = 1e-4;
		c6s0_wakeup_time = 1e-3;
		c6s3_wakeup_time = 1.0;
		
		active_waiting_time = 0.0;
		c0s0_waiting_time = 0.0;
		c1s0_waiting_time = 0.0;
		c3s0_waiting_time = 0.0;
		c6s0_waiting_time = 0.5;
		c6s3_waiting_time = 100;
		
		samplingRate = 100;
		frame_size = 100;
		link_animation = 1;
		
//		switchSPower = 50.0;
//		lcSPower = 20.0;
		switchTransDelay = 0;
		portRates = new Vector<Long>();
		portPower = new Vector<Double>();
		portBuffSize = 100;
		numOfJobs = -1; //
		jobNumRange = new RandomRange();
		jobArrival = JobArrival.fixed;
		numOfTasks = -1; //
		taskSize = -1;
		isWorkloadSingle = true;
		taskSizeRange = new RandomRange();
		taskNumRange = new RandomRange();
		packetNumRange = new RandomRange();
		networkLoads = new Vector<NetworkLoad>();
		allWorkload = NetworkLoad.UNKNOWN;
		routing = Routing.SSPR; // default routing is single shortest path, Dijkstra
								// routing
        speed = 1.0;
		packetNum = 1;
		seed = -1;
		verbose = true;
		//portRate = PortRate.SDR1;
		portRate = PortRate.Base1000;
		taskSchedulerType = null;

		taos = null;
		printFullHis = false;

		meanDelayTime = 0.0;
		meanLowDelayTime = 0.0;
		debugLevel = 1;
		preloadPaths = true;
		collectStats = false;

		doTimingCheck = false;
		dumpPrediction = false;

		dumpJobArrivals = false;
		dumpJobCompletes = false;

		dumpStateDurations = false;
		
		dumpEnergyDistributions = false;
		
		lowDelayServerNum = 0;
		
		absPercentile90th = false;
		
		//by default, the distribution is exponential
		serviceDist = ServiceTimeDistType.EXP;
		
		simTime = 0.0;
		simMode = SimMode.FIXED_JOBS;
		
		pendingThreshold = 0;
		
		mExperiment = null;
		
		singleTao = 0.0;
		
		serverQueueThreshold = 0;

		try {
			this.configName = configFile;
			BufferedReader bw = new BufferedReader(new FileReader(configName));
			String line = null;
			while ((line = bw.readLine()) != null) {
				if (line.startsWith("#") || line.startsWith("%"))
					continue;

				// remove inline comments
				int position = line.indexOf("//");
				if (position != -1) {
					line = line.substring(0, position);
				}

				String[] splits = line.split("\\s+");
				if (splits.length >= 2) {
					if (splits[0].equals("topo_type"))
						mTopo = this.getTopoType(splits[1]);

					else if (splits[0].equals("topo_params")) {
						for (int j = 1; j < splits.length; j++) {
							String[] params = splits[j].split("=");
							if (params.length != 2) {
								System.err.println("param format is incorrect");

							}
							topoParams.put(params[0], params[1]);
							k = Integer.parseInt(params[1]);
						}

					}

					else if (splits[0].equals("num_of_sockets")) {
						numOfSockets = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("cores_per_socket")) {
						coresPerSocket = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("server_spower")) {
						serverSPower = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("server_ppower")) {
						serverPPower = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("processor")) {
						processorType = splits[1];
					}

//					else if (splits[0].equals("switch_spower")) {
//						switchSPower = Double.parseDouble(splits[1]);
//					}
//
//					else if (splits[0].equals("lc_spower")) {
//						lcSPower = Double.parseDouble(splits[1]);
//					}
					
					else if (splits[0].equals("Server_active_to_1_Time")) {
						ServerActiveTo1Time = Double.parseDouble(splits[1]);
					}
					else if (splits[0].equals("Server_1_to_4_Time")) {
						ServerActiveTo4Time = Double.parseDouble(splits[1]);
					}
					else if (splits[0].equals("Server_4_to_5_Time")) {
						ServerActiveTo5Time = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("network_routing_algorithm")) {
						if(splits[1].equals("elastic_tree")) {
							networkRoutingAlgorithm = NetworkRoutingAlgorithm.ELASTIC_TREE;
						}
						else if(splits[1].equals("djikstra")) {
							networkRoutingAlgorithm = NetworkRoutingAlgorithm.DJIKSTRA;
						} else  if(splits[1].equals("popcorns")) {
							networkRoutingAlgorithm = networkRoutingAlgorithm.POPCORNS;
						} else  if(splits[1].equals("wasp")) {
							networkRoutingAlgorithm = networkRoutingAlgorithm.WASP;
						} else {
							Sim.fatalError("Unregconized network routing algorithm");
						}
					}
					
					else if(splits[0].equals("core_switch_bw")) {
						coreSwitchBW = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("aggregate_switch_bw")) {
						aggregateSwitchBW = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s0_dynamic_power")) {
						server_c6s0_dynamic_power = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s3_static_power")) {
						server_c6s3_static_power = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s3_dynamic_power")) {
						server_c6s3_dynamic_power = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c0s0_wakeup_time")) {
						c0s0_wakeup_time = Double.parseDouble(splits[1]);
					}
					
					else if (splits[0].equals("c1s0_wakeup_time")) {
						c1s0_wakeup_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c3s0_wakeup_time")) {
						c3s0_wakeup_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s0_wakeup_time")) {
						c6s0_wakeup_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s3_wakeup_time")) {
						c6s3_wakeup_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("active_waiting_time")) {
						active_waiting_time = Double.parseDouble(splits[1]);
					}
					
					else if (splits[0].equals("c0s0_waiting_time")) {
						c0s0_waiting_time = Double.parseDouble(splits[1]);
					}
					
					else if (splits[0].equals("c1s0_waiting_time")) {
						c1s0_waiting_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c3s0_waiting_time")) {
						c3s0_waiting_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s0_waiting_time")) {
						c6s0_waiting_time = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("c6s3_waiting_time")) {
						c6s3_waiting_time = Double.parseDouble(splits[1]);
					}

//					else if (splits[0].equals("switch_spower")) {
//						switchSPower = Double.parseDouble(splits[1]);
//					}
					
					else if(splits[0].equals("edge_switch_bw")) {
						edgeSwitchBW = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("line_card_power_policy")) {
						if(splits[1].equals("no_management")) {
							lineCardPowerPolicy = LineCardPowerPolicy.NO_MANAGEMENT;
						}
						else if(splits[1].equals("linecard_sleep")) {
							lineCardPowerPolicy = LineCardPowerPolicy.LINECARD_SLEEP;
						}
						else {
							Sim.fatalError("\"" + splits[1] + "\" is an unrecognized line card power policy");
						}
					}
					
					else if(splits[0].equals("port_power_policy")) {
						if(splits[1].equals("no_management")) {
							portPowerPolicy = PortPowerPolicy.NO_MANAGEMENT;
						}
						else if(splits[1].equals("port_lpi")) {
							portPowerPolicy = PortPowerPolicy.Port_LPI;
						}
						else {
							Sim.fatalError("\"" + splits[1] + "\" is an unrecognized port power policy");
						}
					}
					
					else if(splits[0].equals("core_lc_active")) {
						coreLcActivePower = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("core_lc_lpi1")) {
						coreLcLPI1Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("core_lc_lpi2")) {
						coreLcLPI2Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("core_lc_lpi3")) {
						coreLcLPI3Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("aggregate_lc_active")) {
						aggregateLcActivePower = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("aggregate_lc_lpi1")) {
						aggregateLcLPI1Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("aggregate_lc_lpi2")) {
						aggregateLcLPI2Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("aggregate_lc_lpi3")) {
						aggregateLcLPI3Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("edge_lc_active")) {
						edgeLcActivePower = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("edge_lc_lpi1")) {
						edgeLcLPI1Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("edge_lc_lpi2")) {
						edgeLcLPI2Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("edge_lc_lpi3")) {
						edgeLcLPI3Power = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("lc_active_to_lpi1_time")) {
						lcActiveToLPI1Time = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("lc_lpi1_to_lpi2_time")) {
						lcLPI1ToLPI2Time = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("lc_lpi2_to_lpi3_time")) {
						lcLPI2ToLPI3Time = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].equals("lc_lpi3_to_off_time")) {
						lcLPI3ToOffTime = Double.parseDouble(splits[1]);
					}
					
					else if (splits[0].equals("ports_per_line_card")) {
						portsPerLineCard = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("num_of_jobs")) {
						if (splits[1].equals("fixed")) {
							if (splits.length == 3) {
								numOfJobs = Integer.parseInt(splits[2]);
								if (numOfJobs <= 0) {
									System.out
											.println("num_of_jobs negative, set to 1");
									numOfJobs = 1;
								}
							} else {
								System.err
										.println("num_of_jobs parameters incorrect");

							}
						}

						else if (splits[1].equals("random")) {
							if (splits.length == 4) {
								int start = Integer.parseInt(splits[2]);
								int stop = Integer.parseInt(splits[3]);
								jobNumRange.setBeginPoint(start);
								jobNumRange.setEndPoint(stop);
								numOfJobs = 0;

							} else {
								System.err
										.println("num_of_jobs parameters incorrect");

							}

						}

					}
					
					else if(splits[0].equals("job_QoS")) {
						jobQoS = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("num_of_tasks")) {
						if (splits[1].equals("fixed")) {
							if (splits.length == 3) {
								numOfTasks = Integer.parseInt(splits[2]);
								if (numOfTasks <= 0) {
									System.out
											.println("num_of_tasks negative, set to 50");
									// default number of tasks: 50
									numOfTasks = 50;
								}
							} else {
								System.err
										.println("num_of_tasks parameters incorrect");

							}
						}

						else if (splits[1].equals("random")) {
							if (splits.length == 4) {
								int start = Integer.parseInt(splits[2]);
								int stop = Integer.parseInt(splits[3]);
								taskNumRange.initial(100000);
								taskNumRange.setBeginPoint(start);
								taskNumRange.setEndPoint(stop);
								numOfTasks = 0;

							} else {
								System.err
										.println("num_of_taks parameters incorrect");

							}

						}

					}

					else if (splits[0].equals("packet_num")) {
						if (splits[1].equals("fixed")) {
							if (splits.length == 3) {
								packetNum = Integer.parseInt(splits[2]);
								if (packetNum <= 0) {
									System.out
											.println("packet_num negative, set to 1");
									// default number of tasks: 50
									packetNum = 1;
								}
							} else {
								System.err
										.println("packet_num parameters incorrect");

							}
						}

						else if (splits[1].equals("random")) {
							if (splits.length == 4) {
								int start = Integer.parseInt(splits[2]);
								int stop = Integer.parseInt(splits[3]);
								packetNumRange.initial(12345);
								packetNumRange.setBeginPoint(start);
								packetNumRange.setEndPoint(stop);
								packetNum = 0;

							} else {
								System.err
										.println("packet_num parameters incorrect");

							}

						}

					}
		
					else if (splits[0].equals("task_size")) {
						if (splits[1].equals("fixed")) {
							if (splits.length == 3) {
								taskSize = Integer.parseInt(splits[2])*0.001;
								if (taskSize <= 0) {
									System.out
											.println("num_of_tasks negative, set to 10");
									// default number of tasks: 50
									taskSize = 10;
								}
							} else {
								System.err
										.println("task_size parameters incorrect");

							}
						}

						else if (splits[1].equals("random")) {
							if (splits.length == 4) {
								int start = Integer.parseInt(splits[2]);
								int stop = Integer.parseInt(splits[3]);
								taskSizeRange.initial(23456);
								taskSizeRange.setBeginPoint(start);
								taskSizeRange.setEndPoint(stop);
								taskSize = 0;

							} else {
								System.err
										.println("task_size parameters incorrect");

							}

						}

					}

					else if (splits[0].equals("all_jobs")) {
						isWorkloadSingle = true;
						allWorkload = getSingleWorkLoad(splits[1]);
					}

					else if (splits[0].equals("routing")) {
						for (Routing aRoute : Routing.values()) {
							if (aRoute.name().toLowerCase().equals(splits[1])) {
								routing = aRoute;
								break;
							}
						}
					}

					else if (splits[0].equals("port_buffer_size")) {
						portBuffSize = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("verbose")) {
						verbose = Integer.parseInt(splits[1]) == 1 ? true
								: false;
					}

					else if (splits[0].equals("seed")) {
						seed = Long.parseLong(splits[1]);

					}

					else if (splits[0].equals("link_rate")) {
						String slinkRate = splits[1];
						for (PortRate pr : PortRate.values()) {
							if (pr.toString().equals(slinkRate)) {
								portRate = pr;
								break;
							}
						}
					}

					else if (splits[0].equals("task_scheduler")) {
						taskSchedulerType = parseScheduler(splits[1]);
					}

					else if (splits[0].equals("ss_seed")) {
						if (splits[1].equals("random")) {
							isSeedFixed = false;
						}

						else if (splits[1].equals("fixed")) {
							if (splits.length != 3) {
								System.err
										.println("sleepscale seed parameters incorrect, set to random seeding");
								isSeedFixed = false;
							}

							else {
								isSeedFixed = true;
								ssSeed = Long.parseLong(splits[2]);
							}
						}

						else {
							System.err
									.println("sleepscale seeding mode incorrect, set to random seeding");

						}
					}

					else if (splits[0].equals("pb")) {
						this.pb = Double.parseDouble(splits[1]);
					}

					else if (splits[0].equals("multi_states")) {
						this.useMultipleSS = Integer.parseInt(splits[1]) == 1 ? true
								: false;
					}

					else if (splits[0].equals("job_workload")) {
						if (splits[1].equals("poisson")) {
							jobWorkload = JobWorkload.POISSON;
							multiTaskJobWorkload = MultiTaskJobWorkload.POISSON;
						}

						else if (splits[1].equals("mmpp")) {
							jobWorkload = JobWorkload.MMPP;
						}

						else if (splits[1].equals("mixed")) {
							jobWorkload = JobWorkload.MIXED;
						}
						else if(splits[1].equals("mmpp_fluent")){
							jobWorkload =  JobWorkload.MMPPFLU;
						}
						
						else if(splits[1].equals("mmpp_oracle")){
							jobWorkload =  JobWorkload.MMPPORAC;
						}
						
						else if(splits[1].equals("trace")){
							jobWorkload = JobWorkload.TRACE;
						}
						
						else if(splits[1].equals("user_specify")) {
							multiTaskJobWorkload = MultiTaskJobWorkload.USER_SPECIFY;
						}

						else {
							System.err.println("unknown job workload type " + splits[1]);
							System.exit(0);
						}

					}

					else if (splits[0].equals("print_full_his")) {
						printFullHis = Integer.parseInt(splits[1]) == 1 ? true
								: false;
					}

					else if (splits[0].equals("agg_stats")) {
						printAggStats = Integer.parseInt(splits[1]) == 1 ? true
								: false;
					}

					else if (splits[0].equals("servers_sch")) {
						serversToSch = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("shallow_state")) {
						shallowState = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("server_deep_state")) {
						serverDeepestSleepState = Integer.parseInt(splits[1]);
					}
					else if (splits[0].equals("switch_deep_state")) {
						switchDeepestSleepState = Integer.parseInt(splits[1]);
					}
					else if (splits[0].equals("debug_level")) {
						debugLevel = Integer.parseInt(splits[1]);
					}

					else if (splits[0].equals("path_preload")) {
						preloadPaths = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					}

					else if (splits[0].equals("collect_stats")) {
						collectStats = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("timing_check")) {
						doTimingCheck = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("dump_prediction")) {
						dumpPrediction = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("latency_percent")) {
						latencyPercent = Double.parseDouble(splits[1]);
					} else if (splits[0].equals("dump_sch_serverhis")) {
						dumpSchServerHis = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("dump_act_serverhis")) {
						dumpActServerHis = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("dump_queue_his")) {
						dumpQueueHis = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("allserver_stats")) {
						dumpAllServerDetails = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("idle_distribution")) {
						dumpIdleDistribution = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("asyn_log")) {
						asynLogging = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("circular_idlestats")) {
						circularIdleStats = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("job_arrivals")) {
						dumpJobArrivals = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("job_completes")) {
						dumpJobCompletes = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("state_durations")) {
						dumpStateDurations = Integer.parseInt(splits[1]) == 0 ? false
								: true;
					} else if (splits[0].equals("mixture")) {
						mixture = Double.parseDouble(splits[1]);
					} else if (splits[0].equals("aratio")) {
						aRatio = Double.parseDouble(splits[1]);
					} else if (splits[0].equals("tratio")) {
						tRatio = Double.parseDouble(splits[1]);
					} else if (splits[0].equals("cycle")) {
						cycle = Double.parseDouble(splits[1]);
					} else if(splits[0].equals("s4_provision")){
						s4Provision = Double.parseDouble(splits[1]);
					}
					else if(splits[0].equals("sche_policy")){
						if(splits[1].equals("first_pick")){
							schePolicy = TaskSchedulerPolicy.FIRST_PICK;
						}
						else if(splits[1].equals("random_avail")){
							schePolicy = TaskSchedulerPolicy.RANDOM_AVAIL;
						}
						else{
							Sim.fatalError("fatal error: unknown scheduler policy " + splits[1]);
						}
							
					}
					else if(splits[0].equals("abs_percentile_90")){
						absPercentile90th = Integer.parseInt(splits[1]) == 1 ? true: false;
					}
					else if(splits[0].equals("service_dis")){
						if(splits[1].equals("expo"))
							serviceDist = ServiceTimeDistType.EXP;
						else if(splits[1].equals("unif"))
							serviceDist = ServiceTimeDistType.UNIF;
						else
							Sim.fatalError("fatal error: unknown service time distribution type : " + splits[1]);
					}
					else if(splits[0].equals("skip_initial_jobs")){
						skipInitialJobs = Integer.parseInt(splits[1]);
					}
					
					else if(splits[0].equals("energy_distributions")){
						dumpEnergyDistributions = Integer.parseInt(splits[1]) == 0 ? false: true;
					}
					else if(splits[0].equals("trace_filename")){
						traceFileName = splits[1];
					}
					else if(splits[0].equals("sim_mode")){
						if(splits[1].equals("fixed_time"))
							simMode = SimMode.FIXED_TIME;
						else if(splits[1].equals("fixed_jobs"))
							simMode = SimMode.FIXED_JOBS;
						else
							Sim.fatalError("unknown sim mode");
					}
					
					else if(splits[0].equals("sim_time")){
						simTime = Double.parseDouble(splits[1]);
					}
					else if(splits[0].equals("routing_level")){
						if(splits[1].equals("packet")){
							packetRouting = true;
						}
						else if(splits[1].equals("flow")){
							packetRouting = false;
						}
						else{
							Sim.fatalError("unknown routing mode: " + splits[1]);
						}
					}
					else if(splits[0].equals("flow_size")){
						flowSize = Double.parseDouble(splits[1]);
					}
					
					else if(splits[0].contentEquals("sampling_rate")) {
						samplingRate = Integer.parseInt(splits[1]) ;
					}
					else if(splits[0].contentEquals("frame_size")) {
						frame_size = Integer.parseInt(splits[1]);
					}
					else if(splits[0].contentEquals("link_animation")) {
						link_animation = Integer.parseInt(splits[1]);
					}
				}

			}

			bw.close();

		} catch (IOException e) {
			// e.printStackTrace();
			System.out.println("sim.config file not found, exit");
			System.exit(0);
		}

	}
	
	public SimMode getSimMode(){
		return simMode;
	}
	
	public void setSimMode(SimMode mode){
		this.simMode = mode;
	}
	public double getSimTime(){
		return simTime;
	}
	
	public void setSimTime(double time){
		this.simTime = time;
	}
	public boolean doCollectEnergyDistribution(){
		return dumpEnergyDistributions;
	}
	public int initialJobsToSkip(){
		return skipInitialJobs;
	}
	
	public void setInitialJobsToSkip(int num){
		this.skipInitialJobs = num;
	}
	public ServiceTimeDistType getServiceTimeDist(){
		return this.serviceDist;
	}
	
	public boolean dumpAbsPercentile(){
		return absPercentile90th;
	}
	
	public TaskSchedulerPolicy getTaskSchePolicy(){
		return this.schePolicy;
	}

	public JobWorkload getJobWorkload(){
		return jobWorkload;
	}
	
	public void setPendingThreshold(int thd){
		this.pendingThreshold = thd;
	}
	
	public int getPendingThreshold(){
		return pendingThreshold;
	}
	private TaskSchedulerType parseScheduler(String schedulerName) {
		// TODO Auto-generated method stub
		if (schedulerName.equals("simple"))
			taskSchedulerType = TaskSchedulerType.SIMPLE;
		else if (schedulerName.equals("test_trans")) {
			taskSchedulerType = TaskSchedulerType.TEST_TRANS;
		} else if (schedulerName.equals("one_server")) {
			taskSchedulerType = TaskSchedulerType.ONE_SERVER;
		} else if (schedulerName.equals("energy_aware")) {
			taskSchedulerType = TaskSchedulerType.ENERGY_AWARE;
		} else {
			taskSchedulerType = null;
		}

		return taskSchedulerType;
	}

	private NetworkLoad getSingleWorkLoad(String all_job) {
		if (all_job == null) {
			System.err.println("erro, job workload is null");
			return NetworkLoad.UNKNOWN;
		}

		else if (all_job.equals("stride")) {
			return NetworkLoad.STRIDE;
		}

		else if (all_job.equals("hotspot")) {
			return NetworkLoad.HOTSPOT;
		}

		else if (all_job.equals("onetoone")) {
			return NetworkLoad.ONETOONE;
		}

		else if (all_job.equals("random")) {
			return NetworkLoad.RANDOM;
		}

		else if (all_job.equals("small_topo_custom")) {
			return NetworkLoad.SMALL_TOPO_CUSTOM;
		}

		else if (all_job.equals("test_trans")) {
			return NetworkLoad.TEST_TRANS;
		}
		
		else if (all_job.equals("flow_test")) {
			return NetworkLoad.FLOW_TEST;
		}
		
		else if (all_job.equals("WSEARCH")) {
			return NetworkLoad.WSEARCH;
		}
		
		else if (all_job.equals("WSERVICE")) {
			return NetworkLoad.WSERVICE;
		}
		
		else if (all_job.equals("DNS")) {
			return NetworkLoad.DNS;
		}
		
		else
			return NetworkLoad.UNKNOWN;
	}

	protected TopoName getTopoType(String topoName) {
		if (topoName.equals("fattree"))
			return TopoName.FAT_TREE;
		else if (topoName.equals("bcube"))
			return TopoName.BCUBE;
		else if (topoName.equals("camcube"))
			return TopoName.CAMCUBE;
		else if (topoName.equals("smalltopo"))
			return TopoName.SMALL_TOPO;
		else if (topoName.equals("fbfly"))
			return TopoName.FBFLY;
		else if (topoName.equals("flowtesttopo"))
			return TopoName.FLOWTEST;
		else
			return TopoName.UNKNOWN;
	}

	public void setConfigName(String _configName) {
		this.configName = _configName;
	}

	public TopoName getmTopo() {
		return mTopo;
	}

	public HashMap<String, String> getTopoParams() {
		return topoParams;
	}
	
	public int getK() {
		return k;
	}

	public int getNumOfSockets() {
		return numOfSockets;
	}

	public int getCoresPerSocket() {
		return coresPerSocket;
	}

	public double getServerSPower() {
		return serverSPower;
	}

	public double getServerPPower() {
		return serverPPower;
	}

	public String getProcessorType() {
		return processorType;
	}

//	public double getSwitchSPower() {
//		return switchSPower;
//	}

	public NetworkRoutingAlgorithm getNetworkRoutingAlgorithm() {
		return networkRoutingAlgorithm;
	}
	
	public double getCoreSwitchBW() {
		return coreSwitchBW;
	}
	
	public double getAggregateSwitchBW() {
		return aggregateSwitchBW;
	}
	
	public double getEdgeSwitchBW() {
		return edgeSwitchBW;
	}
	/**
	 * Gets the server static power in the sleep state provided 
	 * @param sleepState - <br>
	 * 			0 - active<br>
	 * 			1 - c0s0 <br>
	 * 			2 - c1s0 <br>
	 * 			3 - c3s0 <br>
	 * 			4 - c6s0 <br>
	 * 			5 - c6s3 
	 * 			
	 * @return The server's static power for the sleep state provided
	 */
	public double getServerStaticPower(int sleepState) {
	
		double power = 0;
		switch(sleepState) {
		
		case 0:
			power = server_active_static_power;
			break;
		case 1:
			power = server_c0s0_static_power;
			break;
		case 2:
			power = server_c1s0_static_power;
			break;
		case 3: 
			power = server_c3s0_static_power;
			break;
		case 4:
			power = server_c6s0_static_power;
			break;
		case 5:
			power = server_c6s3_static_power;
			break;
		}
		return power;
	}


	/**
	 * Sets the servers static power for the sleepState provided
	 * @param sleepState - <br>
	 * 			0 - active<br>
	 * 			1 - c0s0 <br>
	 * 			2 - c1s0 <br>
	 * 			3 - c3s0 <br>
	 * 			4 - c6s0 <br>
	 * 			5 - c6s3
	 *
	 */
	public void setServerStaticPower(int sleepState, double power) {

		switch(sleepState){
			case 0:
				this.server_active_static_power = power;
				break;
			case 1:
				this.server_c0s0_static_power = power;
				break;
			case 2:
				this.server_c1s0_static_power = power;
				break;
			case 3:
				this.server_c3s0_static_power = power;
				break;
			case 4:
				this.server_c6s0_static_power = power;
				break;
			case 5:
				this.server_c6s3_static_power = power;
				break;
		}
	}

	/**
	 * 
	 * Gets the server dynamic power in the sleepState provided
	 * @param sleepState - <br>
	 * 			0 - active<br>
	 * 			1 - c0s0 <br>
	 * 			2 - c1s0 <br>
	 * 			3 - c3s0 <br>
	 * 			4 - c6s0 <br>
	 * 			5 - c6s3 
	 * 			
	 * @return The server's static power for the sleep state provided
	 */
	public double getServerDynamicPower(int sleepState) {
		
		double power = 0;
		switch(sleepState) {
		
		case 0:
			power = server_active_dynamic_power;
			break;
		case 1:
			power = server_c0s0_dynamic_power;
			break;
		case 2:
			power = server_c1s0_dynamic_power;
			break;
		case 3: 
			power = server_c3s0_dynamic_power;
			break;
		case 4:
			power = server_c6s0_dynamic_power;
			break;
		case 5:
			power = server_c6s3_dynamic_power;
		}
		return power;
	}

	/**
	 * Sets the servers dynamic power for the sleepState provided
	 * @param sleepState - <br>
	 * 			0 - active<br>
	 * 			1 - c0s0 <br>
	 * 			2 - c1s0 <br>
	 * 			3 - c3s0 <br>
	 * 			4 - c6s0 <br>
	 * 			5 - c6s3
	 *
	 */
	public void setServerDynamicPower(int sleepState, double power) {

		switch(sleepState){
			case 0:
				this.server_active_dynamic_power = power;
				break;
			case 1:
				this.server_c0s0_dynamic_power = power;
				break;
			case 2:
				this.server_c1s0_dynamic_power = power;
				break;
			case 3:
				this.server_c3s0_dynamic_power = power;
				break;
			case 4:
				this.server_c6s0_dynamic_power = power;
				break;
			case 5:
				this.server_c6s3_dynamic_power = power;
				break;
		}
	}

	/**
	 * Gets the wakeuptime for the sleep state provided.
	 * @param sleepState - 
	 * 			<br> 1 - C0S0
	 * 			<br> 2 - C1S0
	 * 			<br> 3 - C3S0
	 * 			<br> 4 - C6S0
	 * 			<br> 5 - C6S3
	 * @return the wakeup time of the state that was requested
	 */
	public double getWakeupTime(int sleepState) {
		double wakeup_time = -1;
		switch(sleepState) {
		case 1:
			wakeup_time = c0s0_wakeup_time;
			break;
		case 2:
			wakeup_time = c1s0_wakeup_time;
			break;
		case 3:
			wakeup_time = c3s0_wakeup_time;
			break;
		case 4:
			wakeup_time = c6s0_wakeup_time;
			break;
		case 5:
			wakeup_time = c6s3_wakeup_time;
			break;
		}
		return wakeup_time;
	}
	
	
	/**
	 * Gets the waitingtime for the sleep state provided.
	 * @param sleepState - 
	 * 			<br> 0 - Active
	 * 			<br> 1 - C0S0
	 * 			<br> 2 - C1S0
	 * 			<br> 3 - C3S0
	 * 			<br> 4 - C6S0
	 * 			<br> 5 - C6S3
	 * @return the waiting time of the state that was requested
	 */
	public double getWaitingTime(int sleepState) {
		double waiting_time = -1;
		switch(sleepState) {
		case 1:
			waiting_time = c0s0_waiting_time;
			break;
		case 2:
			waiting_time = c1s0_waiting_time;
			break;
		case 3:
			waiting_time = c3s0_waiting_time;
			break;
		case 4:
			waiting_time = c6s0_waiting_time;
			break;
		case 5:
			waiting_time = c6s3_waiting_time;
			break;
		}
		return waiting_time;
	}

	/**
	 * Sets the wakeup time of the sleep state provided to the wakeup time that is provided
	 * @param sleepState
	 * @param wakeupTime
	 */
	public void setWakeupTime(int sleepState, double wakeupTime) {
		switch(sleepState) {
		case 1:
			c0s0_wakeup_time = wakeupTime;
			break;
		case 2:
			c1s0_wakeup_time = wakeupTime;
			break;
		case 3:
			c3s0_wakeup_time = wakeupTime;
			break;
		case 4:
			c6s0_wakeup_time = wakeupTime;
			break;
		case 5:
			c6s3_wakeup_time = wakeupTime;
			break;
		}
	}

	/**
	 * Sets the waiting time of the sleep state provided to the waiting time that is provided
	 * @param sleepState
	 * @param waitingTime
	 */
	public void setWaitingTime(int sleepState, double waitingTime) {
		switch(sleepState) {
		case 1:
			c0s0_waiting_time = waitingTime;
			break;
		case 2:
			c1s0_waiting_time = waitingTime;
			break;
		case 3:
			c3s0_waiting_time = waitingTime;
			break;
		case 4:
			c6s0_waiting_time = waitingTime;
			break;
		case 5:
			c6s3_waiting_time = waitingTime;
			break;
		}
	}

	/**
	 * 
	 * Gets the Line card S Power
	 * @return lcSPower
	 */
//	public double getLcSPower() {
//		return lcSPower;
//	}
	
	public LineCardPowerPolicy getLineCardPowerPolicy() {
		return lineCardPowerPolicy;
	}
	
	public PortPowerPolicy getPortPowerPolicy() {
		return portPowerPolicy;
	}
	
	public double getCoreLcActivePower() {
		return coreLcActivePower;
	}
	
	public double getCoreLcLPI1Power() {
		return coreLcLPI1Power;
	}
	
	public double getCoreLcLPI2Power() {
		return coreLcLPI2Power;
	}

	public double getCoreLcLPI3Power() {
		return coreLcLPI3Power;
	}
	
	public double getAggregateLcActivePower() {
		return aggregateLcActivePower;
	}
	
	public double getAggregateLcLPI1Power() {
		return aggregateLcLPI1Power;
	}
	
	public double getAggregateLcLPI2Power() {
		return aggregateLcLPI2Power;
	}

	public double getAggregateLcLPI3Power() {
		return aggregateLcLPI3Power;
	}
	
	public double getEdgeLcActivePower() {
		return edgeLcActivePower;
	}
	
	public double getEdgeLcLPI1Power() {
		return edgeLcLPI1Power;
	}
	
	public double getEdgeLcLPI2Power() {
		return edgeLcLPI2Power;
	}

	public double getEdgeLcLPI3Power() {
		return edgeLcLPI3Power;
	}
	
	public double getLcWaitTime(int currentState) {
		switch(currentState) {
			case 0:  return lcActiveToLPI1Time;
			case 1:  return lcLPI1ToLPI2Time;
			case 2:  return lcLPI2ToLPI3Time;
			case 3:  return lcLPI3ToOffTime;
			default: Sim.fatalError("Line card is in an invalid sleep state and cannot transition into a deeper one.");
					 return -1;
		}
	}
	
	public double getServerSleepWaitTime(int currentState) {
		switch(currentState) {
			case 0:  return ServerActiveTo1Time;
			case 1:  return ServerActiveTo4Time;
			case 2:  return ServerActiveTo4Time;
			case 3:  return ServerActiveTo4Time;
			case 4:  return ServerActiveTo5Time;
			case 5:  return ServerActiveTo5Time;
			
			default: Sim.fatalError("Line card is in an invalid sleep state and cannot transition into a deeper one.");
					 return -1;
		}
	}
	public double getSwitchTransDelay() {
		return switchTransDelay;
	}
	
	public int getPortsPerLineCard() {
		return portsPerLineCard;
	}

	public Vector<Long> getPortRates() {
		return portRates;
	}

	public Vector<Double> getPortPower() {
		return portPower;
	}

	public int getPortBuffSize() {
		return portBuffSize;
	}

	public int getNumOfJobs() {
		if (numOfJobs == 0)
			numOfJobs = jobNumRange.getIntConfig();

		return numOfJobs;
	}

	// public RandomRange getJobNumRange() {
	// return jobNumRange;
	// }

	public JobArrival getJobArrival() {
		return jobArrival;
	}

	public int getNumOfTasks() {
		if (numOfTasks == 0)
			return taskNumRange.getIntConfig();
		else
			return numOfTasks;
	}

	public double getTaskSize() {
		if (taskSize == 0){
			return 0.001*(double)taskSizeRange.getIntConfig();}
		else
			return taskSize;
	}

	// public RandomRange getTaskSizeRange() {
	// return taskSizeRange;
	// }
	//
	// public RandomRange getTaskNumRange() {
	// return taskNumRange;
	// }

	public Vector<NetworkLoad> getNetworkLoads() {
		return networkLoads;
	}

	public boolean isSingleWorkload() {
		return isWorkloadSingle;
	}

	public NetworkLoad getAllWorkload() {
		return allWorkload;
	}

	public Routing getRouting() {
		return this.routing;
	}

	public int getPacketNum() {
		// TODO Auto-generated method stub
		if (packetNum == 0){
			return packetNumRange.getIntConfig();}
		else
		return packetNum;
	}

	public PortRate getPortRate() {
		// TODO Auto-generated method stub
		return portRate;
	}

	public TaskSchedulerType getTaskSchedulerType() {
		return taskSchedulerType;
	}

	// public void setTaskScheduler(AbstractTaskScheduler theTaskScheduler) {
	// this.taskScheduler = theTaskScheduler;
	// }

	public int getInitialSleepState() {
		return sleepState;
	}

	public void setInitialSleepState(int sleepState) {
		this.sleepState = sleepState;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getRou() {
		return rou;
	}

	public void setRou(double rou) {
		this.rou = rou;
	}

	public double getuBar() {
		return uBar;
	}

	public void setuBar(double uBar) {
		this.uBar = uBar;
	}

	public boolean isSeedFixed() {
		return isSeedFixed;
	}

	public long getSsSeed() {
		return ssSeed;
	}

	public long getSeed() {
		return seed;
	}

	public boolean isVerbose() {
		// TODO Auto-generated method stub
		return verbose;
	}

	public double getPb() {
		return pb;
	}

	public void setEnforcePeriod(double enforcePeriod) {
		// TODO Auto-generated method stub
		this.enforcePeriod = enforcePeriod;
	}

	public double getEnforcePeriod() {
		return this.enforcePeriod;
	}

	public boolean isUseMultipleSS() {
		return useMultipleSS;
	}

	public void setUseMultipleSS(boolean useMultipleSS) {
		this.useMultipleSS = useMultipleSS;
	}

	public void setTaos(double waitings[]) {
		taos = waitings;
	}

	public double[] getTaos() {
		return taos;
	}

	public void setSingleTao(double tao){
		if(tao < 0.0)
			Sim.fatalError("fatal error: single tao less than 0 : " + tao);
		this.singleTao = tao;
	}
	
	public double getSingleTao(){
		return this.singleTao;
	}
	public AbstractWorkloadGen getJobWorkloadGen() {

		// initialize other variables
		
		if(abWorkloadGen != null){
			return abWorkloadGen;
		}
		
		u = 1.0 / uBar;
		lambda = rou * u * serversToSch * numOfCores*4;

		boolean randomSeed = seed == -1 ? true : false;

		if(this.getExperiment() instanceof LinecardNetworkExperiment) {
			switch(multiTaskJobWorkload) {
			case POISSON:
				if(randomSeed) {
					abWorkloadGen = new SSWorkloadGen(lambda, u, true, this);
				}
				else {
					abWorkloadGen = new SSWorkloadGen(lambda, u, false, this);
				}
				break;
			case USER_SPECIFY:
				lambda = 1.0 / interArrivalTime;
				if(randomSeed) {
					abWorkloadGen = new SSWorkloadGen(lambda, u, true, this);
				}
				else {
					abWorkloadGen = new SSWorkloadGen(lambda, u, false, this);
				}
				break;
			default:
				abWorkloadGen = null;
			}
		}
		else {
			switch (jobWorkload) {
			case POISSON:
				if (randomSeed)
					abWorkloadGen = new SSWorkloadGen(lambda, u, true, this);
				else
					abWorkloadGen = new SSWorkloadGen(lambda, u, false, this);
				break;
			case MMPP:
				abWorkloadGen = new MMPPWorkloadGen(rou * u, u, serversToSch, numOfCores,
						randomSeed, aRatio, tRatio, cycle, this);
				break;
				
			case MMPPFLU:
				abWorkloadGen = new MMPPFluentWorkloadGen(rou * u, u, serversToSch, numOfCores,
						randomSeed, aRatio, tRatio, cycle, this);
				break;
				
			case MMPPORAC:
				abWorkloadGen = new MMPPOracleGen(rou * u, u, serversToSch, numOfCores,
						randomSeed, aRatio, tRatio, cycle, this, mExperiment);
				break;
			case MIXED:
				// abWorkloadGen = new MixedWorkloadGen(rou, mixture);
				// use MixedWorkloadGenEZ instead
				abWorkloadGen = new MixedWorkloadEZGen(rou, mixture, serversToSch);
				break;
			case TRACE:
				abWorkloadGen = new TraceWorkloadGen(traceFileName, 50, numOfCores, rou, false, this);
				break;
			default:
				abWorkloadGen = null;
			}	
		}

		return abWorkloadGen;
	}

	public boolean isPrintFullHis() {
		return printFullHis;
	}

	public boolean isPrintAggStats() {
		return printAggStats;
	}

	public int getServersToSchedule() {
		return serversToSch;
	}

	public void setServersToSchedule(int numberOfServers) {
		this.serversToSch = numberOfServers;
	}

	// public double getLambda() {
	// return lambda;
	// }
	//
	// public double getU() {
	// return u;
	// }

	public int getShallowState() {
		return shallowState;
	}

	public int getDeepState() {
		return deepState;
	}

	public void setNumOfJobs(int numOfJobs) {
		// TODO Auto-generated method stub
		if(simMode == SimMode.FIXED_JOBS)
			this.numOfJobs = numOfJobs;
		else
			this.numOfJobs = -1;

	}
	
	public void setServerQueueThreshold(int threshold) {
		serverQueueThreshold = threshold;
	}
	
	public int getServerQueueThreshold() {
		return serverQueueThreshold;
	}

	public double getMeanDelayTime() {
		return this.meanDelayTime;
	}
	
	public double getMeanDelayTime2(){
		return this.meanDelayTime2;
	}

	/**
	 * @param delay: meandelay from s4->s5
	 */
	public void setMeanDelayTime(double delay) {
		this.meanDelayTime = delay;
	}
	
	public double getMeanLowDelayTime(){
		return this.meanLowDelayTime;
	}
	
	/**
	 * @param delay: meandelay from idle->s4
	 */
	public void setMeanDelayTime2(double delay) {
		this.meanDelayTime2 = delay;
	}
	

	public void setMeanLowDelayTime(double lowTime){
		 this.meanLowDelayTime = lowTime;
	}
	public int getDebugLevel() {
		return this.debugLevel;
	}

	public boolean preloadPath() {
		return this.preloadPaths;
	}

	public boolean doCollectStats() {
		// TODO Auto-generated method stub
		return collectStats;
	}

	public void setPredictorType(double type) {
		this.predictorType = type;
	}

	public double getPredictorType() {
		return this.predictorType;
	}

	public boolean doTimingCheck() {
		return doTimingCheck;
	}

	public boolean dumpPrediction() {
		return dumpPrediction;
	}

	public double getLatencyPercent() {
		return this.latencyPercent;
	}

	public boolean dumpSchServerHis() {
		return this.dumpSchServerHis;
	}

	public boolean doCollectActServerHis() {
		return this.dumpActServerHis;
	}

	public boolean doCollectQueueHis() {
		return this.dumpQueueHis;
	}

	public boolean doCollectServerDetails() {
		return this.dumpAllServerDetails;
	}

	public boolean doCollectIdleDistribution() {
		return this.dumpIdleDistribution;
	}

	public boolean isAsynLogging() {
		return asynLogging;
	}

	public boolean isCircularIdleStats() {
		return this.circularIdleStats;
	}

	public boolean doCollectJobArrivals() {
		return this.dumpJobArrivals;
	}

	public boolean doCollectJobCompletes() {
		return this.dumpJobCompletes;
	}

	public boolean doCollectStateDurations() {
		return this.dumpStateDurations;
	}
	
	public double getS4ProvisionPercent(){
		return this.s4Provision;
	}

	public void setHighDelayServerNum(int parseInt) {
		// TODO Auto-generated method stub
		this.lowDelayServerNum = parseInt;
		
	}
	
	public int getHighDelayServerNum(){
		return this.lowDelayServerNum;
	}

	public void setNumOfCores(int numOfCores) {
		// TODO Auto-generated method stub
		this.numOfCores = numOfCores;
	}
	
	public int getNumOfCores() {
		return numOfCores;
	}

	public void setJobNumOnTime() {
		System.out.println("setjobnumontime"+this.numOfJobs);
		// TODO Auto-generated method stub
		lambda = rou * serversToSch * numOfCores / (4*uBar);
		this.numOfJobs = (int)(simTime * lambda); 
	}

	public void setExperiment(Experiment experiment) {
		// TODO Auto-generated method stub
		this.mExperiment = experiment;
		
	}
	
	public Experiment getExperiment(){
		return mExperiment;
	}
	
	public boolean doPacketRouting(){
		return this.packetRouting;
	}
	
	public double getAvgFlowSize(){
		return flowSize;
	}

	public int getLPIB() {
		return LPIB;
	}

	public void setLPIB(int lPIB) {
		LPIB = lPIB;
	}
	
	public void setSleepUnit(SleepUnit sleepUnit) {
		this.sleepUnit = sleepUnit;
	}
	
	public SleepUnit getSleepUnit() {
		return sleepUnit;
	}
	
	public void setInterArrivalTime(double interArrivalTime) {
		this.interArrivalTime = interArrivalTime;
	}
	
	public double getInterArrivalTime() {
		return interArrivalTime;
	}
	
	public int getJobQoS() {
		return jobQoS; 
	}
	
	public int getSamplingRate() {
		return samplingRate;
	}
	
	public void setSamplingRate(int rate) {
		samplingRate = rate;
		
	}
	
	public int getFrames() {
		return frame_size;
	}
	
	public void setFrames(int frame) {
		frame_size = frame;
	}
	public int getLinkAnimation() {
		return link_animation;
	}
	
	public void setLinkAnimation(int linkAnimation) {
		link_animation=linkAnimation;
	}
}
