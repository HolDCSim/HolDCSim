package infrastructure;

import java.util.*;

import communication.Flow;
import communication.Packet;
import constants.Constants;
import constants.Constants.*;
import debug.Sim;
import event.DestinationReceivedEvent;
import event.DestinationReceivedFlowEvent;
import event.FlowArriveEvent;
import event.FlowSendEvent;
import event.FlowTransmittedEvent;
import event.PacketArriveEvent;
import event.PacketSendEvent;
import event.PacketTransmittedEvent;
//import event.PortEnterLPIEvent;
import event.PortTransitiontoLPIEvent;
import event.PortWakeupEvent;
import experiment.Experiment;
import scheduler.LinecardNetworkScheduler;


/**
 * @author fan
 * 
 */

/**
 * Available port power states
 * @author jingxin
 *
 */
public class Port {

	public static enum PortPowerState {
		/**
		 * Port is in the LPI mode
		 */
		LOW_POWER_IDLE,
		/**
		 * Port is in the process of transitioning to LPI
		 */
		TRANSITION_TO_LOW_POWER_IDLE,
		/**
		 * Port is in the process of transisioning to active
		 */
		TRANSITIONG_TO_ACTIVE,
		/*
		 * When port is active, it can be transmitting data (busy) or idle
		 */
		idle,
		busy,
		OFF
	}

	/**
	 * Available power management policies for the port.
	 */
	public static enum PortPowerPolicy {
		/**
		 * No power management policy is being used. Port will always be idle
		 */
		NO_MANAGEMENT,

		/**
		 * Port goes into LPI after certain time
		 */
		Port_LPI
	};
	
	/* LPI minimum wakeup and sleep times for different link speeds, 100Mbps, 1000Mbps, 10Gbps */
	public static double[] sleepStateWakeups = { 30.5e-6, 16.5e-6, 4.48e-6};
	public static double[] sleepStateWaitings = { 200e-6, 182e-6, 2.88e-6};
	//public static double[] sleepStateWakeups = { 0, 0, 0};
	//public static double[] sleepStateWaitings = { 0, 0, 0};
	
	/**
	 * The power state of the port.
	 */
	private PortPowerState powerState;
	
	/** The current power management policy. */
	protected PortPowerPolicy powerPolicy;
	
	
	//record the time that the port is in LPI mode
	private double portLPITime;
	private double LPIstartTime;
	private double LPIendTime;

	private DCNode node = null;
	private Experiment experiment = null;
	private Constants.PortRate portRate;	//public static enum PortRate {Base100, Base1000, Base10G, NUM_OF_RATES};
	private int bufferSize;
	public static Vector<Double> availableRates;
	public static Vector<Double> availableSwitchPortPowers;

	public boolean doPacketRouting;

	//private static Vector<Double> powerScales;
	// private LinkedList<Packet> packetQueue;

	/**
	 * the packet queue for ingress traffic
	 */
	private LinkedList<Packet> inGressPacketQueue;

	/**
	 * the packet queue for outgress traffic
	 */
	private LinkedList<Packet> outGressPacketQueue;
	
	/**
	 * the flow queue for ingress traffic
	 */
	private LinkedList<Flow> inGressFlowQueue;

	/**
	 * the flow queue for outgress traffic
	 */
	private LinkedList<Flow> outGressFlowQueue;

	// private PortState portState;

	private PortPowerState inPortState;
	private PortPowerState outPortState;
	/**
	 * private boolean ack = true;
	 */
	private boolean inAck;
	private boolean outAck;
	
	//clarify which port on which node this port is connected to
	private DCNode connectedNode;
	private int connectedPortID;

	public PortPowerState getInPortState() {
		return inPortState;
	}

	public PortPowerState getOutPortState() {
		return outPortState;
	}
	
	private PortTransitiontoLPIEvent nextLPIevent;
	
	public PortTransitiontoLPIEvent getNextLPIevent() {
		return nextLPIevent;
	}

	public void setNextLPIevent(PortTransitiontoLPIEvent nextLPIevent) {
		this.nextLPIevent = nextLPIevent;
	}
	
	private PortWakeupEvent nextWakeUpevent;
	
	public PortWakeupEvent getNextWakeUpevent() {
		return nextWakeUpevent;
	}
	
	public void setNextWakeUpevent(PortWakeupEvent nextWakeUpevent){
		this.nextWakeUpevent = nextWakeUpevent;
	}

