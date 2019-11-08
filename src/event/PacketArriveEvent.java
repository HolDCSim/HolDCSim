package event;

import communication.Packet;
import infrastructure.DCNode;
import infrastructure.Port;
import infrastructure.LineCard;
import infrastructure.Routable;
import experiment.Experiment;
import constants.Constants;

public final class PacketArriveEvent extends AbstractEvent {

	private DCNode mNode;
	private Packet packet;
	private int portId;
	private int linecardId;

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	public PacketArriveEvent(final double time, final Experiment experiment,
			Packet packet, final DCNode node, final int portId, final int linecardId) {
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
		this.packet = packet;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PACKET_EVENT;
	}

	@Override
	public void printEventInfo() {
//	/*	if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				System.out.println("Time: " + this.getTime() + ", packet "
//						+ packet.getPacketId() + " arrives at port " + portId + " of "
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + " (sServer #"
//						+ this.packet.getSourceNode().getNodeId() + " --> dServer #"
//						+ this.packet.getDestinationNode().getNodeId() +
//						", Job "+ packet.get_jobid()+", task "+ packet.get_sourceTask().getTaskId() +" to " + packet.get_destinationTask().getTaskId() + ")");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				System.out.println("Time: " + this.getTime() + ", packet "
//						+ packet.getPacketId() + " arrives at port " + portId + " of linecard " + linecardId + " in "
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + " (sServer #"
//						+ this.packet.getSourceNode().getNodeId() + " --> dServer #"
//						+ this.packet.getDestinationNode().getNodeId() +
//						", Job "+ packet.get_jobid()+", task "+ packet.get_sourceTask().getTaskId() +" to " + packet.get_destinationTask().getTaskId() + ")");
//			}
//		}
//*/
	}

	public void process() {

		verbose();
		try {
			// FIXME: DCNode comparison
			Port port = mNode.getPortById(portId);
			int portsPerLineCard = experiment.getExpConfig().getPortsPerLineCard();
			if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="switch") {
				LineCard linecard = mNode.getLinecardById(linecardId);
				int connectedLcId = (port.getConnectedPortID() - 1) / portsPerLineCard + 1;
				LineCard connectedLc = mNode.getLinecardById(connectedLcId);
				//if the port is in the LPI mode, the port needs to be waked up 
				//immediately or after some time based on the scheduling policy 
				//(actually two ports of link needs to be waked up)
				//if linecard is in the sleep mode, then check the port state
				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
							//	port.setNextWakeUpevent(null);
							//
							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
							//	port.getConnectedPort().setNextWakeUpevent(null);
							//}
							if (linecard.getNextWakeUpevent() != null && connectedLc.getNextWakeUpevent() != null) {
								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
								linecard.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
								connectedLc.setNextWakeUpevent(null);
							}
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							
							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							
							if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
								if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
	
				
									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
									PortWakeupEvent wk2Event = new PortWakeupEvent( time , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
									
								}
							}

						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							double WakeupTao = 20E-6;
							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
							
							mNode.getDataCenter().experiment.addEvent(lcwkevent);
							linecard.setNextWakeUpevent(lcwkevent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
							connectedLc.setNextWakeUpevent(lcwk2event);
							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
									port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							port.getConnectedPort().setNextWakeUpevent(wk2Event);
						}
					}
				}
			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
				int connectedLcId = (port.getConnectedPortID() - 1) / portsPerLineCard + 1;
				LineCard connectedLc = port.getConnectedNode().getLinecardById(connectedLcId);
				
				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
					//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
						if (connectedLc.getNextWakeUpevent() != null) {
							port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
							connectedLc.setNextWakeUpevent(null);
						}
						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
							port.setNextWakeUpevent(null);
							
							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
							port.getConnectedPort().setNextWakeUpevent(null);
						}
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						if (connectedLc.getLinecardState()==LineCard.LineCardState.SLEEP) {
							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
								PortWakeupEvent wk2Event = new PortWakeupEvent( time , mNode.getDataCenter().experiment, 
										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							}
						} else if (connectedLc.getLinecardState()==LineCard.LineCardState.ACTIVE) {
							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
								PortWakeupEvent wk2Event = new PortWakeupEvent( time , mNode.getDataCenter().experiment, 
										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							}
						}
					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
						double WakeupTao = 20E-6;
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
						
						LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
						port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
						connectedLc.setNextWakeUpevent(lcwk2event);
						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
								port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
						port.getConnectedPort().setNextWakeUpevent(wk2Event);
					}
				}
			} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
				LineCard linecard = mNode.getLinecardById(linecardId);
				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
							//	port.setNextWakeUpevent(null);
							//
							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
							//	port.getConnectedPort().setNextWakeUpevent(null);
							//}
							if (linecard.getNextWakeUpevent() != null) {
								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
								linecard.setNextWakeUpevent(null);
							}
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							
							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
						
							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
								PortWakeupEvent wk2Event = new PortWakeupEvent( time , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), linecardId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							}

						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							double WakeupTao = 20E-6;
							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
							
							mNode.getDataCenter().experiment.addEvent(lcwkevent);
							linecard.setNextWakeUpevent(lcwkevent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
									port.getConnectedNode(), port.getConnectedPortID(), linecardId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							port.getConnectedPort().setNextWakeUpevent(wk2Event);
						}
					}
				}
			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="server") {
				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
							port.setNextWakeUpevent(null);
							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
							port.getConnectedPort().setNextWakeUpevent(null);
						}
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						
					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
						double WakeupTao = 20E-6;
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
						
						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
								port.getConnectedNode(), port.getConnectedPortID(), linecardId);
						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
						port.getConnectedPort().setNextWakeUpevent(wk2Event);
					}
				}
				
			}
			
			
			// the current package arrives in the inport of destination server
			if (packet.getDestinationNode() == mNode
					|| packet.getSourceNode() == mNode) {
				if (packet.isZeroHop()) {
					double tTime = (Packet.packet_size * 8)
							/ (Port.availableRates.get(0) * Math.pow(10, 6));
					time = time + tTime + 0.005;

					DestinationReceivedEvent drEvent = new DestinationReceivedEvent(
							time + 0.002, mNode.getDataCenter().experiment,
							packet, mNode, packet.getDestinationPortId());
					mNode.getDataCenter().experiment.addEvent(drEvent);
					return;
				}
				
				if (port == null) {
					System.out.println("packet arrive, but port in destination server could not be found\n");
					System.out.println(mNode.getNodeId() + " " + portId);
				}
				// if the current hop is on edge, we directly operate on port as
				// we do not have Routable interfaces
				// to work on
				port.insertPacket(packet, time, false);
				return;
			}

			Routable routable = (Routable) mNode;
			// indicate whether this is a preceding packet or not
			routable.routePacket(time, experiment, packet, portId);

		} catch (ClassCastException e) {
			System.err.println("Current node: " + mNode.getNodeId()
					+ " is not Routable");
			// TODO: maybe tolerate this error?
		}

	}
}
