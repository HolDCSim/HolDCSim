package stochastic;

import java.util.*;

public class ExponentialRandom extends RandomGenerator {
	private long seed;
	private double lambda;
	Random rand;

	public ExponentialRandom(double lambda, long seed) {
		this.lambda = lambda;
		this.seed = seed;
		rand = new Random(this.seed);
	}

	public double getNextDouble() {

		double u = rand.nextDouble();

		return (-Math.log(1 - u) / lambda);
	}

}