	// parent
	public Port(DCNode _node, PortRate pr, int bSize) {
		
		// preset link rate modes
		availableRates = new Vector<Double>(Arrays.asList(Constants.PORTRATES));	//public static Double[] PORTRATES = { 100.0, 1000.0,10000.0 }; //Mbps
		// the first three values correspond to switch port rate 
		Double[] switchportpower = { Constants.POWER_ACTIVE_PORT, Constants.POWER_LPI_PORT };
		availableSwitchPortPowers = new Vector<Double>(
				Arrays.asList(switchportpower));

		/** packetQueue = new LinkedList<Packet>(); **/
		inGressPacketQueue = new LinkedList<Packet>();
		outGressPacketQueue = new LinkedList<Packet>();

		/** portState = PortState.idle; **/
		inPortState = PortPowerState.idle;
		outPortState = PortPowerState.idle;

		/** ack = true; **/
		inAck = true;
		outAck = true;

		node = _node;
		experiment = node.getDataCenter().experiment;
		bufferSize = bSize;
		portRate = pr;
		doPacketRouting = node.getDataCenter().getExpConfig().doPacketRouting();

		LPIstartTime = 0;
		LPIendTime = 0;
	}
	
	public void updateConnectedPort(){
		//find the adjacent node and the corresponding port this port connects to
		for(int i=0; i<node.mDataCenter.mTopo.getTotalNodeNo();i++){
			if(node.mDataCenter.mTopo.getNodePortsMapping().get(node.matrixNodeId-1).get(i)
					==node.getIdByPort(this)){
				connectedNode=node.getDataCenter().getDCNodeById(i+1);
				break;
			}
		}
		for(int i=0;i<connectedNode.portMap.size();i++){
			if(node.mDataCenter.mTopo.getNodePortsMapping().get((connectedNode.matrixNodeId)-1).get((node.matrixNodeId)-1)
					==(i+1)){
				connectedPortID = i+1;
				break;
			}
		}	
	}
	public int getConnectedPortID(){
		return connectedPortID;
	}
	public Port getConnectedPort(){
		return connectedNode.getPortById(connectedPortID);
	}
	public void setNode(DCNode _node){
		this.node=_node;
	}
	public DCNode getNode(){
		return this.node;
	}
	public DCNode getConnectedNode(){
		return this.connectedNode;
	}
	public int getOutGressQueueSize(){
		if (doPacketRouting) {
			return outGressPacketQueue.size();
		}
		
		return 0;
	}
	
	public int getQueueSize(){
		if(inGressPacketQueue.size()>outGressPacketQueue.size()){
			if (doPacketRouting) {
				return inGressPacketQueue.size();
			}
		} else{
			if (doPacketRouting) {
				return outGressPacketQueue.size();
			}
		}
		
		return 0;
	}
	
	/**
	 * Sets the power management currently used by the port.
	 */
	public void setPowerPolicy(final PortPowerPolicy policy) {
		this.powerPolicy = policy;
		if (powerPolicy == PortPowerPolicy.Port_LPI) {
			this.powerState = PortPowerState.LOW_POWER_IDLE;
			this.inPortState = PortPowerState.LOW_POWER_IDLE;
			this.outPortState = PortPowerState.LOW_POWER_IDLE;
		}
	}
	
	public PortPowerPolicy getPowerPolicy() {
		return this.powerPolicy;
	}
	
	public void TransitiontoLPI(final double time){
		this.powerState = PortPowerState.TRANSITION_TO_LOW_POWER_IDLE;
		updateInOutState();
	}
	public void enterLPI(final double time){
		this.powerState = PortPowerState.LOW_POWER_IDLE;
		updateInOutState();
	}
	public void TransitiontoActive(){
		this.powerState = PortPowerState.TRANSITIONG_TO_ACTIVE;
		updateInOutState();
	}
	//the port exits LPI mode and is set to be in active idle state so that it can transmit data
	public void exitLPI(final double time){
		this.powerState = PortPowerState.idle;
		updateInOutState();
		if (doPacketRouting) {
			if (inGressPacketQueue.size() != 0) {
				Packet nextpacket = inGressPacketQueue.poll();
				insertPacket(nextpacket, time, true);
			}
			if (outGressPacketQueue.size() != 0) {
				Packet nextpacket = outGressPacketQueue.poll();
				insertPacket(nextpacket, time, true);
			}
		}
	}
	public void updateInOutState(){
		this.inPortState = this.powerState;
		this.outPortState = this.powerState;
	}

