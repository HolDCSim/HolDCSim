package workload;

import java.util.ArrayList;

import stochastic.ExponentialRandom;
import utility.Pair;
import workload.MMPPWorkloadGen.MMPPState;
import debug.Sim;
import experiment.ExperimentConfig;

/**
 * assume a fluent transition from low rou to high rou
 * @author fanyao
 *
 */
public class MMPPFluentWorkloadGen extends AbstractWorkloadGen {
	public enum MMPPState {
		HIGHU, 
		LOWU
	}
	
	public enum MMPPSubState{
		TRANS_HIGH, REAL_HIGH, TRANS_LOW
	}

	
	/**        _____
	 *   _____/     \______
	 * */
	
	double climbHill = 0.1;
	double lambdaClimbHill = 0;
	double lambdaRealHigh = 0.0;
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
	private double tRatio = 0.5;
	/** arivate rate ratio active : inactive */
	private double aRatio = 16;
	
	private double lambdaActive;
	private double lambdaInactive;
	
	private double durationActive;
	private double durationInactive;

	/*****************************************/
	// the average total time for two stages in seconds
	double timeUnit = 30.0;

	// exponential random variable for on state occupancy
	private ExponentialRandom highOccupancyGen;

	// exponential random variable for off state occupancy
	private ExponentialRandom lowOccupancyGen;

	// use SSWorkloadGen for on state
	SSWorkloadGen ssHighUgen;
	SSWorkloadGen ssLowUGen;
	
	SSWorkloadGen ssRealHighGen;
	SSWorkloadGen ssClimbHillGen;

	private double highUStateDuration;
	private double transHighDuration;
	private double transLowDuration;
	private double lowUStateDuration;

	private MMPPState currentState;
	
	private MMPPSubState subState;

	private double lastStateStartTime;

	// history of mmpp on/off status
	private ArrayList<Pair<Double, Integer>> mmppHis;

	private double presetServiceTime;

	public MMPPFluentWorkloadGen(double lambda, double u, int numOfServers, int numOfCores,
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
		subState = MMPPSubState.TRANS_HIGH;
		lastStateStartTime = 0.0;

		// now let lambda be the average lambda
		// we create different burstiness based on the average lambda
		lambdaInactive = lambda * (tRatio + 1) / (tRatio * aRatio + 1);
		lambdaActive = aRatio * lambdaInactive;
		lambdaClimbHill = lambdaActive / 2.0;
		lambdaRealHigh = lambdaActive * (1.0 - climbHill) /(1 - 2.0 * climbHill) ;

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
		
		ssClimbHillGen = new SSWorkloadGen(lambdaClimbHill * numOfServers * numOfCores, u, 17, expConfig);
		ssRealHighGen = new SSWorkloadGen(lambdaRealHigh * numOfServers * numOfCores, u, 37, expConfig);

		/********************************************/

	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {

		if (Double.compare(currentArrivalTime, 0.0) == 0) {
			// generate on state duration
			highUStateDuration = highOccupancyGen.getNextDouble();
			transHighDuration = climbHill * highUStateDuration;
			transLowDuration = (1-climbHill) * highUStateDuration;
			
			lowUStateDuration = lowOccupancyGen.getNextDouble();
		}

		// ExponentialRandom toggleDurationGen = null;
		if (currentState == MMPPState.HIGHU) {
			presetServiceTime = ssHighUgen.getNextServiceTime();

			Double nextJobArrival = 0.0;
			if (subState == MMPPSubState.TRANS_HIGH) {
				nextJobArrival = ssClimbHillGen
						.getNextInterArrival(currentArrivalTime);
				if (Double.compare(currentArrivalTime + nextJobArrival,
						lastStateStartTime + transHighDuration) <= 0) {

					return nextJobArrival;
				} else {
					subState = MMPPSubState.REAL_HIGH;
					return lastStateStartTime + transHighDuration
							- currentArrivalTime;
				}
			} else if (subState == MMPPSubState.REAL_HIGH) {
				nextJobArrival = ssRealHighGen
						.getNextInterArrival(currentArrivalTime);

				if (Double.compare(currentArrivalTime + nextJobArrival,
						lastStateStartTime + transLowDuration) <= 0) {

					return nextJobArrival;
				} else {
					subState = MMPPSubState.TRANS_LOW;
					return lastStateStartTime + transLowDuration
							- currentArrivalTime;
				}
			} else {
				nextJobArrival = ssClimbHillGen
						.getNextInterArrival(currentArrivalTime);

				if (Double.compare(currentArrivalTime + nextJobArrival,
						lastStateStartTime + highUStateDuration) <= 0) {

					return nextJobArrival;
				} else {

					currentState = MMPPState.LOWU;
					lowUStateDuration = lowOccupancyGen.getNextDouble();

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
				subState = MMPPSubState.TRANS_HIGH;
				highUStateDuration = highOccupancyGen.getNextDouble();
				
				//update transhigh and translow accordingly
				transHighDuration = climbHill * highUStateDuration;
				transLowDuration = (1-climbHill) * highUStateDuration;

				double delta = lastStateStartTime + lowUStateDuration
						- currentArrivalTime;

				// collect mmpp on/off history
				/*
				 * Pair<Double, Integer> pair = new Pair<Double, Integer>();
				 * pair.setFirst(lastOnStartTime + activeStateDuration);
				 * pair.setSecond(0); mmppHis.add(pair);
				 */

				// if there is no time to even generate one job
				lastStateStartTime = lastStateStartTime + lowUStateDuration;

				/***************************************************/
				presetServiceTime = ssClimbHillGen.getNextServiceTime();
				nextJobArrival = ssClimbHillGen
						.getNextInterArrival(currentArrivalTime);
				if (Double.compare(nextJobArrival, highUStateDuration) <= 0) {

					if(nextJobArrival < transHighDuration){
						subState = MMPPSubState.TRANS_HIGH;
					}
					else if(nextJobArrival < transLowDuration){
						subState = MMPPSubState.REAL_HIGH;
					}
					else{
						subState = MMPPSubState.TRANS_LOW;
					}
					return nextJobArrival + delta;
				}

				else {
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
