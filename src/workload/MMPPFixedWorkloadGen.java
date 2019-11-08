package workload;

import java.util.Random;

import debug.Sim;

import stochastic.ExponentialRandom;

public class MMPPFixedWorkloadGen extends AbstractWorkloadGen {

	// consider a mixed workload of google and dns like jobs

	public int numOfJobsGenerated;
	/**
	 * proportion denotes the fraction of google like jobs should be configured
	 * in sim.config file
	 */

	private double internalServiceTime;
	// factor that changes from active to idle
	private double switchActiveToIdle;

	private int transitionFreq;

	private Random switchToIdleRam;

	ExponentialRandom activeInterArrivalGen;
	ExponentialRandom activeServiceTimelGen;

	private ExponentialRandom idleInterArrivalGen;

	public MMPPFixedWorkloadGen(double lambda, double u) {

		// switchToActiveRam = new Random(200);
		transitionFreq = 50;

		// FIXME: seed should be parameterized
		if (Sim.RANDOM_SEEDING) {
			
			switchToIdleRam = new Random(System.currentTimeMillis());
			activeInterArrivalGen = new ExponentialRandom(lambda, System.currentTimeMillis());
			activeServiceTimelGen = new ExponentialRandom(u, System.currentTimeMillis());
			idleInterArrivalGen = new ExponentialRandom(lambda / transitionFreq, System.currentTimeMillis());
		} else {
			switchToIdleRam = new Random(100);
			activeInterArrivalGen = new ExponentialRandom(lambda, 10000);
			activeServiceTimelGen = new ExponentialRandom(u, 20000);
			idleInterArrivalGen = new ExponentialRandom(lambda / transitionFreq, 30000);
		}
		/********************************************/
	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {
		// TODO Auto-generated method stub
		return getNextInterArrivalFixed();
	}

	@Override
	public double getNextServiceTime() {
		// TODO Auto-generated method stub
		return getNextServiceTimeFixed();
	}

	private double getNextInterArrivalFixed() {
		// TODO Auto-generated method stub
		if ((numOfJobsGenerated) % transitionFreq == 0
				&& numOfJobsGenerated != 0) {
			if (Double
					.compare(switchToIdleRam.nextDouble(), switchActiveToIdle) < 0) {
				numOfJobsGenerated++;
				internalServiceTime = activeServiceTimelGen.getNextDouble();
				return activeInterArrivalGen.getNextDouble();
			}else {
				numOfJobsGenerated++;
				double idleDuration = idleInterArrivalGen.getNextDouble();
				double activeDuration = activeInterArrivalGen.getNextDouble();
				activeDuration += idleDuration;
				internalServiceTime = activeServiceTimelGen.getNextDouble();
				return activeDuration;
			}
		}else {
			numOfJobsGenerated++;
			internalServiceTime = activeServiceTimelGen.getNextDouble();
			return activeInterArrivalGen.getNextDouble();
		}

	}

	private double getNextServiceTimeFixed() {
		return internalServiceTime;
	}

	@Override
	public int getJobType() {
		// TODO Auto-generated method stub
		return 0;
	}

}
