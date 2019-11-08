package workload;

import java.util.*;

import debug.Sim;
import experiment.ExperimentConfig;

import stochastic.ExponentialRandom;
import utility.Pair;

public class MMPPWorkloadGen extends AbstractWorkloadGen {

	public enum MMPPState {
		HIGHU, LOWU
	}

	// consider a mixed workload of google and dns like jobs

	public int numOfJobsGenerated;
	/**
	 * proportion denotes the fraction of google like jobs should be configured
	 * in sim.config file
	 */

	// private double internalServiceTime;
	// // factor that changes from active to idle
	// private double switchActiveToIdle;
	//
	// // factor that changes from idle to active
	// private double switchIdleToActive;
	//
	// private int transitionFreq;
	//
	// private Random switchToIdleRam;
	// private Random switchToActiveRam;
	//
	// ExponentialRandom activeInterArrivalGen;
	// ExponentialRandom activeServiceTimelGen;
	//
	// private ExponentialRandom idleInterArrivalGen;

	/*****************************************/
	// state occupancy time active : inactive ratio
	// private double tRatio = 0.5;
	// // arivate rate ratio active : inactive
	// private double aRatio = 16;

	/**
	 * ratio of duration time between active and inactive
	 */
	protected double tRatio = 0.5;
	/** arivate rate ratio active : inactive */
	protected double aRatio = 16;
	
	protected double lambdaActive;
	protected double lambdaInactive;
	
	private double durationActive;
	private double durationInactive;

	/*****************************************/
	// the average total time for two stages in seconds
	double timeUnit = 30.0;

	// exponential random variable for on state occupancy
	protected ExponentialRandom highOccupancyGen;

	// exponential random variable for off state occupancy
	protected ExponentialRandom lowOccupancyGen;

	// use SSWorkloadGen for on state
	SSWorkloadGen ssHighUgen;
	SSWorkloadGen ssLowUGen;

	protected double highUStateDuration;
	protected double lowUStateDuration;

	protected MMPPState currentState;

	protected double lastStateStartTime;

	// history of mmpp on/off status
	protected ArrayList<Pair<Double, Integer>> mmppHis;

	protected double presetServiceTime;

