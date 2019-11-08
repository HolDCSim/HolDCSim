package event;

import communication.Flow;
import infrastructure.DCNode;
import infrastructure.Port;
import infrastructure.LineCard;
import infrastructure.Routable;
import experiment.Experiment;
import constants.Constants;

public final class FlowArriveEventBackup extends AbstractEvent {

	private DCNode mNode;
	private Flow flow;
	private int portId;
	private int linecardId;

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	public FlowArriveEventBackup(final double time, final Experiment experiment,
			Flow flow, final DCNode node, final int portId, final int linecardId) {
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
		this.flow = flow;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.FLOW_EVENT;
	}

	@Override
	public void printEventInfo() {
		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of "
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of linecard " + linecardId + " in " 
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId()  + 
//						". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			}
		}

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
				//System.out.println("port.getOutGressQueueSize()"+port.getOutGressQueueSize());
				//System.out.println("getLPIB()"+this.getExperiment().getExpConfig().getLPIB());
				if (port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){	//the linecard can be active or sleep
					if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
							if (linecard.getNextWakeUpevent() != null && connectedLc.getNextWakeUpevent() != null ) {
								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
								linecard.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
								connectedLc.setNextWakeUpevent(null);
							}
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(
										port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							
							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, 
									mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
							linecard.setNextWakeUpevent(lcwkEvent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							//process the connected line card
							if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
								if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, 
											port.getConnectedNode().getDataCenter().experiment, 
											port.getConnectedNode(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
									connectedLc.setNextWakeUpevent(lcwk2Event);;
									PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
											port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
											port.getConnectedPortID(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
									port.getConnectedPort().setNextWakeUpevent(wk2Event);
								} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
									// connected line card is active, just wake up the port without delay
									PortWakeupEvent wk2Event = new PortWakeupEvent( time, 
											port.getConnectedNode().getDataCenter().experiment, 
											port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
									port.getConnectedPort().setNextWakeUpevent(wk2Event);
								}
							}
						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							//double WakeupTao = 20E-6;
							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + constants.Constants.LCWakeupTao, 
									mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkevent);
							linecard.setNextWakeUpevent(lcwkevent);
							
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
									+constants.Constants.PortWakeupAfterLC, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+constants.Constants.LCWakeupTao, 
									port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
							connectedLc.setNextWakeUpevent(lcwk2event);
							
							PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
									+constants.Constants.PortWakeupAfterLC, port.getConnectedNode().getDataCenter().experiment, 
									port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							port.getConnectedPort().setNextWakeUpevent(wk2Event);
						}
					} else if (linecard.getLinecardState() == LineCard.LineCardState.ACTIVE) { 
						// line card is active, wake up the port only
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
							// port queue size is full
							
							// cancel all the wakeup events from previous node
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							// wake up the port without delay
							PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							//process the connected line card
							if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
								if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, 
											port.getConnectedNode(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
									connectedLc.setNextWakeUpevent(lcwk2Event);
									PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
											port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
									port.getConnectedPort().setNextWakeUpevent(wk2Event);
								} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
									// connected line card is active, just wake up the port without delay
									PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
											port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
									port.getConnectedPort().setNextWakeUpevent(wk2Event);
								}
							}
						} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							//double WakeupTao = 20E-6;
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
						
							PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
									port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
									port.getConnectedPortID(), connectedLcId);
							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
							port.getConnectedPort().setNextWakeUpevent(wk2Event);
						}
					}
				} else if (port.getPortState()==Port.PortPowerState.OFF) {
					
				}
			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
				int connectedLcId = (port.getConnectedPortID() - 1) / portsPerLineCard + 1;
				LineCard connectedLc = port.getConnectedNode().getLinecardById(connectedLcId);
				
				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
						// port queue size is full
						// cancel all the wakeup events from previous node
						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
							port.setNextWakeUpevent(null);
							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
							port.getConnectedPort().setNextWakeUpevent(null);
						}
						// wake up the port without delay
						PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
						
						//process the connected line card
						if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
							if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
								LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, 
										port.getConnectedNode(), connectedLcId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
								connectedLc.setNextWakeUpevent(lcwk2Event);
								PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
										port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
								port.getConnectedPort().setNextWakeUpevent(wk2Event);
							} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
								// connected line card is active, just wake up the port without delay
								PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
								port.getConnectedPort().setNextWakeUpevent(wk2Event);
							}
						}
					} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
						//add wakeup events after some time; if packets accumulate to B before this time, 
						//wake up at that time and cancel this wakeup event
						//double WakeupTao = 20E-6;
						PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
								mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
					
						PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
								port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
								port.getConnectedPortID(), connectedLcId);
						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
						port.getConnectedPort().setNextWakeUpevent(wk2Event);
					}
				}
			} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
				LineCard linecard = mNode.getLinecardById(linecardId);
				
				if (port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){	//the linecard can be active or sleep
					if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
							if (linecard.getNextWakeUpevent() != null) {
								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
								linecard.setNextWakeUpevent(null);
							}
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(
										port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							
							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, 
									mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
							linecard.setNextWakeUpevent(lcwkEvent);
							PortWakeupEvent wkEvent = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							//process the connected line card
							if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
								PortWakeupEvent wk2Event = new PortWakeupEvent( time, 
										port.getConnectedNode().getDataCenter().experiment, 
										port.getConnectedNode(), port.getConnectedPortID(), linecardId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
								port.getConnectedPort().setNextWakeUpevent(wk2Event);
							}
						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							//double WakeupTao = 20E-6;
							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + constants.Constants.LCWakeupTao, 
									mNode.getDataCenter().experiment, mNode, linecardId);
							mNode.getDataCenter().experiment.addEvent(lcwkevent);
							linecard.setNextWakeUpevent(lcwkevent);
							
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
									+constants.Constants.PortWakeupAfterLC, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
						}
					} else if (linecard.getLinecardState() == LineCard.LineCardState.ACTIVE) { 
						// line card is active, wake up the port only
						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
							// port queue size is full
							
							// cancel all the wakeup events from previous node
							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
								port.setNextWakeUpevent(null);
								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
								port.getConnectedPort().setNextWakeUpevent(null);
							}
							// wake up the port without delay
							PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
							
							//process the connected line card
							if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
								PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
										port.getConnectedNode(), port.getConnectedPortID(), linecardId);
								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
								port.getConnectedPort().setNextWakeUpevent(wk2Event);
							}
						} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
							//double WakeupTao = 20E-6;
							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
									mNode.getDataCenter().experiment, mNode, portId, linecardId);
							mNode.getDataCenter().experiment.addEvent(wkEvent);
							port.setNextWakeUpevent(wkEvent);
						
							PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
									port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
									port.getConnectedPortID(), linecardId);
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
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time , 
								mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
						
					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
						//double WakeupTao = 20E-6;
						
						PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
								mNode.getDataCenter().experiment, mNode, portId, linecardId);
						mNode.getDataCenter().experiment.addEvent(wkEvent);
						port.setNextWakeUpevent(wkEvent);
						
						PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
								port.getConnectedNode().getDataCenter().experiment, 
								port.getConnectedNode(), port.getConnectedPortID(), linecardId);
						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
						port.getConnectedPort().setNextWakeUpevent(wk2Event);
					}
				}
				
			}
			
			
			// the current flow arrives in the inport of destination server
			if (flow.getDestinationNode() == mNode
					|| flow.getSourceNode() == mNode) {
				if (flow.isZeroHop()) {
					double tTime = (flow.getFlowSize() * 8)
							/ (Port.availableRates.get(0) * Math.pow(10, 6));
					time = time + tTime + 0.005;

					DestinationReceivedFlowEvent drEvent = new DestinationReceivedFlowEvent(
							time + 0.002, mNode.getDataCenter().experiment,
							flow, mNode, flow.getDestinationPortId());
					mNode.getDataCenter().experiment.addEvent(drEvent);
					return;
				}
				
				if (port == null) {
					System.out.println("Flow arrives, but port in destination server could not be found.\n");
					System.out.println(mNode.getNodeId() + " " + portId);
				}
				// if the current hop is on edge, we directly operate on port as
				// we do not have Routable interfaces
				// to work on
				port.insertFlow(flow, time, false);
				return;
			}

			Routable routable = (Routable) mNode;
			// indicate whether this is a preceding packet or not
			routable.routeFlow(time, experiment, flow, portId);

		} catch (ClassCastException e) {
			System.err.println("Current node: " + mNode.getNodeId()
					+ " is not Routable");
			// TODO: maybe tolerate this error?
		}

	}
}











