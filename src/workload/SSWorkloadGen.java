package workload;

import experiment.ExperimentConfig;
import experiment.ExperimentConfig.ServiceTimeDistType;
import stochastic.*;

public class SSWorkloadGen extends AbstractWorkloadGen {

	private RandomGenerator interArrivalGen;
	private RandomGenerator serviceTimeGen;
	private ExperimentConfig mExpConfig;

	public SSWorkloadGen(double lambda, double u, boolean seedingMode, ExperimentConfig expConfig) {
		
		this.mExpConfig = expConfig;
		// use random seed for production, true means random seed
		if (seedingMode) {
			interArrivalGen = new ExponentialRandom(lambda, System.currentTimeMillis());
			if(mExpConfig.getServiceTimeDist() == ServiceTimeDistType.EXP){
				serviceTimeGen = new ExponentialRandom(u, System.currentTimeMillis()*2);
			}
			else if( mExpConfig.getServiceTimeDist() ==  ServiceTimeDistType.UNIF){
				serviceTimeGen = new UniformRandom(u, System.currentTimeMillis()*2);
			}
		} else {
			interArrivalGen = new ExponentialRandom(lambda, 10000);
			
			if(mExpConfig.getServiceTimeDist() == ServiceTimeDistType.EXP){
				serviceTimeGen = new ExponentialRandom(u, 20000);
			}
			else if( mExpConfig.getServiceTimeDist() ==  ServiceTimeDistType.UNIF){
				serviceTimeGen = new UniformRandom(u, 20000);
			}
		}
	}
	
	public SSWorkloadGen(double lambda, double u, long seed, ExperimentConfig expConfig) {
		
			this.mExpConfig = expConfig;
		// use random seed for production, true means random seed
			interArrivalGen = new ExponentialRandom(lambda, seed);
			if(mExpConfig.getServiceTimeDist() == ServiceTimeDistType.EXP){
				serviceTimeGen = new ExponentialRandom(u, seed);
			}
			else if( mExpConfig.getServiceTimeDist() ==  ServiceTimeDistType.UNIF){
				serviceTimeGen = new UniformRandom(u, seed);
			}
		
	}

	@Override
	public double getNextInterArrival(double currentArrivalTime) {
		// TODO Auto-generated method stub
		return interArrivalGen.getNextDouble();
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
	

}
