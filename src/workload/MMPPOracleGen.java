package workload;

import scheduler.OnSleepScheduler;
import workload.MMPPWorkloadGen.MMPPState;
import debug.Sim;
import event.OracleLoadEvent;
import experiment.*;
//import experiment.ExperimentConfig;

public class MMPPOracleGen extends MMPPWorkloadGen {
	
	private int serversNeeded;
	private static int serverProvison = 2;
	private OnSleepScheduler onSleepScheduler;
	private OnSleepExperiment experiment;

	public MMPPOracleGen(double lambda, double u, int numOfServers,
			int numOfCores, boolean randomSeed, double arrivalRatio,
			double tRatio, double cycle, ExperimentConfig expConfig, Experiment experiment) {
		super(lambda, u, numOfServers, numOfCores, randomSeed, arrivalRatio, tRatio,
				cycle, expConfig);
		double highURate = lambdaActive / u;
		serversNeeded = (int)(highURate * numOfServers) + serverProvison;
		this.experiment = (OnSleepExperiment)experiment;
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {

		if (Double.compare(currentArrivalTime, 0.0) == 0) {
			// generate on state duration
			highUStateDuration = highOccupancyGen.getNextDouble();
			lowUStateDuration = lowOccupancyGen.getNextDouble();
			
			//System.out.println("high : " + highUStateDuration);
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

				//generate Oracle load event when the next lowUStateDuration is known
				//FIXME: hardcoded number 0.99, a little bit after the highU begins
				double eventTime = lastStateStartTime + highUStateDuration + lowUStateDuration - 1.0;
				
				if (eventTime > currentArrivalTime) {
					OracleLoadEvent oracleEvent = new OracleLoadEvent(
							eventTime, serversNeeded,
							(OnSleepScheduler) experiment.getTaskScheduler(),
							experiment);
					experiment.addEvent(oracleEvent);
				}
				//System.out.println("low : " + lowUStateDuration);
				
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
				
				//System.out.println("high : " + highUStateDuration);
				
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


}