	public void setPortRate(Constants.PortRate _portRate) {
		this.portRate = _portRate;
	}

	public void setInPortState(PortPowerState ps) {
		this.inPortState = ps;
	}

	public void setOutPortState(PortPowerState ps) {
		this.outPortState = ps;
	}
	
	public void setPortState(PortPowerState ps){
		this.powerState = ps;
		this.inPortState = ps;
		this.outPortState = ps;
	}
	public PortPowerState getPortState(){
		return this.powerState;
	}

	/**
	 * public void setAck(boolean ack) { this.ack = ack; }
	 **/

	public void setInAck(boolean ack) {
		this.inAck = ack;
	}

	public void setOutAck(boolean ack) {
		this.outAck = ack;
	}

	public void markMessageStart(Packet packet, double time) {

	}

	public double getPortRate() {
		return availableRates.get(portRate.ordinal());
	}

	public double getPortBusyPower() {
		return availableSwitchPortPowers.get(0);
	}

	/**
	 * insert a packet to a port: the first thing is to check whether the port
	 * is full or not. If the port queue is full, then this packet needs to be
	 * retransmitted from last port; else if the port is busy, this packet could
	 * be added to the queue of the port and be addressed later; otherwise, the
	 * port could process this packet(it means to add PacketArriveEvent to the
	 * next port after some processing time)
	 * 
	 * @param packet
	 * @param time
	 * @param cont, set to true if this packet is retrieved directly from the queue,         
	 */
	public void insertPacket(Packet packet, double time, boolean cont) {
		LinkedList<Packet> packetQueue = null;
		PortPowerState portState;
		boolean isInPort;
		boolean ack;
		
		int portsPerLineCard = experiment.getExpConfig().getPortsPerLineCard();
		int linecardId = (node.getIdByPort(this) - 1) / portsPerLineCard + 1;
		// if the next hop port is ingress port
		//if packet arrives for the first time and next hop is ingress or if packet is previously arrived and next hop is outgress
		if (cont ^ packet.isNextHopIngress()) {
			packetQueue = inGressPacketQueue;
			portState = inPortState;
			isInPort = true;
			ack = inAck;

		} else {
			packetQueue = outGressPacketQueue;
			portState = outPortState;
			isInPort = false;
			ack = outAck;
		}

		if (packetQueue.size() == bufferSize) {
			// need to retransmit this packet by inserting the packet again
			// after some time
			System.out.println("Time : " + time + " Need retransmission " + " to switch " + this.node.getNodeId() + " port"
					+ node.getIdByPort(this) + " (sServer: #" + packet.getSourceNode().getNodeId() + "---> dServer: #"
					+ packet.getDestinationNode().getNodeId() + ")");
			System.out.println("!!!!!!!!");
			// this.insertpacket(packet, time + 0.009, cont, ack);
			if (this.node != null) {
				PacketArriveEvent msgEvent = new PacketArriveEvent(time + 0.003, node.getDataCenter().experiment, packet,
						this.node, node.getIdByPort(this), linecardId);
				node.getDataCenter().experiment.addEvent(msgEvent);
			}
			return;
		} else {
			/**
			 * @ author:jingxin because the buffer is not full, no matter what the port state is (active (busy/idle) or LPI), 
			 * the packet could either be added to the queue or being processed immediately, thus we could
			 * remove the packet stored in the previous port by adding PacketTransmittedEvent to the last hop port
			 */

			if (!cont) {
				packet.forwardCurrentHop();
				if (this.node != packet.getSourceNode()) {
					// remove packet from the output port of the last switch of the route

					// fanyao
					// FIXME: if buffer is not full, the packet first needs to be received
					// that means: first, we should add some delay; second, the status of the
					// port should be changed considering full-duplex port configuration
					PacketTransmittedEvent mtEvent = new PacketTransmittedEvent(time, node.getDataCenter().experiment, packet, 
							node.getDataCenter().getDCNodeById(packet.getLastNodeId()), packet.getLastPortId(), linecardId);
					node.getDataCenter().experiment.addEvent(mtEvent);
					System.out.println("lubingqian0");
				}
			}
			// ***************************************************//

			if ((portState == PortPowerState.idle) && ack || cont || (packet.isNextHopIngress())) {
				/** portState = PortState.busy; **/
				if (isInPort) {
					inPortState = PortPowerState.busy;
					//System.out.println("in port state set to busy");

				} else {
					outPortState = PortPowerState.busy;
					//System.out.println("out port state set to busy");
				}
				
				//cancel the TransitiontoLPI event if tao>0
				
				if(node.getDataCenter().getExpConfig().getLcWaitTime(0) > 0){
					if (this.getNextLPIevent() != null){
						node.getDataCenter().experiment.cancelEvent(this.getNextLPIevent());
						this.setNextLPIevent(null);
					}
					if(connectedNode.getPortById(connectedPortID).getNextLPIevent() != null){
						node.getDataCenter().experiment.cancelEvent(connectedNode.getPortById(connectedPortID).getNextLPIevent());
						connectedNode.getPortById(connectedPortID).setNextLPIevent(null);
					}
				}
				
				markMessageStart(packet, time);
				
				double tTime = (Packet.packet_size * 8)
						/ (Constants.portRateMap.get(portRate) * Math.pow(10, 6));
				// FIXME: the added constant needs to be explained
				time = time + tTime;
				//time = time + tTime + Constants.RoutingDelay.PACKET_TRANSMIT_DELAY;

				// try to find current switch only if the current hop is switch

				// 0.002s is added to time, DestinationReceivedEvent is treated
				// same as
				// PacketArrivalEvent for processor
				if (this.node == packet.getDestinationNode()) {

					// first of all, create a PacketTransmitted event for current port
					// as we the "next hop", processor would always accept the packet
					// PacketTransmittedEvent mtEvent = new PacketTransmittedEvent(time + 0.002, experiment, packet, node,packet.getCurrentPortId());
					// node.getDataCenter().experiment.addEvent(mtEvent);

					DestinationReceivedEvent drEvent = new DestinationReceivedEvent(
							time + RoutingDelay.PROPOGATIONG_DELAY + RoutingDelay.PACKET_ARRIVE_DELAY, node.getDataCenter().experiment,
							packet, node, packet.getDestinationPortId());
					node.getDataCenter().experiment.addEvent(drEvent);
				}
				else {
					PacketSendEvent mtEvent = new PacketSendEvent(time, node.getDataCenter().experiment,
							packet, node.getDataCenter().getDCNodeById(packet.getCurrentNodeId()),
							packet.getCurrentPortId(), (packet.getCurrentPortId() - 1) / portsPerLineCard + 1);
					node.getDataCenter().experiment.addEvent(mtEvent);
					System.out.println("lubingqian1");
					int matrixNodeId = packet.getNextHopNodeId();
					PacketArriveEvent msgEvent = new PacketArriveEvent(
							time + RoutingDelay.PROPOGATIONG_DELAY +  RoutingDelay.PACKET_ARRIVE_DELAY, node.getDataCenter().experiment,
							packet, node.getDataCenter().getDCNodeById(
									matrixNodeId), packet.getNextHopPortId(), (packet.getNextHopPortId() - 1) / portsPerLineCard + 1);
					node.getDataCenter().experiment.addEvent(msgEvent);
					System.out.println("lubingqian2");
				}

			} else {
				packetQueue.add(packet);
				//add a wake up event if the port is transitioning to idle
				
			}
			
		}
	}

