package constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for the entire simulator.
 *
 * @author David Meisner (meisner@umich.edu)
 */
/**
 * @author fan
 * 
 */
public final class Constants {

	/**
	 * Should never be called.
	 */
	private Constants() {

	}

	public static enum NodeState {
		ON, SLEEPING, OFF
	}

	public static enum TopoName {
		FBFLY, FAT_TREE, CAMCUBE, BCUBE, SMALL_TOPO, FLOWTEST, UNKNOWN
	}

	public static enum TopoClass {
		SWITCH_ONLY, SERVER_ONLY, HYBRID
	}

	public static enum JobArrival {
		fixed, random1, random2
	}
	

	/**
	 * Outputs which can be monitored as a Statistic have to be defined here.
	 */
	public static enum StatName {
		/** Length in time of idle periods. */
		IDLE_PERIOD_TIME,

		/** Per Task response time. */
		SOJOURN_TIME,

		/** Amount of capping (in watts) for a cluster. */
		TOTAL_CAPPING,

		/** Fraction of time a server is completely idle. */
		FULL_SYSTEM_IDLE_FRACTION,

		/** Amount of time a server is busy. */
		BUSY_PERIOD_TIME,

		/** The interarrival time as generated in the simulation. */
		GENERATED_ARRIVAL_TIME,

		/** The Task service time as generated in the simulation. */
		GENERATED_SERVICE_TIME,

		/** Per-Task wait time. */
		WAIT_TIME,

		/** Amount of capping (in watts) for an individual server. */
		SERVER_LEVEL_CAP

		// fanyao added

	}

	/**
	 * Outputs which can be monitored as a TimeWeightedStatistic have to be
	 * defined here.
	 */
	public static enum TimeWeightedStatName {
		/** Time-weighted server power. */
		SERVER_POWER,

		/** Time-weighted server utilization. */
		SERVER_UTILIZATION,

		/** Time-weighted fraction of time a server is idle. */
		SERVER_IDLE_FRACTION,

		// fanyao added
		PORT_UTILIZATION,

		THROUGHPUT,

		TOTAL_THROUGHPUT,

		TASK_EVENT,

		/** Time-weighted switch power. */
		SWITCH_POWER
	}

	/* Power breakdown for servers. */
	// TODO(meisner@umich.edu) Fill in these values.
	/** Power of "other" components at idle. */
	public static final int SERVER_IDLE_OTHER_POWER = 1;

	/** Dynamic power of "other" components at max. */
	public static final int SERVER_DYN_OTHER_POWER = 1;

	/** Power of the memory system at idle. */
	public static final int SERVER_IDLE_MEM_POWER = 1;

	/** Dynamic power of the memory system at max. */
	public static final int SERVER_DYN_MEM_POWER = 1;

	/** Power of CPU "uncore" at idle. */
	//fanyao modified: no idle power related to socket
	public static final int SOCKET_IDLE_POWER = 0;

	/** Power transitioning the "uncore" of a CPU. */
	public static final int SOCKET_TRANSITION_POWER = 1;

	/** Power of CPU "uncore" while in socket parking. */
	public static final int SOCKET_PARK_POWER = 0;

	/** Dynamic power of CPU core at max. */
	public static final int CORE_ACTIVE_POWER = 1;

	/** Power of CPU core components at idle. */
	public static final int CORE_IDLE_POWER = 1;

	/** Power of transitioning a core in/out of park. */
	public static final int CORE_TRANSITION_POWER = 1;

	/** Power of CPU core while parked. */
	public static final int CORE_PARK_POWER = 1;

	/* Transition times */

	/** The time to transition the socket into park. */
	//fanyao commented: remove the final option so that the parameter could be set later
	public static double SOCKET_PARK_TRANSITION_TIME = 500e-6;
	
	//fanyao commented: transition to park time for core
	public static double CORE_TRANSITION_TO_PAKR = 1e-6;

	/** Maximum length of queues. */
	public static final int MAX_QUEUE_SIZE = 500000;

	/* Values for statistical tests */
	/** Z value for 95th-percentile confidence from a normal distribution. */
	public static final double Z_95_CONFIDENCE = 1.96;

	/** Parameter for 95th-percentile in a Chi-squared test. */
	public static final double CHI_2_95_TEST = 12.592;

	/** Time window over which to compute utilization of a machine. */
	public static final double DEFAULT_UTILIZATION_WINDOW = .01;

	/** Time window over which to compute idleness of a machine. */
	public static final double DEFAULT_IDLENESS_WINDOW = .01;

	/* Constants for Statistics */
	/** Size of the buffer for the runs test. */
	public static final int RUNS_TEST_BUFFER_SIZE = 50000;

	/** The minimum number of samples to converge a statistic. */
	public static final long MINIMUM_CONVERGE_SAMPLES = 10;

	/** The maximum stride length between samples before giving up. */
	public static final int GIVE_UP_STRIDE = 100;

	/** The level of verbosity for debugging. */
	public static final int DEBUG_VERBOSE = 5;
	
	/**
	 * maximum number of messages between two tasks, used for random message
	 * generation.
	 */
	public static final int MAX_NUM_OF_MESSAGES = 2;

	// TODO: task size could be float type also

	/**
	 * the size of the buffer in each port (number of messages)
	 */
	public static final int PORT_BUFFER_SIZE = 100000;

	public static final int SIZE_TO_WRITE_FILE = 10000000; // 20M

	/**
	 * maximum number of packets between two tasks, used for random packet
	 * generation.
	 */
	public static final int MAX_NUM_OF_PACKETS = 100;

