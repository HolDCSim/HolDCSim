package event;

import communication.Flow;
import infrastructure.DCNode;
import infrastructure.LCSwitch;
import infrastructure.Port;
import infrastructure.LineCard;
import infrastructure.Routable;
import infrastructure.SwitchSleepController;
import scheduler.LinecardNetworkScheduler;
import experiment.Experiment;
import constants.Constants;

public final class FlowArriveEvent extends AbstractEvent {

	private DCNode mNode;
	private Flow flow;
	private int portId;
	private int linecardId;

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	public FlowArriveEvent(final double time, final Experiment experiment,
			Flow flow, final DCNode node, final int portId, final int linecardId) {
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
		this.flow = flow;
		
		this.flow.setNextFlowArrival(this);
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.FLOW_EVENT;
	}

	@Override
	public void printEventInfo() {
		if (mNode != null) {
			if (mNode.getNodeTypeName() == "server") {
				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of "
						+ mNode.getNodeTypeName() + " " + mNode.getNodeId() + ". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
			} else if (mNode.getNodeTypeName() == "switch") {
				System.out.println("Time: " + this.getTime() + ", [FLOW_EVENT], flow for [server#" + this.flow.getSourceNode().getNodeId()+ " --> server#" + this.flow.getDestinationNode().getNodeId() + "] arrives at port " + portId + " of linecard " + linecardId + " in "
						+ mNode.getNodeTypeName() + " " + mNode.getNodeId()  +
						". Job "+ flow.getJob().getJobId()+", task "+ flow.getSourceTask().getTaskId() +" to " + flow.getDestinationTask().getTaskId() + ".");
			}
		}

	}

	public void process() {

		verbose();
		try {
			LinecardNetworkScheduler switchScheduler = (LinecardNetworkScheduler)experiment.getTaskScheduler();
			Port port = mNode.getPortById(portId);
			
			if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="switch") {
				// Decrement flow queue size only if this is the first time the flow has arrived to the linecard
				if(!flow.getWaitingForLineCardWakeup()) {
					((LCSwitch)mNode).flowQueueSize--;
					((LCSwitch)(port.getConnectedNode())).flowQueueSize++;
				}
				
				LineCard linecard = switchScheduler.getLineCard((LCSwitch)mNode, portId);
				int linecardId = ((LCSwitch)mNode).getIdByLinecard(linecard);
				
				if(wakeLineCard(switchScheduler, mNode, port, linecard, linecardId)) {
					return;
				}
			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="switch") {
				// Increment flow queue size only if this is the first time the flow has arrived to the linecard
				if(!flow.getWaitingForLineCardWakeup()) {
					((LCSwitch)(port.getConnectedNode())).flowQueueSize++;	
				}
				
				LineCard connectedLc = switchScheduler.getLineCard((LCSwitch)port.getConnectedNode(), portId);
				int connectedLcId = ((LCSwitch)(port.getConnectedNode())).getIdByLinecard(connectedLc);
				
				if(wakeLineCard(switchScheduler, port.getConnectedNode(), port, connectedLc, connectedLcId)) {
					return;
				}
			} else if (mNode.getNodeTypeName() == "switch" && port.getConnectedNode().getNodeTypeName()=="server") {
				// Decrement flow queue size only if this is the first time the flow has arrived to the linecard
				if(!flow.getWaitingForLineCardWakeup()) {
					((LCSwitch)mNode).flowQueueSize--;	
				}
				
				LineCard linecard = switchScheduler.getLineCard((LCSwitch)mNode, portId);
				int linecardId = ((LCSwitch)mNode).getIdByLinecard(linecard);
				
				if(wakeLineCard(switchScheduler, mNode, port, linecard, linecardId)) {
					return;
				}
			} else if (mNode.getNodeTypeName() == "server" && port.getConnectedNode().getNodeTypeName()=="server") {
				handlePortStates(port);
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
			
			// Update switch thresholds

		} catch (ClassCastException e) {
			System.err.println("Current node: " + mNode.getNodeId()
					+ " is not Routable");
			// TODO: maybe tolerate this error?
		}

	}
	
	public void handlePortStates(Port port) {
		if (port.getPortState()==Port.PortPowerState.LOW_POWER_IDLE){	//the linecard can be active or sleep 
			// line card is active, wake up the port only
			if(port.getNextWakeUpevent() == null && port.getConnectedPort().getNextWakeUpevent() == null){
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
	
	public void resendFlow() {
		FlowArriveEvent flowArriveEvent = new FlowArriveEvent(time + constants.Constants.LCWakeupTao, this.getExperiment(),
				flow, mNode, portId, linecardId);
				this.getExperiment().addEvent(flowArriveEvent);
				flow.setWaitingForLineCardWakeup(true);
	}
	
	public boolean wakeLineCard(LinecardNetworkScheduler switchScheduler, DCNode lineCardNode, Port port, LineCard linecard, int linecardId) {
		// Current node is switch
		if(mNode instanceof LCSwitch) {
			SwitchSleepController switchSleepController = linecard.getSleepController();

			// Linecard is asleep
			if(switchSleepController.getCurrentSleepState() != 0) {
				LineCardWakeupEvent wakeupLC;
				
				if(((LCSwitch)lineCardNode).flowQueueSize >= this.getExperiment().getExpConfig().getLPIB()) {
					// If passed threshold, wake up right away
					if(linecard.getNextWakeUpevent() != null) {
						this.getExperiment().cancelEvent(linecard.getNextWakeUpevent());
					}
					
					wakeupLC = new LineCardWakeupEvent(time, this.getExperiment(),
							lineCardNode, linecardId);
					this.getExperiment().addEvent(wakeupLC);
					linecard.setNextWakeUpevent(wakeupLC);
				}
				else {
					// Else, wake up after some time
					if(linecard.getNextWakeUpevent() == null) {
						wakeupLC = new LineCardWakeupEvent(time + constants.Constants.LCWakeupTao, this.getExperiment(),
								lineCardNode, linecardId);
						this.getExperiment().addEvent(wakeupLC);
						linecard.setNextWakeUpevent(wakeupLC);
					}
				}
				
				// Flow can't progress until linecard wakes up. Resend flow
				resendFlow();
				return true;
			}	
			
			// Linecard is awake
			flow.setWaitingForLineCardWakeup(false);
			
			handlePortStates(port);
			
			return false;
		}
		// Current node is server
		else {
			return false;
		}
	}
	
	public DCNode getMNode() {
		return this.mNode;
	}
	
	public int getPortId() {
		return this.portId;
	}
	
	public int getLinecardId() {
		return this.linecardId;
	}
}