	public void removePacket(Packet packet, double time,
			boolean fromPacketTransmitted) {

		LinkedList<Packet> packetQueue = null;
		boolean isInPort;

		if (!fromPacketTransmitted) {
			this.setInAck(true);
			isInPort = true;
			packetQueue = inGressPacketQueue;
		}

		else {
			if (packet.isLastHopIngress()) {
				/** this.ack = true; **/
				this.setInAck(true);
				packetQueue = inGressPacketQueue;
				isInPort = true;

			} else {
				this.setOutAck(true);
				packetQueue = outGressPacketQueue;
				isInPort = false;

			}
		}

		if (packetQueue.size() == 0) {
			// nothing to do here
			if (isInPort) {
				inPortState = PortPowerState.idle;
				//System.out.println("in port state set to idle");
			} else {
				outPortState = PortPowerState.idle;
				//System.out.println("out port state set to idle");
			}
		} else {
			Packet nextpacket = packetQueue.poll();
			insertPacket(nextpacket, time, true);
		}
		
		if((inPortState==PortPowerState.idle)&&(outPortState==PortPowerState.idle) &&(outGressPacketQueue.size()==0)){
			if((connectedNode.getPortById(connectedPortID).inPortState==PortPowerState.idle)
					&&(connectedNode.getPortById(connectedPortID).outPortState==PortPowerState.idle)
					&&(connectedNode.getPortById(connectedPortID).outGressPacketQueue.size()==0)){
			
				
				double LPItao=node.getDataCenter().getExpConfig().getLcWaitTime(0);
				int portsPerLineCard = experiment.getExpConfig().getPortsPerLineCard();
				
				int linecardId = (node.getIdByPort(this) - 1) / portsPerLineCard + 1;
				PortTransitiontoLPIEvent ttp1LPIEvent = new PortTransitiontoLPIEvent(
						time+LPItao, node.getDataCenter().experiment,
						node, node.getIdByPort(this), linecardId);
				node.getDataCenter().experiment.addEvent(ttp1LPIEvent);
				this.setNextLPIevent(ttp1LPIEvent);
				int connectedLcId = (connectedPortID - 1) / portsPerLineCard + 1;
				PortTransitiontoLPIEvent ttp2LPIEvent = new PortTransitiontoLPIEvent(
						time+LPItao, connectedNode.getDataCenter().experiment,
						connectedNode, connectedPortID, connectedLcId);
				node.getDataCenter().experiment.addEvent(ttp2LPIEvent);
				connectedNode.getPortById(connectedPortID).setNextLPIevent(ttp2LPIEvent);
			}
		}
	}
	
