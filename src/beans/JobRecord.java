package beans;

public class JobRecord  implements Writable{

	private long jobId;
	private double arrivalTime;
	private double startTime;
	private double finishTime;

	public JobRecord(long jobId, double arrivalTime, double startTime,
			double finishTime) {
		this.jobId = jobId;
		this.arrivalTime = arrivalTime;
		this.startTime = startTime;
		this.finishTime = finishTime;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivaTime) {
		this.arrivalTime = arrivaTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(double finishTime) {
		this.finishTime = finishTime;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}

}