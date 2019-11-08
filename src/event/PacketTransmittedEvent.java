package event;

import communication.Packet;

import infrastructure.DCNode;
import infrastructure.Port;
import experiment.Experiment;

public final class PacketTransmittedEvent extends AbstractEvent {
	private DCNode node;
	private Packet packet;
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
	public PacketTransmittedEvent(final double time,
			final Experiment experiment, Packet packet, final DCNode dcNode,
			final int portId, final int linecardId) {
		super(time, experiment);
		this.node = dcNode;
		this.portId = portId;
		this.packet = packet;
		this.linecardId = linecardId;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PACKET_EVENT;
	}

	@Override
	public void printEventInfo() {
		if (node != null) {
			if (node.getNodeTypeName() == "server") {
				System.out.println("Time: " + this.getTime() + ", packet"
						+ packet.getPacketId() + " transmitted event for port " + portId + " of "
						+ node.getNodeTypeName() + " " + node.getNodeId() + " (sServer #"
						+ this.packet.getSourceNode().getNodeId() + " --> dServer #"
						+ this.packet.getDestinationNode().getNodeId() + ")");
			} else if (node.getNodeTypeName() == "switch") {
				System.out.println("Time: " + this.getTime() + ", packet"
						+ packet.getPacketId() + " transmitted event for port " + portId + " of linecard " + linecardId + " in "
						+ node.getNodeTypeName() + " " + node.getNodeId() + " (sServer #"
						+ this.packet.getSourceNode().getNodeId() + " --> dServer #"
						+ this.packet.getDestinationNode().getNodeId() + ")");
			}
			

		}
	}

	public void process() {

		verbose();
		Port port = node.getPortById(portId);
		if (port == null)
			System.err
					.println("could not remove transmitted packet from last port, port is null\n");
		port.removePacket(packet, time, true);

	}
}
