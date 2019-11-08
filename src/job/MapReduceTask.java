package job;

public class MapReduceTask extends Task {

	public static enum TASK_TYPE
	{
		MAP, 
		REDUCE
	};
	
	private TASK_TYPE mTaskType;
	public MapReduceTask(double theTaskSize, Job job, TASK_TYPE taskType) {
		super(theTaskSize, job);
		mTaskType = taskType;
		// TODO Auto-generated constructor stub
	}
	
	public TASK_TYPE getTaskType()
	{
		return mTaskType;
	}

}
