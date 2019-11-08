package event;

import communication.Flow;
import infrastructure.DCNode;
import infrastructure.Port.PortPowerState;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;

public class DestinationReceivedFlowEvent extends AbstractEvent {
	/**
	 * The serialization id.
	 */

	private DCNode node;
	private Flow flow;
	private int portId;
	private static final long serialVersionUID = 1L;

	public DestinationReceivedFlowEvent(final double time,
			final Experiment experiment, Flow flow, final DCNode node,
			final int portId) {
		super(time, experiment);
		this.node = node;
		this.portId = portId;
		this.flow = flow;
		
		this.flow.setNextFlowArrival(null);
		this.flow.setLastFlowArrival(this);
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.FLOW_EVENT;
	}

	@Override
	public void printEventInfo() {
//		System.out.println("Time: " + this.getTime()
//				+ ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()
//				+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] is received at destination server. Job "
//				+ this.flow.getJob().getJobId() + ", task " + this.flow.getSourceTask().getTaskId() + " to "
//				+ this.flow.getDestinationTask().getTaskId() + ".");
		//System.out.println(flow.getStartTime());
		//System.out.println("The Flow Completion time is " + (this.time-flow.getStartTime()));
		//System.out.println((this.time-flow.getStartTime())*1000);
		this.flow.getJob().setFlowCompletionTime(this.time-flow.getStartTime());
	}

	public void process() {
		verbose();
		if (node == null)
			System.err.println("destination node null\n");

		else {
			if (!flow.isZeroHop()) {
				/** node.getPortById(portId).setPortState(PortState.idle); **/
				//need to be changed according to the scheduler
				
				node.getPortById(portId).setInPortState(PortPowerState.idle);

				/*
				 * same to PacketTransmitted events,keep it need to create a
				 * PacketTransmitted events, otherswise the packet that uses
				 * this current server as a internal hop would get stuck
				 */
				node.getPortById(portId).removeFlow(flow, time, false);
			}
			
			// Remove task dependency
			this.flow.getJob().removeDependencyByFlow(time, flow);
			
			// Remove flow from links
			LinecardNetworkExperiment netExp = (LinecardNetworkExperiment)this.getExperiment();
			netExp.getFlowController().flowCompleted(flow, time);
		}
	}
	
	public DCNode getNode() {
		return node;
	}
	
	public int getPortId() {
		return portId;
	}
}
