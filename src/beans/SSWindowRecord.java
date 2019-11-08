package beans;

public class SSWindowRecord {

	public double windowSize;

	private double speed;
	private int sleepState;

	private double activeTimePeriod = 0.0;
	private double idleTimePeriod = 0.0;
	private double windowStateTime = 0.0;
	private int jobsInQueue = 0;
	private int numofCompletedJobs = 0;

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public int getSleepState() {
		return sleepState;
	}

	public void setSleepState(int sleepState) {
		this.sleepState = sleepState;
	}

	public double getActiveTimePeriod() {
		return activeTimePeriod;
	}

	public void setActiveTimePeriod(double activeTimePeriod) {
		this.activeTimePeriod = activeTimePeriod;
	}

	public double getIdleTimePeriod() {
		return idleTimePeriod;
	}

	public void setIdleTimePeriod(double idleTimePeriod) {
		this.idleTimePeriod = idleTimePeriod;
	}

	public double getWindowStateTime() {
		return windowStateTime;
	}

	public void setWindowStateTime(double windowStateTime) {
		this.windowStateTime = windowStateTime;
	}

	public double getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(double windowSize) {
		this.windowSize = windowSize;
	}

	public void incrementIdleTime(double duration) {
		this.idleTimePeriod += duration;
	}

	public void setJobsInQueue(int jobNum) {
		this.jobsInQueue = jobNum;
	}

	public int getJobsInQueue() {
		return this.jobsInQueue;
	}

	public int getCompletedJobs() {
		return this.numofCompletedJobs;
	}

	public void incrementCompletedJobs() {
		this.numofCompletedJobs += 1;
	}

}