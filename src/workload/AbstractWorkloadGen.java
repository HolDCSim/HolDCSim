package workload;

public abstract class AbstractWorkloadGen {

	public abstract double getNextInterArrival(double currentArrivalTime);

	public abstract double getNextServiceTime();
	
	public abstract int getJobType();

}
