package workload;

import java.util.Random;

import debug.Sim;

import stochastic.ExponentialRandom;


/**
 * @author fanyao
 * keep a fixed arrival rate for the two mixed workload
 */
public class MixedWorkloadEZGen extends AbstractWorkloadGen {
	//consider a mixed workload of google and dns like jobs
	
		public int numOfDNSJobs;
		public int numOfGoogleJobs;
		/**
		 *proportion denotes the fraction of google like jobs
		 *should be configured in sim.config file 
		 */
		
		private double proportion;
		
		private static double googleUbar = 4.2e-3;
		private static double dnsUbar = 194e-3;
		private double averageUbar;
		
		private double mixedlambda;
		
		private double internalServiceTime;
		
		private Random switchWorkloads;
		
		private ExponentialRandom mixedInterArrivalGen;
		private ExponentialRandom googleServiceTimeGen;
		
		private ExponentialRandom dnsServiceTimeGen;
		
		private int jobType; //1 denotes google and 2 denotes dns
		
		
		public MixedWorkloadEZGen(double rou, double mixture, int numOfServers){
			proportion = mixture;

			//googleLambda = rou / googleUbar;
			averageUbar = proportion * googleUbar + (1-proportion) * dnsUbar;
			mixedlambda = rou / averageUbar * numOfServers;
			
			if(Sim.RANDOM_SEEDING){
				switchWorkloads = new Random(System.currentTimeMillis());
				
				mixedInterArrivalGen = new ExponentialRandom(mixedlambda, System.currentTimeMillis());
				googleServiceTimeGen = new ExponentialRandom(1.0/googleUbar, System.currentTimeMillis());
				
				//dnsInterArrivalGen = new ExponentialRandom(dnsLamdba, 30000);
				dnsServiceTimeGen = new ExponentialRandom(1.0/dnsUbar, System.currentTimeMillis());
			}
			else{
				switchWorkloads = new Random(100);
				
				mixedInterArrivalGen = new ExponentialRandom(mixedlambda, 10000);
				googleServiceTimeGen = new ExponentialRandom(1.0/googleUbar, 20000);
				
				//dnsInterArrivalGen = new ExponentialRandom(dnsLamdba, 30000);
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
				return mixedInterArrivalGen.getNextDouble();
			}
			
			else{
				numOfDNSJobs ++;
				internalServiceTime = dnsServiceTimeGen.getNextDouble();
				jobType = 1;
				return mixedInterArrivalGen.getNextDouble();
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

		//needs to be called after getServiceTime();
		@Override
		public int getJobType() {
			// TODO Auto-generated method stub
			return jobType;
		}


}
