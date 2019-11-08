package event;

import communication.Packet;
import infrastructure.DCNode;
import infrastructure.Port.PortPowerState;
import experiment.Experiment;

public class DestinationReceivedEvent extends AbstractEvent {
	/**
	 * The serialization id.
	 */

	private DCNode node;
	private Packet packet;
	private int portId;
	private static final long serialVersionUID = 1L;

	public DestinationReceivedEvent(final double time,
			final Experiment experiment, Packet packet, final DCNode node,
			final int portId) {
		super(time, experiment);
		this.node = node;
		this.portId = portId;
		this.packet = packet;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PACKET_EVENT;
	}

	@Override
	public void printEventInfo() {
		System.out.println("Time :" + this.getTime()
				+ " Destination received event " + "Server " + node.getNodeId()
				+ " (sServer #" + this.packet.getSourceNode().getNodeId()
				+ "--> dServer #"
				+ this.packet.getDestinationNode().getNodeId() + ")" + "Job "
				+ this.packet.get_jobid() + " succeeding task "
				+ this.packet.get_destinationTask().getTaskId());
	}

	public void process() {
		verbose();
		if (node == null)
			System.err.println("destination node null\n");

		else {
			if (!packet.isZeroHop()) {
				/** node.getPortById(portId).setPortState(PortState.idle); **/
				//need to be changed according to the scheduler
				
				node.getPortById(portId).setInPortState(PortPowerState.idle);

				/*
				 * same to PacketTransmitted events,keep it need to create a
				 * PacketTransmitted events, otherswise the packet that uses
				 * this current server as a internal hop would get stuck
				 */
				node.getPortById(portId).removePacket(packet, time, false);
			}
			// experiment.getJobs().get((int) this.packet.get_jobid() -
			// 1).removeDependency(time, packet);
			this.packet.getJob().removeDependency(time, packet);
		}
	}

}