	/**
	 * NUMBER OF JOBS in experiment
	 */
	public static final int NUM_OF_JOB = 2;
	/**
	 * maximum size of job, used to generate random number of tasks in a job
	 */
	public static final int MAX_SIZE_OF_JOB = 8;
	// TODO: task size could be float type also
	/**
	 * maximum size of task, used to generate random length of task
	 */
	public static final int MAX_SIZE_OF_TASK = 5;

	/**
	 * the size of the buffer in each port (number of packets)
	 */
	public static final int PORT_BUFFER_SIZE_SERVER = 10000;
	public static final int PORT_BUFFER_SIZE_SWITCH = 15360; // 60M/4k

	/**
	 * the probability of dependency between tasks
	 */
	public static final double PROB_OF_DEPEND = 0.5;

	public static final int NUM_OF_CORE = 1;
	public static final int NUM_OF_SOCKET = 1;

	/**
	 * the chasis power in a switch in Watt
	 */
	public static final double POWER_CHASIS = 55;

	public static final double LC_SLEEP2ACTIVE = 200.48e-5;
	public static final double LC_OFF2ACTIVE = 200.48e-3;
	public static final double LC_SLEEP2OFF = 5;
	public static final double LC_ACTIVETOSLEEP = 1.48e-6;
	
	//delay to wake up a port after waking up the linecard
	public static final double PortWakeupAfterLC = 2e-6;
	
	/* Linecard minimum wakeup and sleep times for different link speeds, 100Mbps, 1000Mbps, 10Gbps */
//	public static double[] sleepStateWakeupsLC = { 30.5e-5, 25e-5, 4.48e-5};
//	public static double[] sleepStateWaitingsLC = { 200e-5, 182e-5, 2.88e-5};
	
	/* LPI minimum wakeup and sleep times for different link speeds, 100Mbps, 1000Mbps, 10Gbps */
	public static double[] sleepStateWakeupsPort = { 30.5e-6, 16.5e-6, 4.48e-6};
	public static double[] sleepStateWaitingsPort = { 200e-6, 182e-6, 2.88e-6};
	
	/* if the queue of port is not full, wake up the port after this delay*/
	public static double portWakeupTao = 2e-6;
	public static double LCWakeupTao = 2e-3;
	public static final double POWER_LINECARD = 39.5;
	
	public static final double POWER_LINECARD_L0 = 39.5;
	public static final double POWER_LINECARD_L1 = 39.5;
	public static final double POWER_LINECARD_L3 = 39.5;
	public static final double POWER_LINECARD_L6 = 39.5;

	/**
	 * power per enabled port in Watt
	 */
	public static final double POWER_ENABLEDPORT = 0.25;
	
	/**
	 * Switch base power in Watt
	 */
	public static final double POWER_SWITCH_BASE = 120;
	/**
	 * power per active port in Watt
	 */
	public static final double POWER_ACTIVE_PORT = 1.21;
	
	/**
	 * port LPI power
	 */
	public static final double POWER_LPI_PORT = 1.01;

	//public static final double tao = 0;//0.0001;
	
	/**
	 * switch latency for turn on/off
	 */
	public static final double LATENCY_SWITCH = 0;
	
	/**
	 * number of params required for fat tree
	 */
	public static final int FAT_TREE_PARAMS = 1;
	
	
	/**
	 * number of params required for flattened butterfly 
	 */
	public static final int FBFLY_PARAMS = 2;

	/**
	 * number of params required for bcude 
	 */
	public static final int BCUBE_PARAMS = 2;
	
	public static final boolean PRIORITIZE_JOB = true;
	public static enum TaskPriority{SHORTJOB_FIRST, EARLIEST_STARTTIME};
	public static final TaskPriority taskPriority = TaskPriority.EARLIEST_STARTTIME; 
	
	/**
	 *number of params required for camcube 
	 */
	public static final int CAMCUBE_PARAMS = 1;
	/**
	 * Routing related delays needs to be set according to link rate
	 */
	public static class RoutingDelay {
//		public static final double PACKET_TRANSMIT_DELAY = 0.005;
//		public static final double PROPOGATIONG_DELAY = 0.002;
//		public static final double PACKET_ARRIVE_DELAY = 0.002;
		
		public static final double PACKET_TRANSMIT_DELAY = 0.0002;
		public static final double PROPOGATIONG_DELAY = 0.0001;
		public static final double PACKET_ARRIVE_DELAY = 0.0002;
		public static final double PACKET_SEQUENCE_DELAY = 0.00001;
	}

	//public static Double[] PORTRATES = { 10.0, 40.0, 20.0, 80.0, 40.0, 160.0 };
	public static Double[] PORTRATES = { 100.0, 1000.0,10000.0 }; //Mbps

//	public static enum PortRate {
//		SDR1, SDR4, DDR1, DDR4, QDR1, QDR4, NUM_OF_RATES
//	};
	public static enum PortRate {
		Base100, Base1000, Base10G, NUM_OF_RATES
	};
	
	public static double TIMING_RRECISION = 0.00001;
	
	public static Map<PortRate, Double> portRateMap = new HashMap<PortRate, Double>(){
		private static final long serialVersionUID = 1L;

	{
		/*put(PortRate.SDR1, 10.0);
		put(PortRate.SDR4, 40.0);
		put(PortRate.DDR1, 20.0);
		put(PortRate.DDR4, 80.0);
		put(PortRate.QDR1, 40.0);
		put(PortRate.QDR4, 160.0);*/
		put(PortRate.Base100,100.0);
		put(PortRate.Base1000,1000.0);
		put(PortRate.Base10G,10000.0);
	}};
	
	public static enum SleepUnit{
		SOCKET_LEVEL,
		CORE_LEVEL,
		SOCKET_CORE_COMBINED,
		NO_SLEEP
	}
	
	public static SleepUnit sleepUnit = SleepUnit.CORE_LEVEL;
}