	public MMPPWorkloadGen(double lambda, double u, int numOfServers, int numOfCores,
			boolean randomSeed, double arrivalRatio, double tRatio, double cycle, ExperimentConfig expConfig) {
		// /********************************************/
		// switchActiveToIdle = 0.5;
		// switchIdleToActive = 1.0;
		//
		// switchToIdleRam = new Random(100);
		// switchToActiveRam = new Random(200);
		//
		// transitionFreq = 50;
		//
		// activeInterArrivalGen = new ExponentialRandom(lambda, 10000);
		// activeServiceTimelGen = new ExponentialRandom(u, 20000);
		//
		// idleInterArrivalGen = new ExponentialRandom(lambda / transitionFreq,
		// 30000);
		// /********************************************/
		;
		/********************************************/
		/*
		 * mmppHis = new ArrayList<Pair<Double, Integer>>(); Pair<Double,
		 * Integer> pair = new Pair<Double, Integer>(); pair.setFirst(0.0);
		 * pair.setSecond(1); mmppHis.add(pair);
		 */

		if (arrivalRatio != 0.0)
			this.aRatio = arrivalRatio;
		
		if (tRatio != 0.0)
			this.tRatio = tRatio;
		
		if (cycle != 0.0)
			this.timeUnit = cycle;
		currentState = MMPPState.HIGHU;
		lastStateStartTime = 0.0;

		// now let lambda be the average lambda
		// we create different burstiness based on the average lambda
		lambdaInactive = lambda * (tRatio + 1) / (tRatio * aRatio + 1);
		lambdaActive = aRatio * lambdaInactive;

		durationActive = timeUnit * tRatio / (1 + tRatio);
		durationInactive = timeUnit - durationActive;

		if(Sim.RANDOM_SEEDING){
			highOccupancyGen = new ExponentialRandom(1 / durationActive, System.currentTimeMillis());
			lowOccupancyGen = new ExponentialRandom(1 / durationInactive, System.currentTimeMillis());	
		}
		else{
			highOccupancyGen = new ExponentialRandom(1 / durationActive, 1000);
			lowOccupancyGen = new ExponentialRandom(1 / durationInactive, 2000);
		}
		
		ssHighUgen = new SSWorkloadGen(lambdaActive * numOfServers * numOfCores, u, 7, expConfig);
		ssLowUGen = new SSWorkloadGen(lambdaInactive * numOfServers * numOfCores, u, 31, expConfig);

		/********************************************/

	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {

		if (Double.compare(currentArrivalTime, 0.0) == 0) {
			// generate on state duration
			highUStateDuration = highOccupancyGen.getNextDouble();
			lowUStateDuration = lowOccupancyGen.getNextDouble();
		}

		// ExponentialRandom toggleDurationGen = null;
		if (currentState == MMPPState.HIGHU) {
			
			presetServiceTime = ssHighUgen.getNextServiceTime();
			Double nextJobArrival = ssHighUgen
					.getNextInterArrival(currentArrivalTime);
			if (Double.compare(currentArrivalTime + nextJobArrival,
					lastStateStartTime + highUStateDuration) <= 0) {

				return nextJobArrival;
			}

			else {
				currentState = MMPPState.LOWU;
				lowUStateDuration = lowOccupancyGen.getNextDouble();

				// collect mmpp on/off history
				/*
				 * Pair<Double, Integer> pair = new Pair<Double, Integer>();
				 * pair.setFirst(lastOnStartTime + activeStateDuration);
				 * pair.setSecond(0); mmppHis.add(pair);
				 */

				lastStateStartTime = lastStateStartTime + highUStateDuration;
				highUStateDuration = -1;
				return lastStateStartTime - currentArrivalTime;
			}

		}

		else {
			
			presetServiceTime = ssLowUGen.getNextServiceTime();
			Double nextJobArrival = ssLowUGen
					.getNextInterArrival(currentArrivalTime);
			if (Double.compare(currentArrivalTime + nextJobArrival,
					lastStateStartTime + lowUStateDuration) <= 0) {

				return nextJobArrival;
			}

			else {
				currentState = MMPPState.HIGHU;
				highUStateDuration = highOccupancyGen.getNextDouble();
				
				double delta = lastStateStartTime + lowUStateDuration
						- currentArrivalTime;

				// collect mmpp on/off history
				/*
				 * Pair<Double, Integer> pair = new Pair<Double, Integer>();
				 * pair.setFirst(lastOnStartTime + activeStateDuration);
				 * pair.setSecond(0); mmppHis.add(pair);
				 */

				// if there is no time to event generate one job
				lastStateStartTime = lastStateStartTime + lowUStateDuration;

				/***************************************************/
				presetServiceTime = ssHighUgen.getNextServiceTime();
				nextJobArrival = ssHighUgen
						.getNextInterArrival(currentArrivalTime);
				
				//TODO: should change to generate one job at end of lowUtilization stage
				//when there are no jobs comming during that time
				if (Double.compare(nextJobArrival, highUStateDuration) <= 0) {
					return nextJobArrival + delta;
				}

				else {
					
					Sim.fatalError("mmpp error flag");
					currentState = MMPPState.LOWU;
					lowUStateDuration = lowOccupancyGen
							.getNextDouble();

					// collect mmpp on/off history
					/*
					 * Pair<Double, Integer> pair = new Pair<Double, Integer>();
					 * pair.setFirst(lastOnStartTime + activeStateDuration);
					 * pair.setSecond(0); mmppHis.add(pair);
					 */

					lastStateStartTime = lastStateStartTime
							+ highUStateDuration;
					highUStateDuration = -1;
					return lastStateStartTime - currentArrivalTime;
				}

				/***************************************************/
				// lastStateStartTime = lastStateStartTime +
				// inactiveStateDuration;
				// inactiveStateDuration = -1;
				// return lastStateStartTime - currentArrivalTime;
			}

		}

		// if (currentState == MMPPState.INACTIVE) {
		// currentState = MMPPState.ACTIVE;
		// activeStateDuration = activeOccupancyGen.getNextDouble();
		// lastOnStartTime = currentArrivalTime;
		//
		// // collect mmpp on/off history
		// /*Pair<Double, Integer> pair = new Pair<Double, Integer>();
		// pair.setFirst(currentArrivalTime);
		// pair.setSecond(1);
		// mmppHis.add(pair);*/
		// }
		//
		// Double nextJobArrival =
		// ssActiveGen.getNextInterArrival(currentArrivalTime);
		// if (Double.compare(currentArrivalTime + nextJobArrival,
		// lastOnStartTime
		// + activeStateDuration) <= 0) {
		// return nextJobArrival;
		// }
		//
		// else {
		// currentState = MMPPState.INACTIVE;
		// double offTime = inActiveOccupancyGen.getNextDouble();
		//
		// // collect mmpp on/off history
		// /*Pair<Double, Integer> pair = new Pair<Double, Integer>();
		// pair.setFirst(lastOnStartTime + activeStateDuration);
		// pair.setSecond(0);
		// mmppHis.add(pair);*/
		//
		// return lastOnStartTime + activeStateDuration - currentArrivalTime
		// + offTime;
		// }

		// return getNextInterArrivalFixed();

	}

	@Override
	public double getNextServiceTime() {

		return presetServiceTime;
		// return ssGen.getNextServiceTime();
		// TODO Auto-generated method stub
		// return getNextInterArrivalFixed();
	}

	public ArrayList<Pair<Double, Integer>> getMMPPHis() {
		return mmppHis;
	}

	@Override
	public int getJobType() {
		// TODO Auto-generated method stub
		return 0;
	}

}