	public void insertFlow(Flow flow, double time, boolean cont) {
		LinkedList<Flow> flowQueue = null;
		PortPowerState portState;
		boolean isInPort;
		boolean ack;
		
		int portsPerLineCard = experiment.getExpConfig().getPortsPerLineCard();
		int linecardId = (node.getIdByPort(this) - 1) / portsPerLineCard + 1;
		// if the next hop port is ingress port
		//if packet arrives for the first time and next hop is ingress or if packet is previously arrived and next hop is outgress
		if (cont ^ flow.isNextHopIngress()) {
			portState = inPortState;
			isInPort = true;
			ack = inAck;

		} else {
			portState = outPortState;
			isInPort = false;
			ack = outAck;
		}

//		if (flowQueue.size() == bufferSize) {
//			// need to retransmit this packet by inserting the packet again
//			// after some time
//			System.out.println("Time: " + time + ", Need retransmission " + " to switch " + this.node.getNodeId() + " port"
//					+ node.getIdByPort(this) + " (sServer: #" + flow.getSourceNode().getNodeId() + "---> dServer: #"
//					+ flow.getDestinationNode().getNodeId() + ")");
//			System.out.println("!!!!!!!!");
//			// this.insertpacket(packet, time + 0.009, cont, ack);
//			if (this.node != null) {
//				FlowArriveEvent msgEvent = new FlowArriveEvent(time + 0.003, node.getDataCenter().experiment, flow,
//						this.node, node.getIdByPort(this), linecardId);
//				node.getDataCenter().experiment.addEvent(msgEvent);
//			}
//			return;
//		} else {
			/**
			 * @ author:jingxin because the buffer is not full, no matter what the port state is (active (busy/idle) or LPI), 
			 * the packet could either be added to the queue or being processed immediately, thus we could
			 * remove the packet stored in the previous port by adding PacketTransmittedEvent to the last hop port
			 */

			if (!cont) {
				flow.forwardCurrentHop();
				if (this.node != flow.getSourceNode()) {
					// remove packet from the output port of the last switch of the route

					// fanyao
					// FIXME: if buffer is not full, the packet first needs to be received
					// that means: first, we should add some delay; second, the status of the
					// port should be changed considering full-duplex port configuration
					FlowTransmittedEvent mtEvent = new FlowTransmittedEvent(time, node.getDataCenter().experiment, flow, 
							node.getDataCenter().getDCNodeById(flow.getLastNodeId()), flow.getLastPortId(), linecardId);
					node.getDataCenter().experiment.addEvent(mtEvent);
				}
			}
			// ***************************************************//

//			if ((portState == PortPowerState.idle) && ack || cont || (flow.isNextHopIngress())) {
				/** portState = PortState.busy; **/
				if (isInPort) {
					inPortState = PortPowerState.busy;
					//System.out.println("in port state set to busy");

				} else {
					outPortState = PortPowerState.busy;
					//System.out.println("out port state set to busy");
				}
				
				//cancel the TransitiontoLPI event if tao>0
				
				if(node.getDataCenter().getExpConfig().getLcWaitTime(0) > 0){
					if (this.getNextLPIevent() != null){
						node.getDataCenter().experiment.cancelEvent(this.getNextLPIevent());
						this.setNextLPIevent(null);
					}
					if(connectedNode.getPortById(connectedPortID).getNextLPIevent() != null){
						node.getDataCenter().experiment.cancelEvent(connectedNode.getPortById(connectedPortID).getNextLPIevent());
						connectedNode.getPortById(connectedPortID).setNextLPIevent(null);
					}
				}
				
				//markMessageStart(flow, time);
				
				double tTime = (flow.getFlowSize() * 8)
						/ (Constants.portRateMap.get(portRate) * Math.pow(10, 6));
				// FIXME: the added constant needs to be explained
				time = time + tTime;
				//time = time + tTime + Constants.RoutingDelay.PACKET_TRANSMIT_DELAY;

				// try to find current switch only if the current hop is switch

				// 0.002s is added to time, DestinationReceivedEvent is treated
				// same as
				// PacketArrivalEvent for processor
				if (this.node == flow.getDestinationNode()) {
					// first of all, create a PacketTransmitted event for current port
					// as we the "next hop", processor would always accept the packet
					// PacketTransmittedEvent mtEvent = new PacketTransmittedEvent(time + 0.002, experiment, packet, node,packet.getCurrentPortId());
					// node.getDataCenter().experiment.addEvent(mtEvent);

					DestinationReceivedFlowEvent drEvent = new DestinationReceivedFlowEvent(
							time + RoutingDelay.PROPOGATIONG_DELAY + RoutingDelay.PACKET_ARRIVE_DELAY, node.getDataCenter().experiment,
							flow, node, flow.getDestinationPortId());
					node.getDataCenter().experiment.addEvent(drEvent);
				}
				else {
					FlowSendEvent mtEvent = new FlowSendEvent(time, node.getDataCenter().experiment,
							flow, node.getDataCenter().getDCNodeById(flow.getCurrentNodeId()),
							flow.getCurrentPortId(), (flow.getCurrentPortId() - 1) / portsPerLineCard + 1);
					node.getDataCenter().experiment.addEvent(mtEvent);
					
					int matrixNodeId = flow.getNextHopNodeId();
					FlowArriveEvent msgEvent = new FlowArriveEvent(
							time + RoutingDelay.PROPOGATIONG_DELAY +  RoutingDelay.PACKET_ARRIVE_DELAY, node.getDataCenter().experiment,
							flow, node.getDataCenter().getDCNodeById(
									matrixNodeId), flow.getNextHopPortId(), (flow.getNextHopPortId() - 1) / portsPerLineCard + 1);
					node.getDataCenter().experiment.addEvent(msgEvent);
				}
//
//			} else {
//				flowQueue.add(flow);
//				//add a wake up event if the port is transitioning to idle
//				
//			}
			
//		}
	}

