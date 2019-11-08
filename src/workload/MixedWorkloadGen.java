package workload;

import java.util.*;

import debug.Sim;

import stochastic.ExponentialRandom;

public class MixedWorkloadGen extends AbstractWorkloadGen {
	
	//consider a mixed workload of google and dns like jobs
	
	public int numOfDNSJobs;
	public int numOfGoogleJobs;
	/**
	 *proportion denotes the fraction of google like jobs
	 *should be configured in sim.config file 
	 */
	
	private double proportion;
	
	private double googleUbar;
	private double dnsUbar;
	
	private static double googleLambda = 4.2e-3;
	private static double dnsLamdba = 194e-3;
	
	private double internalServiceTime;
	
	private Random switchWorkloads;
	
	private ExponentialRandom googleInterArrivalGen;
	private ExponentialRandom googleServiceTimeGen;
	
	private ExponentialRandom dnsInterArrivalGen;
	private ExponentialRandom dnsServiceTimeGen;
	
	private int jobType;
	
	public MixedWorkloadGen(double rou, double mixture){
		this.proportion = mixture;
		
		googleLambda = rou / googleUbar;
		
		dnsUbar    = 194e-3;
		dnsLamdba = rou / dnsUbar;
		
		if(Sim.RANDOM_SEEDING){
			switchWorkloads = new Random(System.currentTimeMillis());
			
			googleInterArrivalGen = new ExponentialRandom(googleLambda, System.currentTimeMillis());
			googleServiceTimeGen = new ExponentialRandom(1.0/googleUbar, System.currentTimeMillis());
			
			dnsInterArrivalGen = new ExponentialRandom(dnsLamdba, System.currentTimeMillis());
			dnsServiceTimeGen = new ExponentialRandom(1.0/dnsUbar, System.currentTimeMillis());
		}
		
		else{
			switchWorkloads = new Random(100);
			
			googleInterArrivalGen = new ExponentialRandom(googleLambda, 10000);
			googleServiceTimeGen = new ExponentialRandom(1.0/googleUbar, 20000);
			
			dnsInterArrivalGen = new ExponentialRandom(dnsLamdba, 30000);
			dnsServiceTimeGen = new ExponentialRandom(1.0/dnsUbar, 40000);
		}
		
	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {
		// TODO Auto-generated method stub
		if(Double.compare(switchWorkloads.nextDouble(), proportion) < 0){
			numOfGoogleJobs ++;
			internalServiceTime = googleServiceTimeGen.getNextDouble();
			jobType = 0;
			return googleInterArrivalGen.getNextDouble();
		}
		
		else{
			numOfDNSJobs ++;
			internalServiceTime = dnsServiceTimeGen.getNextDouble();
			jobType = 1;
			return dnsInterArrivalGen.getNextDouble();
		}
	}
	
	

	@Override
	public double getNextServiceTime() {
		// TODO Auto-generated method stub
		return internalServiceTime;
	}
	
	public double getProportion(){
		return proportion;
	}

	@Override
	public int getJobType() {
		// TODO Auto-generated method stub
		return jobType;
	}

}
