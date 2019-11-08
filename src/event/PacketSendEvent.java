package event;

import communication.Packet;

import infrastructure.DCNode;
import infrastructure.Port.PortPowerState;
import experiment.Experiment;

/**
 * this event occurs when a packet is being sent out from a port. change the
 * port status (from busy to idle); also change the ack flag (from true to
 * false), the ack flag will change back to true when PacketTransmittedEvent is
 * added to the port
 * 
 * @author wjx
 * 
 */
public final class PacketSendEvent extends AbstractEvent {

	private Packet packet;
	private int portId;
	private int linecardId;
	private DCNode node;
	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	public PacketSendEvent(final double time, final Experiment experiment,
			Packet packet, final DCNode node, final int portId, final int linecardId) {
		super(time, experiment);
		this.portId = portId;
		this.linecardId = linecardId;
		this.packet = packet;
		this.node = node;
	}

	@Override
	public void printEventInfo() {
		if (node != null) {
			if (node.getNodeTypeName() == "server") {
				System.out.println("Time: " + this.getTime() + ", packet "
						+ packet.getPacketId() + " is sent from port " + portId + " of "
						+ node.getNodeTypeName() + " " + node.getNodeId() + " (sServer #"
						+ this.packet.getSourceNode().getNodeId()
						+ " --> dServer #"
						+ this.packet.getDestinationNode().getNodeId() + ")");
			} else if (node.getNodeTypeName() == "switch") {
				System.out.println("Time: " + this.getTime() + ", packet "
						+ packet.getPacketId() + " is sent from port " + portId + " of linecard " + linecardId + " in "
						+ node.getNodeTypeName() + " " + node.getNodeId() + " (sServer #"
						+ this.packet.getSourceNode().getNodeId()
						+ " --> dServer #"
						+ this.packet.getDestinationNode().getNodeId() + ")");
			}
		}

	}

	public void process() {

		verbose();
		// change the flag of the packet
		if (packet.getCurrentHop() == 0 || packet.isLastHopIngress()) {
			this.node.getPortById(portId).setOutAck(false);
			this.node.getPortById(portId).setOutPortState(PortPowerState.idle);
		}

		else {
			this.node.getPortById(portId).setInAck(false);
			this.node.getPortById(portId).setInPortState(PortPowerState.idle);
		}
		/**
		 * this.node.getPortById(portId).setAck(false);
		 * this.node.getPortById(portId).setPortState(PortState.idle);
		 **/

	}

	@Override
	public EVENT_TYPE getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE.PACKET_EVENT;
	}
}