//package event;
//
//import communication.Flow;
//import infrastructure.DCNode;
//import infrastructure.Port;
//import infrastructure.LineCard;
//import infrastructure.Routable;
//import experiment.Experiment;
//import constants.Constants;
//
//public final class FlowArriveEvent extends AbstractEvent {
//
//	private DCNode mNode;
//	private Flow flow;
//	private int portId;
//	private int linecardId;
//
//	/**
//	 * The serialization id.
//	 */
//	private static final long serialVersionUID = 1L;
//
//	public FlowArriveEvent(final double time, final Experiment experiment,
//			Flow flow, final DCNode node, final int portId, final int linecardId) {
//		super(time, experiment);
//		this.mNode = node;
//		this.portId = portId;
//		this.linecardId = linecardId;
//		this.flow = flow;
//	}
//
//	@Override
//	public EVENT_TYPE getEventType() {
//		return EVENT_TYPE.FLOW_EVENT;
//	}
//
//	@Override
//	public void printEventInfo() {
//		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of "
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of linecard " + linecardId + " in " 
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId()  + 
//						". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			}
//		}
//
//	}
//
//	public void process() {
//
//		verbose();
//		try {
//			// FIXME: DCNode comparison
//			Port port = mNode.getPortById(portId);
//			if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="switch") {
//				LineCard linecard = mNode.getLinecardById(linecardId);
//				int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//				LineCard connectedLc = mNode.getLinecardById(connectedLcId);
//				//if the port is in the LPI mode, the port needs to be waked up 
//				//immediately or after some time based on the scheduling policy 
//				//(actually two ports of link needs to be waked up)
//				//if linecard is in the sleep mode, then check the port state
//				System.out.println("port.getOutGressQueueSize()"+port.getOutGressQueueSize());
//				System.out.println("getLPIB()"+this.getExperiment().getExpConfig().getLPIB());
//				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							//	port.setNextWakeUpevent(null);
//							//
//							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							//	port.getConnectedPort().setNextWakeUpevent(null);
//							//}
//							if (linecard.getNextWakeUpevent() != null && connectedLc.getNextWakeUpevent() != null) {
//								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//								linecard.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
//								connectedLc.setNextWakeUpevent(null);
//							}
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							
//							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time+20E-6 , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							
//							if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
//								if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//	
//				
//									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time+20E-6 , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									
//								}
//							}
//
//						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//							double WakeupTao = 20E-6;
//							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
//							
//							mNode.getDataCenter().experiment.addEvent(lcwkevent);
//							linecard.setNextWakeUpevent(lcwkevent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao+20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							port.setNextWakeUpevent(wkEvent);
//							
//							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//							connectedLc.setNextWakeUpevent(lcwk2event);
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao+20E-6, port.getConnectedNode().getDataCenter().experiment, 
//									port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
//				int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//				LineCard connectedLc = port.getConnectedNode().getLinecardById(connectedLcId);
//				
//				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//					//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//						if (connectedLc.getNextWakeUpevent() != null) {
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
//							connectedLc.setNextWakeUpevent(null);
//						}
//						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							port.setNextWakeUpevent(null);
//							
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							port.getConnectedPort().setNextWakeUpevent(null);
//						}
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						if (connectedLc.getLinecardState()==LineCard.LineCardState.SLEEP) {
//							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time+20E-6 , mNode.getDataCenter().experiment, 
//										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//						} else if (connectedLc.getLinecardState()==LineCard.LineCardState.ACTIVE) {
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time , mNode.getDataCenter().experiment, 
//										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//						}
//					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//						double WakeupTao = 20E-6;
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						port.setNextWakeUpevent(wkEvent);
//						
//						LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//						connectedLc.setNextWakeUpevent(lcwk2event);
//						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao+20E-6, port.getConnectedNode().getDataCenter().experiment, 
//								port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//						port.getConnectedPort().setNextWakeUpevent(wk2Event);
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
//				LineCard linecard = mNode.getLinecardById(linecardId);
//				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							//	port.setNextWakeUpevent(null);
//							//
//							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							//	port.getConnectedPort().setNextWakeUpevent(null);
//							//}
//							if (linecard.getNextWakeUpevent() != null) {
//								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//								linecard.setNextWakeUpevent(null);
//							}
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							
//							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time +20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//						
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//
//						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//							double WakeupTao = 20E-6;
//							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
//							
//							mNode.getDataCenter().experiment.addEvent(lcwkevent);
//							linecard.setNextWakeUpevent(lcwkevent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao+20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							port.setNextWakeUpevent(wkEvent);
//							
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
//									port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="server") {
//				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							port.setNextWakeUpevent(null);
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							port.getConnectedPort().setNextWakeUpevent(null);
//						}
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						
//					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//						double WakeupTao = 20E-6;
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						port.setNextWakeUpevent(wkEvent);
//						
//						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
//								port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//						port.getConnectedPort().setNextWakeUpevent(wk2Event);
//					}
//				}
//				
//			}
//			
//			
//			// the current package arrives in the inport of destination server
//			if (flow.getDestinationNode() == mNode
//					|| flow.getSourceNode() == mNode) {
//				if (flow.isZeroHop()) {
//					double tTime = (flow.getFlowSize() * 8)
//							/ (Port.availableRates.get(0) * Math.pow(10, 6));
//					time = time + tTime + 0.005;
//
//					DestinationReceivedFlowEvent drEvent = new DestinationReceivedFlowEvent(
//							time + 0.002, mNode.getDataCenter().experiment,
//							flow, mNode, flow.getDestinationPortId());
//					mNode.getDataCenter().experiment.addEvent(drEvent);
//					return;
//				}
//				
//				if (port == null) {
//					System.out.println("Flow arrives, but port in destination server could not be found.\n");
//					System.out.println(mNode.getNodeId() + " " + portId);
//				}
//				// if the current hop is on edge, we directly operate on port as
//				// we do not have Routable interfaces
//				// to work on
//				port.insertFlow(flow, time, false);
//				return;
//			}
//
//			Routable routable = (Routable) mNode;
//			// indicate whether this is a preceding packet or not
//			routable.routeFlow(time, experiment, flow, portId);
//
//		} catch (ClassCastException e) {
//			System.err.println("Current node: " + mNode.getNodeId()
//					+ " is not Routable");
//			// TODO: maybe tolerate this error?
//		}
//
//	}
	
	
	
//	package event;
//
//	import communication.Flow;
//	import infrastructure.DCNode;
//	import infrastructure.Port;
//	import infrastructure.LineCard;
//	import infrastructure.Routable;
//	import experiment.Experiment;
//	import constants.Constants;
//
//	public final class FlowArriveEvent extends AbstractEvent {
//
//		private DCNode mNode;
//		private Flow flow;
//		private int portId;
//		private int linecardId;
//
//		/**
//		 * The serialization id.
//		 */
//		private static final long serialVersionUID = 1L;
//
//		public FlowArriveEvent(final double time, final Experiment experiment,
//				Flow flow, final DCNode node, final int portId, final int linecardId) {
//			super(time, experiment);
//			this.mNode = node;
//			this.portId = portId;
//			this.linecardId = linecardId;
//			this.flow = flow;
//		}
//
//		@Override
//		public EVENT_TYPE getEventType() {
//			return EVENT_TYPE.FLOW_EVENT;
//		}
//
//		@Override
//		public void printEventInfo() {
//			if (mNode != null) {
//				if (mNode.getNodeTypeName() == "server") {
//					System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of "
//							+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//				} else if (mNode.getNodeTypeName() == "switch") {
//					System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of linecard " + linecardId + " in " 
//							+ mNode.getNodeTypeName() + " " + mNode.getNodeId()  + 
//							". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//				}
//			}
//
//		}
//
//		public void process() {
//
//			verbose();
//			try {
//				// FIXME: DCNode comparison
//				Port port = mNode.getPortById(portId);
//				if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="switch") {
//					LineCard linecard = mNode.getLinecardById(linecardId);
//					int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//					LineCard connectedLc = mNode.getLinecardById(connectedLcId);
//					//System.out.println("port.getOutGressQueueSize()"+port.getOutGressQueueSize());
//					//System.out.println("getLPIB()"+this.getExperiment().getExpConfig().getLPIB());
//					if (port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){	//the linecard can be active or sleep
//						if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//							if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//								if (linecard.getNextWakeUpevent() != null && connectedLc.getNextWakeUpevent() != null) {
//									mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//									linecard.setNextWakeUpevent(null);
//									port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
//									connectedLc.setNextWakeUpevent(null);
//								}
//								if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//									mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//									port.setNextWakeUpevent(null);
//									port.getConnectedNode().getDataCenter().experiment.cancelEvent(
//											port.getConnectedPort().getNextWakeUpevent());
//									port.getConnectedPort().setNextWakeUpevent(null);
//								}
//								
//								LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, 
//										mNode.getDataCenter().experiment, mNode, linecardId);
//								mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//								//linecard.setNextWakeUpevent(lcwkEvent);
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//								
//								//process the connected line card
//								if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
//									if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
//										LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, 
//												port.getConnectedNode().getDataCenter().experiment, 
//												port.getConnectedNode(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
//										connectedLc.setNextWakeUpevent(lcwk2Event);
//										PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
//												port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
//												port.getConnectedPortID(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//										port.getConnectedPort().setNextWakeUpevent(wk2Event);
//									} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
//										// connected line card is active, just wake up the port without delay
//										PortWakeupEvent wk2Event = new PortWakeupEvent( time, 
//												port.getConnectedNode().getDataCenter().experiment, 
//												port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//										port.getConnectedPort().setNextWakeUpevent(wk2Event);
//									}
//								}
//							}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//								//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//								//double WakeupTao = 20E-6;
//								LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + constants.Constants.LCWakeupTao, 
//										mNode.getDataCenter().experiment, mNode, linecardId);
//								mNode.getDataCenter().experiment.addEvent(lcwkevent);
//								//linecard.setNextWakeUpevent(lcwkevent);
//								
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
//										+constants.Constants.PortWakeupAfterLC, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//								
//								LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+constants.Constants.LCWakeupTao, 
//										port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//								connectedLc.setNextWakeUpevent(lcwk2event);
//								
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
//										+constants.Constants.PortWakeupAfterLC, port.getConnectedNode().getDataCenter().experiment, 
//										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//								port.getConnectedPort().setNextWakeUpevent(wk2Event);
//							}
//						} else if (linecard.getLinecardState() == LineCard.LineCardState.ACTIVE) { 
//							// line card is active, wake up the port only
//							if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
//								// port queue size is full
//								
//								// cancel all the wakeup events from previous node
//								if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//									mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//									port.setNextWakeUpevent(null);
//									port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//									port.getConnectedPort().setNextWakeUpevent(null);
//								}
//								// wake up the port without delay
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//								
//								//process the connected line card
//								if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
//									if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
//										LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, 
//												port.getConnectedNode(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
//										connectedLc.setNextWakeUpevent(lcwk2Event);
//										PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
//												port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//										port.getConnectedPort().setNextWakeUpevent(wk2Event);
//									} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
//										// connected line card is active, just wake up the port without delay
//										PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
//												port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//										port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//										port.getConnectedPort().setNextWakeUpevent(wk2Event);
//									}
//								}
//							} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//								//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//								//double WakeupTao = 20E-6;
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//							
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//										port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
//										port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//								port.getConnectedPort().setNextWakeUpevent(wk2Event);
//							}
//						}
//					}
//				} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
//					int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//					LineCard connectedLc = port.getConnectedNode().getLinecardById(connectedLcId);
//					
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
//							// port queue size is full
//							// cancel all the wakeup events from previous node
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							// wake up the port without delay
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							//port.setNextWakeUpevent(wkEvent);
//							
//							//process the connected line card
//							if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
//								if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
//									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, 
//											port.getConnectedNode(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
//									connectedLc.setNextWakeUpevent(lcwk2Event);
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
//											port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									port.getConnectedPort().setNextWakeUpevent(wk2Event);
//								} else if (connectedLc.getLinecardState() == LineCard.LineCardState.ACTIVE) {
//									// connected line card is active, just wake up the port without delay
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
//											port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									port.getConnectedPort().setNextWakeUpevent(wk2Event);
//								}
//							}
//						} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, 
//							//wake up at that time and cancel this wakeup event
//							//double WakeupTao = 20E-6;
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//									mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							//port.setNextWakeUpevent(wkEvent);
//						
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//									port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
//									port.getConnectedPortID(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//				} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
//					LineCard linecard = mNode.getLinecardById(linecardId);
//					
//					if (port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){	//the linecard can be active or sleep
//						if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//							if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//								if (linecard.getNextWakeUpevent() != null) {
//									mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//									linecard.setNextWakeUpevent(null);
//								}
//								if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//									mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//									port.setNextWakeUpevent(null);
//									port.getConnectedNode().getDataCenter().experiment.cancelEvent(
//											port.getConnectedPort().getNextWakeUpevent());
//									port.getConnectedPort().setNextWakeUpevent(null);
//								}
//								
//								LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, 
//										mNode.getDataCenter().experiment, mNode, linecardId);
//								mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//								//linecard.setNextWakeUpevent(lcwkEvent);
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time+Constants.PortWakeupAfterLC, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//								
//								//process the connected line card
//								if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time, 
//											port.getConnectedNode().getDataCenter().experiment, 
//											port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									port.getConnectedPort().setNextWakeUpevent(wk2Event);
//								}
//							}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//								//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//								//double WakeupTao = 20E-6;
//								LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + constants.Constants.LCWakeupTao, 
//										mNode.getDataCenter().experiment, mNode, linecardId);
//								mNode.getDataCenter().experiment.addEvent(lcwkevent);
//								//linecard.setNextWakeUpevent(lcwkevent);
//								
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.LCWakeupTao
//										+constants.Constants.PortWakeupAfterLC, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//							}
//						} else if (linecard.getLinecardState() == LineCard.LineCardState.ACTIVE) { 
//							// line card is active, wake up the port only
//							if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){	
//								// port queue size is full
//								
//								// cancel all the wakeup events from previous node
//								if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//									mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//									port.setNextWakeUpevent(null);
//									port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//									port.getConnectedPort().setNextWakeUpevent(null);
//								}
//								// wake up the port without delay
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//								
//								//process the connected line card
//								if (port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time, port.getConnectedNode().getDataCenter().experiment, 
//											port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									port.getConnectedPort().setNextWakeUpevent(wk2Event);
//								}
//							} else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//								//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//								//double WakeupTao = 20E-6;
//								PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//										mNode.getDataCenter().experiment, mNode, portId, linecardId);
//								mNode.getDataCenter().experiment.addEvent(wkEvent);
//								//port.setNextWakeUpevent(wkEvent);
//							
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//										port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), 
//										port.getConnectedPortID(), linecardId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//								port.getConnectedPort().setNextWakeUpevent(wk2Event);
//							}
//						}
//					}
//				} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="server") {
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time , 
//									mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							port.setNextWakeUpevent(wkEvent);
//							
//						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//							//double WakeupTao = 20E-6;
//							
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//									mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							//port.setNextWakeUpevent(wkEvent);
//							
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + constants.Constants.portWakeupTao, 
//									port.getConnectedNode().getDataCenter().experiment, 
//									port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//					
//				}
//				
//				
//				// the current flow arrives in the inport of destination server
//				if (flow.getDestinationNode() == mNode
//						|| flow.getSourceNode() == mNode) {
//					if (flow.isZeroHop()) {
//						double tTime = (flow.getFlowSize() * 8)
//								/ (Port.availableRates.get(0) * Math.pow(10, 6));
//						time = time + tTime + 0.005;
//
//						DestinationReceivedFlowEvent drEvent = new DestinationReceivedFlowEvent(
//								time + 0.002, mNode.getDataCenter().experiment,
//								flow, mNode, flow.getDestinationPortId());
//						mNode.getDataCenter().experiment.addEvent(drEvent);
//						return;
//					}
//					
//					if (port == null) {
//						System.out.println("Flow arrives, but port in destination server could not be found.\n");
//						System.out.println(mNode.getNodeId() + " " + portId);
//					}
//					// if the current hop is on edge, we directly operate on port as
//					// we do not have Routable interfaces
//					// to work on
//					port.insertFlow(flow, time, false);
//					return;
//				}
//
//				Routable routable = (Routable) mNode;
//				// indicate whether this is a preceding packet or not
//				routable.routeFlow(time, experiment, flow, portId);
//
//			} catch (ClassCastException e) {
//				System.err.println("Current node: " + mNode.getNodeId()
//						+ " is not Routable");
//				// TODO: maybe tolerate this error?
//			}
//
//		}
//	}
//
//
//
//
//
//package event;
//
//import communication.Flow;
//import infrastructure.DCNode;
//import infrastructure.Port;
//import infrastructure.LineCard;
//import infrastructure.Routable;
//import experiment.Experiment;
//import constants.Constants;
//
//public final class FlowArriveEvent extends AbstractEvent {
//
//	private DCNode mNode;
//	private Flow flow;
//	private int portId;
//	private int linecardId;
//
//	/**
//	 * The serialization id.
//	 */
//	private static final long serialVersionUID = 1L;
//
//	public FlowArriveEvent(final double time, final Experiment experiment,
//			Flow flow, final DCNode node, final int portId, final int linecardId) {
//		super(time, experiment);
//		this.mNode = node;
//		this.portId = portId;
//		this.linecardId = linecardId;
//		this.flow = flow;
//	}
//
//	@Override
//	public EVENT_TYPE getEventType() {
//		return EVENT_TYPE.FLOW_EVENT;
//	}
//
//	@Override
//	public void printEventInfo() {
//		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of "
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of linecard " + linecardId + " in " 
//						+ mNode.getNodeTypeName() + " " + mNode.getNodeId()  + 
//						". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
//			}
//		}
//
//	}
//
//	public void process() {
//
//		verbose();
//		try {
//			// FIXME: DCNode comparison
//			Port port = mNode.getPortById(portId);
//			if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="switch") {
//				LineCard linecard = mNode.getLinecardById(linecardId);
//				int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//				LineCard connectedLc = mNode.getLinecardById(connectedLcId);
//				//if the port is in the LPI mode, the port needs to be waked up 
//				//immediately or after some time based on the scheduling policy 
//				//(actually two ports of link needs to be waked up)
//				//if linecard is in the sleep mode, then check the port state
//				System.out.println("port.getOutGressQueueSize()"+port.getOutGressQueueSize());
//				System.out.println("getLPIB()"+this.getExperiment().getExpConfig().getLPIB());
//				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							//	port.setNextWakeUpevent(null);
//							//
//							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							//	port.getConnectedPort().setNextWakeUpevent(null);
//							//}
//							if (linecard.getNextWakeUpevent() != null && connectedLc.getNextWakeUpevent() != null) {
//								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//								linecard.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
//								connectedLc.setNextWakeUpevent(null);
//							}
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							
//							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time+20E-6 , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							
//							if(connectedLc.getLinecardState() == LineCard.LineCardState.SLEEP) {
//								if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//	
//				
//									LineCardWakeupEvent lcwk2Event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2Event);
//									PortWakeupEvent wk2Event = new PortWakeupEvent( time+20E-6 , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//									port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//									
//								}
//							}
//
//						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//							double WakeupTao = 20E-6;
//							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
//							
//							mNode.getDataCenter().experiment.addEvent(lcwkevent);
//							linecard.setNextWakeUpevent(lcwkevent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao+20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							port.setNextWakeUpevent(wkEvent);
//							
//							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//							connectedLc.setNextWakeUpevent(lcwk2event);
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao+20E-6, port.getConnectedNode().getDataCenter().experiment, 
//									port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
//				int connectedLcId = (port.getConnectedPortID()-1)/constants.Constants.NUM_OF_PORTS_PER_LINECARD + 1;
//				LineCard connectedLc = port.getConnectedNode().getLinecardById(connectedLcId);
//				
//				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//					//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//						if (connectedLc.getNextWakeUpevent() != null) {
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(connectedLc.getNextWakeUpevent());
//							connectedLc.setNextWakeUpevent(null);
//						}
//						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							port.setNextWakeUpevent(null);
//							
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							port.getConnectedPort().setNextWakeUpevent(null);
//						}
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						if (connectedLc.getLinecardState()==LineCard.LineCardState.SLEEP) {
//							LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time+20E-6 , mNode.getDataCenter().experiment, 
//										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//						} else if (connectedLc.getLinecardState()==LineCard.LineCardState.ACTIVE) {
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time , mNode.getDataCenter().experiment, 
//										port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//						}
//					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//						double WakeupTao = 20E-6;
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						port.setNextWakeUpevent(wkEvent);
//						
//						LineCardWakeupEvent lcwk2event = new LineCardWakeupEvent(time+WakeupTao, port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), connectedLcId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(lcwk2event);
//						connectedLc.setNextWakeUpevent(lcwk2event);
//						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao+20E-6, port.getConnectedNode().getDataCenter().experiment, 
//								port.getConnectedNode(), port.getConnectedPortID(), connectedLcId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//						port.getConnectedPort().setNextWakeUpevent(wk2Event);
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
//				LineCard linecard = mNode.getLinecardById(linecardId);
//				if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
//					if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//						if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						//if((port.getOutGressQueueSize()+port.getConnectedPort().getOutGressQueueSize())==this.getExperiment().getExpConfig().getLPIB()){
//							//if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							//	mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							//	port.setNextWakeUpevent(null);
//							//
//							//	port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							//	port.getConnectedPort().setNextWakeUpevent(null);
//							//}
//							if (linecard.getNextWakeUpevent() != null) {
//								mNode.getDataCenter().experiment.cancelEvent(linecard.getNextWakeUpevent());
//								linecard.setNextWakeUpevent(null);
//							}
//							if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//								mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//								port.setNextWakeUpevent(null);
//								port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//								port.getConnectedPort().setNextWakeUpevent(null);
//							}
//							
//							LineCardWakeupEvent lcwkEvent = new LineCardWakeupEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//							mNode.getDataCenter().experiment.addEvent(lcwkEvent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time +20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//						
//							if(port.getConnectedPort().getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//								PortWakeupEvent wk2Event = new PortWakeupEvent( time , port.getConnectedNode().getDataCenter().experiment, port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//								port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							}
//
//						}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//							//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//							double WakeupTao = 20E-6;
//							LineCardWakeupEvent lcwkevent = new LineCardWakeupEvent (time + WakeupTao, mNode.getDataCenter().experiment, mNode, linecardId);
//							
//							mNode.getDataCenter().experiment.addEvent(lcwkevent);
//							linecard.setNextWakeUpevent(lcwkevent);
//							PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao+20E-6, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//							mNode.getDataCenter().experiment.addEvent(wkEvent);
//							port.setNextWakeUpevent(wkEvent);
//							
//							PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
//									port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//							port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//							port.getConnectedPort().setNextWakeUpevent(wk2Event);
//						}
//					}
//				}
//			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="server") {
//				if(port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){
//					if(port.getOutGressQueueSize() ==this.getExperiment().getExpConfig().getLPIB()){
//						if(port.getNextWakeUpevent() != null && port.getConnectedPort().getNextWakeUpevent() != null ){
//							mNode.getDataCenter().experiment.cancelEvent(port.getNextWakeUpevent());
//							port.setNextWakeUpevent(null);
//							port.getConnectedNode().getDataCenter().experiment.cancelEvent(port.getConnectedPort().getNextWakeUpevent());
//							port.getConnectedPort().setNextWakeUpevent(null);
//						}
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time , mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						
//					}else if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
//						//add wakeup events after some time; if packets accumulate to B before this time, wake up at that time and cancel this wakeup event
//						double WakeupTao = 20E-6;
//						
//						PortWakeupEvent wkEvent = new PortWakeupEvent( time + WakeupTao, mNode.getDataCenter().experiment, mNode, portId, linecardId);
//						mNode.getDataCenter().experiment.addEvent(wkEvent);
//						port.setNextWakeUpevent(wkEvent);
//						
//						PortWakeupEvent wk2Event = new PortWakeupEvent( time + WakeupTao, port.getConnectedNode().getDataCenter().experiment, 
//								port.getConnectedNode(), port.getConnectedPortID(), linecardId);
//						port.getConnectedNode().getDataCenter().experiment.addEvent(wk2Event);
//						port.getConnectedPort().setNextWakeUpevent(wk2Event);
//					}
//				}
//				
//			}
//			
//			
//			// the current package arrives in the inport of destination server
//			if (flow.getDestinationNode() == mNode
//					|| flow.getSourceNode() == mNode) {
//				if (flow.isZeroHop()) {
//					double tTime = (flow.getFlowSize() * 8)
//							/ (Port.availableRates.get(0) * Math.pow(10, 6));
//					time = time + tTime + 0.005;
//
//					DestinationReceivedFlowEvent drEvent = new DestinationReceivedFlowEvent(
//							time + 0.002, mNode.getDataCenter().experiment,
//							flow, mNode, flow.getDestinationPortId());
//					mNode.getDataCenter().experiment.addEvent(drEvent);
//					return;
//				}
//				
//				if (port == null) {
//					System.out.println("Flow arrives, but port in destination server could not be found.\n");
//					System.out.println(mNode.getNodeId() + " " + portId);
//				}
//				// if the current hop is on edge, we directly operate on port as
//				// we do not have Routable interfaces
//				// to work on
//				port.insertFlow(flow, time, false);
//				return;
//			}
//
//			Routable routable = (Routable) mNode;
//			// indicate whether this is a preceding packet or not
//			routable.routeFlow(time, experiment, flow, portId);
//
//		} catch (ClassCastException e) {
//			System.err.println("Current node: " + mNode.getNodeId()
//					+ " is not Routable");
//			// TODO: maybe tolerate this error?
//		}
//
//	}
//}