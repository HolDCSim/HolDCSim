package event;

import communication.Flow;

import infrastructure.DCNode;
import infrastructure.Port;
import experiment.Experiment;

public final class FlowTransmittedEvent extends AbstractEvent {
	private DCNode node;
	private Flow flow;
	private int portId;
	private int linecardId;
	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param time
	 * @param experiment
	 * @param packet
	 * @param _switch
	 * @param _server
	 * @param port
	 */
	public FlowTransmittedEvent(final double time,
			final Experiment experiment, Flow packet, final DCNode dcNode,
			final int portId, final int linecardId) {
		super(time, experiment);
		this.node = dcNode;
		this.portId = portId;
		this.flow = packet;
		this.linecardId = linecardId;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.FLOW_EVENT;
	}

	@Override
	public void printEventInfo() {
		if (node != null) {
//			if (node.getNodeTypeName() == "server") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" +
//						+ this.flow.getSourceNode().getNodeId() + " --> server#" + this.flow.getDestinationNode().getNodeId()
//						+ "] transmitted event for port " + portId + " of " + node.getNodeTypeName() + " " + node.getNodeId()
//						+ ". Job "+ flow.getJob().getJobId()+ ", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			} else if (node.getNodeTypeName() == "switch") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" +
//						+ this.flow.getSourceNode().getNodeId() + " --> server#" + this.flow.getDestinationNode().getNodeId()
//						+ "] transmitted event for port " + portId + " of linecard " + linecardId + " in "
//						+ node.getNodeTypeName() + " " + node.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() 
//						+" to " + flow.getDestinationTask().getTaskId() + ".");
//			}
			

		}
	}

	public void process() {
		verbose();
		Port port = node.getPortById(portId);
		if (port == null)
			System.err
					.println("could not remove transmitted flow from last port, port is null\n");
		port.removeFlow(flow, time, true);

	}
}
