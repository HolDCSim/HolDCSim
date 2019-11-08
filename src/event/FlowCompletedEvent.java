package event;

import communication.Flow;
import event.Event.EVENT_TYPE;
import experiment.*;

public class FlowCompletedEvent extends AbstractEvent {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Experiment mExp;
	Flow aFlow;

	public FlowCompletedEvent(double theTime, Experiment anExperiment, Flow flow) {
		super(theTime, anExperiment);
		// TODO Auto-generated constructor stub
		this.aFlow = flow;
		this.mExp = anExperiment;
	}
	
	@Override
	public void printEventInfo() {
		System.out.println("Time: " + this.getTime()
				+ ", [FLOW_EVENT], flow for [server#" + aFlow.getSourceNode().getNodeId() + " --> server#" 
				+ aFlow.getDestinationNode().getNodeId() + "] completed event. Job "+aFlow.getJob().getJobId()+ ", task "
				+ aFlow.getSourceTask().getTaskId() + " to " + aFlow.getDestinationTask().getTaskId() + ".");
	}

	@Override
	public void process() {
		verbose();
		LinecardNetworkExperiment netExp = (LinecardNetworkExperiment)mExp;
		netExp.getFlowController().flowCompleted(aFlow, time);
		System.out.println(aFlow);
		System.out.println(aFlow.getJob());
		aFlow.getJob().removeDependencyByFlow(time, aFlow);
	}
	
	@Override
	public EVENT_TYPE getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE.FLOW_EVENT;
	}

}
