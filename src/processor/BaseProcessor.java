package processor;

import infrastructure.Core;
import infrastructure.Server;

import java.util.HashMap;

import job.Task;

import experiment.Experiment;




/**
 * @author Fan Yao base class for processors
 * 
 */
public abstract class BaseProcessor {

	
	/**
	 * The server this Processor belongs to.
	 */

	/**
	 * A mapping of Tasks to cores. This allows bookeeping of cores when Tasks
	 * finish.
	 */
	private HashMap<Task, Core> TaskToCoreMap;

	public static enum BaseProcessorPowerPolicy {
		/**
		 * Various custom power management policy for processor
		 */
		CUSTOM1, CUSTOM2
	};

	private Server server;

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}


	/**
	 * The number of cores in this Processor.
	 */
	private int nCores;

	/**
	 * The experiment the Processor is part of.
	 */
	
}
