package controller;
import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.DataCenter;

import java.util.Random;

import debug.Sim;

import experiment.SingletonJobExperiment;

public class RandomSSEnforcer extends WindowedSSEnforcer {

	Random rand ;
	
	public RandomSSEnforcer(DataCenter dataCenter, SingletonJobExperiment experiment,
			double enforcePeriod) {
		super(dataCenter, experiment, enforcePeriod);
		rand = new Random(System.currentTimeMillis());
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int getNextSleepState() {
		// TODO Auto-generated method stub
		
	
		//return dataCenter.getExpConfig().getSleepState();
		//return fixedProbSleepState();
		//return biasedProbSleepState();
		return getSleepStateWithU(4.2e-3);
	}
	
	protected int fixedProbSleepState(){
		int numOfStates = AbstractSleepController.sleepStateWakeups.length;
		
		int selectedState = rand.nextInt(numOfStates);
		return selectedState + 1;
	}
	
	protected int biasedProbSleepState(){
		int numOfStates = AbstractSleepController.sleepStateWakeups.length;
		int selectedState = 0;
		double[] probOfStates = {0.1, 0.4, 0.3, 0.15, 0.05 };
	    double randNum = rand.nextDouble();
	    
	    double probSum = 0.0;
	    for(int i = 0; i < numOfStates; i++){
	    	probSum += probOfStates[i];
	    	if(Double.compare(randNum, probSum) < 0){
	    		selectedState = i;
	    		break;
	    	}
	    		
	    }

	    return selectedState + 1;
	}

	@Override
	protected double getNextFrequency() {
		// TODO Auto-generated method stub
		//set fixed speed
		//return dataCenter.getExpConfig().getSpeed();
		return getFrequencyWithQueuedJobs(windowedRecords.lastElement().getJobsInQueue());
		//return getUniformFrequency();
	}
	
	
	protected double getUniformFrequency(){
		double baseFreq = experiment.getDataCenter().getExpConfig().getRou();
		double slack = 1.0 - baseFreq;
		double randNum = rand.nextDouble();
		return baseFreq + slack * randNum;
	}

	
   private int getSleepStateWithU(double u){
	   if(u > AbstractSleepController.sleepStateWakeups[4])
		   return 5;
	   if(u > AbstractSleepController.sleepStateWakeups[3])
		   return 4;
	   else{
		   Random rand = null;
		   
		   if(Sim.RANDOM_SEEDING){
			   rand = new Random(System.currentTimeMillis());			   
		   }
		   else{
			   rand = new Random(1000);
		   }
		   return rand.nextInt()%3 + 1;
	   }
		   
   }
   
   private double getFrequencyWithQueuedJobs(int queuedJobs){
	   if(windowedRecords.size() < 2)
		   return 1.0;
	   if(queuedJobs > 125 && queuedJobs< 175){
		   return minDouble(windowedRecords.get(windowedRecords.size()-2).getSpeed() * 1.2, 1.0);
				   
	   }
	   
	   else if(queuedJobs > 200){
		   return minDouble(windowedRecords.get(windowedRecords.size()-2).getSpeed() * 1.5, 1.0);
	   }
	   
	   else if(queuedJobs < 50 && queuedJobs > 25){
		   return minDouble(windowedRecords.get(windowedRecords.size()-2).getSpeed() * 0.8, 1.0);
	   }
	   
	   else if(queuedJobs < 25){
		  return minDouble(windowedRecords.get(windowedRecords.size()-2).getSpeed() * 0.6, 1.0);
	   }
	   
	   else{
		   return 1.0;
	   }

   }
   
  private  double minDouble(double a, double b){
	   return Double.compare(a, b) > 0 ? b: a;
   }



}
