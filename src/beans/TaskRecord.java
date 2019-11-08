package beans;

public class TaskRecord implements Writable {

	private long jobId;
	private double arrivalTime;
	private double startTime;
	private double computationFinishTime;
	private double taskFinishTime;
	private long taskId;

	public TaskRecord() {
		jobId = -1;
		taskId = -1;
		arrivalTime = 0.0;
		startTime = 0.0;
		computationFinishTime = 0.0;
		taskFinishTime = 0.0;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
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

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getComputationFinishTime() {
		return computationFinishTime;
	}

	public void setComputationFinishTime(double computationFinishTime) {
		this.computationFinishTime = computationFinishTime;
	}

	public double getTaskFinishTime() {
		return taskFinishTime;
	}

	public void setTaskFinishTime(double taskFinishTime) {
		this.taskFinishTime = taskFinishTime;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}

}