	public void removeFlow(Flow flow, double time,
			boolean fromFlowTransmitted) {

		boolean isInPort;

		if (!fromFlowTransmitted) {
			this.setInAck(true);
			isInPort = true;
		}

		else {
			if (flow.isLastHopIngress()) {
				/** this.ack = true; **/
				this.setInAck(true);
				isInPort = true;

			} else {
				this.setOutAck(true);
				isInPort = false;

			}
		}

		// nothing to do here
		if (isInPort) {
			inPortState = PortPowerState.idle;
			//System.out.println("in port state set to idle");
		} else {
			outPortState = PortPowerState.idle;
			//System.out.println("out port state set to idle");
		}
		
		// Update switch sleep states
		LinecardNetworkScheduler scheduler = (LinecardNetworkScheduler)(this.experiment.getTaskScheduler());
		scheduler.updateSwitchThresholds(this, connectedNode.getPortById(connectedPortID), connectedPortID, node, connectedNode, time);
	}

	public double getPortLPITime() {
		return portLPITime;
	}

	public void updatePortLPITime() {
		this.portLPITime = portLPITime + (LPIendTime - LPIstartTime);
	}

	public double getLPIstartTime() {
		return LPIstartTime;
	}

	public void setLPIstartTime(double lPIstartTime) {
		LPIstartTime = lPIstartTime;
	}

	public double getLPIendTime() {
		return LPIendTime;
	}

	public void setLPIendTime(double lPIendTime) {
		LPIendTime = lPIendTime;
	}
}
