package beans;

/**
 * @author fanyao
 * task service time and latency record
 */
public class TaskSLRecord implements Comparable<TaskSLRecord>{

	public double taskSize;
	public double taskLatency;
	public int jobType;
	public long taskId;
	
	//sort by latency
	@Override
	public int compareTo(TaskSLRecord o) {
		// TODO Auto-generated method stub
		Double firstNLatency = taskLatency / taskSize;
		Double secondNLatency = o.taskLatency / o.taskSize;
		
		return ((Double) firstNLatency).compareTo(secondNLatency);
	}
	
	public TaskSLRecord(double taskSize, double taskLatency, int jobType, long taskId){
		this.taskSize = taskSize;
		this.taskLatency = taskLatency;
		this.jobType = jobType;
		this.taskId = taskId;
	}
	
	public TaskSLRecord(){
		
	}
	
	@Override
	public String toString(){
		return "(id:" + taskId + ", jobType:" + jobType + ", taskSize: " + taskSize + ", latency:" + Double.toString(taskLatency) + ")";
	}
	

}
