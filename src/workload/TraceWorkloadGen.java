package workload;

import java.util.Vector;
import java.io.*;
import java.util.*;

import experiment.ExperimentConfig;
import experiment.ExperimentConfig.ServiceTimeDistType;

import stochastic.ExponentialRandom;
import stochastic.RandomGenerator;
import stochastic.UniformRandom;

public class TraceWorkloadGen extends AbstractWorkloadGen {

	private LinkedList<Double> arrivals;
	private RandomGenerator serviceTimeGen;
	private int numberOfArrivals;
	private ExperimentConfig mExpConfig;
	
	public TraceWorkloadGen(String traceFileName, int numberOfServers, int numOfCores,
			double exptectedRho, boolean seedingMode, ExperimentConfig expConfig) {
		
		arrivals = new LinkedList<Double>();

		// read from the wiki trace file
		try {
			String line = null;
			BufferedReader br = new BufferedReader(new FileReader(traceFileName));
			while ((line = br.readLine()) != null) {
				double delta = Double.parseDouble(line);
				arrivals.add(delta);
			}
			
			br.close();
		} catch (IOException e) {
			System.out.println("trace find " + traceFileName + " not found");
			e.printStackTrace(); 
		}
		
		this.mExpConfig = expConfig;
		
		numberOfArrivals = arrivals.size();
		
		//FIXME: this is preset for an hour's trace
		//for a 1 hour trace
	    double u = ((numberOfArrivals/(3600))/(exptectedRho * numberOfServers * numOfCores));
	   // double u = ((numberOfArrivals/(3600 * 24))/(exptectedRho * numberOfServers));
	    
		if (seedingMode) {
			
			if(mExpConfig.getServiceTimeDist() == ServiceTimeDistType.EXP){
				serviceTimeGen = new ExponentialRandom(u, System.currentTimeMillis()*2);
			}
			else if( mExpConfig.getServiceTimeDist() ==  ServiceTimeDistType.UNIF){
				serviceTimeGen = new UniformRandom(u, System.currentTimeMillis()*2);
			}
		} else {
			//serviceTimeGen = new ExponentialRandom(u, 20000);
			if(mExpConfig.getServiceTimeDist() == ServiceTimeDistType.EXP){
				serviceTimeGen = new ExponentialRandom(u, 20000);
			}
			else if( mExpConfig.getServiceTimeDist() ==  ServiceTimeDistType.UNIF){
				serviceTimeGen = new UniformRandom(u, 20000);
			}
		}
	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {
		// TODO Auto-generated method stub
		return arrivals.poll();
	}

	@Override
	public double getNextServiceTime() {
		// TODO Auto-generated method stub
		return serviceTimeGen.getNextDouble();
	}

	@Override
	public int getJobType() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//number of jobs is determined by the trace length
	public int getTotalNumOfJobs(){
		return numberOfArrivals;
	}

}
