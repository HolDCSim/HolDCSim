package stochastic;

import java.util.*;

public class UniformRandom extends RandomGenerator {

	private double expectedValue;
	private double span;
	private Random rand;
	private double start;
	private static double FLUCRANGE = 0.50;
	
	//to keep consistency with ExponentialRandom, use lambda instead of average value
	public UniformRandom(double lambda, long seed){

		this.expectedValue = 1.0 / lambda;
		span = 2 * expectedValue * FLUCRANGE;
		start = expectedValue * (1.0 - FLUCRANGE);
		rand = new Random(seed);
	}
	
	@Override
	public double getNextDouble() {
		// TODO Auto-generated method stub
		double u = rand.nextDouble();
		//return start + span;
		return start + u * span;
	}

}
