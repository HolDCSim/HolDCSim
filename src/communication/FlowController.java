package communication;

import infrastructure.DCNode;
import infrastructure.DataCenter;
import infrastructure.Link;
import infrastructure.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import utility.FlowSizeGen;
import utility.Pair;
import communication.Packet.Route;
import constants.Constants;
import debug.Sim;
import event.DestinationReceivedEvent;
import event.FlowArriveEvent;
import event.PacketArriveEvent;
import event.TaskArrivalEvent;
import event.TaskFinishEvent;
import experiment.CombineExperiment;
import experiment.Experiment;
import experiment.NetworkExperiment;
import communication.CommunicationBundle;
import job.Job;
import job.Task;
import network.Djikstra;
import topology.Topology.NetworkRoutingAlgorithm;
import constants.Constants;

/**
 * @author fan
 *
 */
public class FlowController {
	// flag for packet based or flow based routing
	private boolean packetRouting = false;
	// used to generate unique id for each flow
	private static Long flowUUID = (long) 0;
	// flow tables for all flows existing in the current data center
	//private Map<Long, Flow> flowMap;
	
	//all current active flows in the network
	private Vector<Flow> activeFlows;

	private Experiment mExp;

	/*
	 * used to store the links that have currently been seem used so far
	 * Datacenter class should create all the links at initialization.
	 * Pair<Integer, Integer> is the pair of index for source and destination
	 */
	// FIXME: this design is not compatible for topologies with duplicate links
	// between same pair of nodes
	public Map<Pair<Integer, Integer>, Link> cachedLinks;

	public FlowController(Experiment exp) {
		this.mExp = exp;
		this.activeFlows = new Vector<Flow>();
		this.cachedLinks = new HashMap<Pair<Integer,Integer>, Link>();
	}


	/**
	 * @param stask
	 *            source task
	 * @param dtask
	 *            destination task routing between task are supported at two
	 *            different granularities 1. packet based routing, each packet
	 *            is associated with an event 2. flow based routing
	 *            communication is modeled as amount of data to be transfered
	 * 
	 */
	public void routePackets(Task stask, Vector<Task> dependingTaskList,
			double time) {
		// this functionality has already been implemented by our job model
		int taskOrder = 0;
		for (Task iTask : dependingTaskList) {
			taskOrder++;
			/* fan yao commented, */
			/*
			 * originally only one message is created, since we need multiple
			 * message we need to maintain a list of messages, and their
			 * mappings (sourceTask, destTask) ==> ArrayList<Message> currently
			 * we could generate the list of message when initialize experiment
			 */

			/*
			 * now we get the message list and push them to the server port for
			 * simplicity, just assume that the buffer in server size is now
			 * infinite
			 */
			/*
			 * do scheduling and assign a server for dest task; generate PacketsList between two tasks;
			 */
			ArrayList<Packet> insertedPackets = stask.getaJob()
					.getPacketsBetween2Task(stask, iTask, time);

			if (insertedPackets == null) {
				System.out.println("no packets required\n");
				return;
			}
			if(stask.getServer()==iTask.getServer()){
				System.out.println("no network communication required\n");
				Vector<Task> dependedList = stask.getaJob().getTaskDependency().get(iTask);
				if (dependedList == null) {
					System.err.println("error, dependedList is null\n");
					System.exit(0);
				}
				dependedList.remove(stask);
				if (stask.getaJob().getChildTasks(stask).size() == 0) {
					TaskFinishEvent finishEvent = new TaskFinishEvent(time, mExp,
							stask, stask.getServer(), time, 0);
					mExp.addEvent(finishEvent);
				}

				if (dependedList.size() == 0) {
					// needs to check if successful
					stask.getaJob().getTaskDependency().remove(iTask);
					TaskArrivalEvent tEvent = new TaskArrivalEvent(time+0.002, mExp,
							iTask, iTask.getServer());
					mExp.addEvent(tEvent);
				}
				

			}

			/*
			 * if there are message to be transferred, we just append all of
			 * them to the server port by generating a bunch of Packet Arrival
			 * Events which have different delays
			 */
			// Packet aPacket = insertedPackets.get(0);
			// if(aPacket.getSourceNode() == aPacket.getDestinationNode()){
			//
			// }
			else{
				int k = 0;
				for (Packet aPacket : insertedPackets) {
					k++;
					
					int portsPerLineCard = mExp.getExpConfig().getPortsPerLineCard();
					PacketArriveEvent pckEvent = new PacketArriveEvent(time
							+ taskOrder * 0.001 + k
							* Constants.RoutingDelay.PACKET_SEQUENCE_DELAY, mExp,
							aPacket, (DCNode) stask.getServer(),
							aPacket.getStartPortIndex(), (aPacket.getStartPortIndex() - 1) / portsPerLineCard + 1);
					/*
					 * here is another consideration, we should not add all the
					 * packets of one task and then packets from another realistic
					 * case is round-robin
					 */
					mExp.addEvent(pckEvent);
				}
			}
			
		}

	}

	protected void forwardFlows(Task stask, Vector<Task> dependingTaskList,
			double time) {
			ArrayList<Link> pathLinks = new ArrayList<Link>();
		for (Task iTask : dependingTaskList) {
			DataCenter dataCenter = stask.getaJob().dataCenter;
			
			int srcServerId = stask.getServer().getNodeId();
			int dstServerId = iTask.getServer().getNodeId();

			// Dependent tasks are on same server. No communication required
			if (srcServerId == dstServerId) {
				Job job = stask.getaJob();
				job.removeDependencyOnSameServer(stask, iTask);
				
				continue;
			}
			CommunicationBundle commBundle = stask.getaJob().getCommBundle(stask, iTask, time);
			Vector<Vector<Integer>> path = commBundle.getPath();
			
			Flow oneFlow = new Flow(dataCenter.getServerById(srcServerId),
					dataCenter.getServerById(dstServerId), stask, iTask, time,
					commBundle.getFlowSize(), mExp, path);
			
			
			/*
			 * create the link if necessary and put them in the cache one
			 * element is one pathNode
			 */
			//initial to the DCNode index of first node in the path
			//System.out.println("path"+path);
			int startNodeId = path.get(0).get(0);
			for (int i = 1; i < path.size(); i++) {
				int currentNodeId = path.get(i).get(0);
				//System.out.println("Path links are: currentNodeID " +currentNodeId + " startNodeID " + startNodeId);
				Pair<Integer, Integer> nodePair = new Pair<Integer, Integer>(
						startNodeId, currentNodeId);
				Link oneLink = null;
				// the link does not exist, create it
				if (cachedLinks.get(nodePair) == null) {
					// FIXME: for now the port number doesn't matter, needs to
					// be specified in the future
					oneLink = new Link(dataCenter.getDCNodeById(startNodeId),
							dataCenter.getDCNodeById(currentNodeId),
							startNodeId, currentNodeId, null, null);
					cachedLinks.put(nodePair, oneLink);

				} else {
					oneLink = cachedLinks.get(nodePair);
				}
				
				oneLink.addFlow(oneFlow);
				pathLinks.add(oneLink);
				
				startNodeId = currentNodeId;
				
				
			}

			oneFlow.setFlowLinks(pathLinks);
			activeFlows.add(oneFlow);
			
			int portsPerLineCard = mExp.getExpConfig().getPortsPerLineCard();
			FlowArriveEvent flowEvent = new FlowArriveEvent(time
					+ 1 * 0.001 + 1
					* Constants.RoutingDelay.PACKET_SEQUENCE_DELAY, mExp,
					oneFlow, (DCNode) stask.getServer(),
					oneFlow.getStartPortIndex(), (oneFlow.getStartPortIndex() - 1) / portsPerLineCard + 1);
			mExp.addEvent(flowEvent);
		}
		maxMinFairAllocate(new ArrayList<Link>(cachedLinks.values()), activeFlows, time);
		
		for(Link oneLink : pathLinks) {
			if(mExp.getExpConfig().getLinkAnimation() == 1) {
				double Utilized = this.calculateUtilization(oneLink);
				Pair <Double,Double> pair = new Pair<Double,Double>();
				pair.setFirst(Utilized);
				pair.setSecond(time);
				oneLink.UtilTrace.add(pair);
				System.out.println("Link "+ oneLink.hashCode()+":"+ pair);
			}
		}
	}
	
	protected void maxMinFairAllocate(ArrayList<Link> activeLinks, Vector<Flow> flows, double time){
		int flowsAllocated = 0;		
		Link aggLink= null;
		for(Link aLink : activeLinks){
			aLink.prepareAllocation();
		}
		while(flowsAllocated != flows.size()){
			double minShare = activeLinks.get(0).linkRate;
			for(Link aLink : activeLinks){
				double tempShare = aLink.getCurrentCap() / aLink.unAllocatedFlows.size();
				if(minShare >= tempShare){
					aggLink = aLink;
					minShare = tempShare;
				}
			}
			
			//System.out.println(minShare);
			//now aggLink would be the saturated link
			flowsAllocated += aggLink.unAllocatedFlows.size();
			activeLinks.remove(aggLink);
			
			//update the allocated flows for other links along the flow
			Vector<Flow> allocatedFlows = aggLink.unAllocatedFlows;
			
			for(Flow aFlow: allocatedFlows){
				double newBandwidth = aggLink.getCurrentCap() / allocatedFlows.size();
				double edgeSwitchBandwidth = mExp.getExpConfig().getEdgeSwitchBW();
			      aFlow.setAggLink(aggLink);
				// Flow cannot go faster than bw of edge switch
				if(newBandwidth <= edgeSwitchBandwidth) {
					aFlow.setCurBandwidth(newBandwidth, time);
				}
				else {
					aFlow.setCurBandwidth(edgeSwitchBandwidth, time);
				}
				
				for(Link aPathLink : aFlow.getPathLinks()){
					if(aPathLink != aggLink){
						aPathLink.setCurrentCap(aPathLink.getCurrentCap() - aFlow.getCurBandwidth());
						aPathLink.unAllocatedFlows.remove(aFlow);
						aPathLink.allocatedFlows.add(aFlow);
						//if this path no longer has unallocated flows, remove this link 
						if(aPathLink.unAllocatedFlows.size() == 0){
							activeLinks.remove(aPathLink);
						}
					}
				}
				
				aggLink.setCurrentCap(aggLink.getCurrentCap() - aFlow.getCurBandwidth());
			}
			aggLink.allocatedFlows.addAll(aggLink.unAllocatedFlows);
			aggLink.unAllocatedFlows.clear();
		}
		
		// Update weight matrix for network routing
		Djikstra djikstra = mExp.getDataCenter().mTopo.getAbGen().getDjikstra();
		for (Link link : cachedLinks.values()) {
			djikstra.updateLink(link.srcNode.getMatrixNodeId(), link.dstNode.getMatrixNodeId(), link.linkRate,
					link.allocatedFlows.size());
		}
	}
	
	public void flowCompleted(Flow flow, double time){
		ArrayList<Link> links = flow.getPathLinks();
//		System.out.println("flow completed, links"+links);

		if(!activeFlows.remove(flow)){
			Sim.fatalError("remove link from activeFlows failed");
		}
		for(Link aLink : links){
			aLink.getCurrentFlows().remove(flow);
			if(aLink.getCurrentFlows().size() == 0){
				//there is currently no active flows along the link, remove it
				int srcNodeIndex = aLink.getSrcNodeIndex();
				int dstNodeIndex = aLink.getDstNodeIndex();
				
				Pair<Integer, Integer> key = new Pair<Integer, Integer>();
				key.setFirst(srcNodeIndex);
				key.setSecond(dstNodeIndex);
				// Update weight matrix for network routing
				Djikstra djikstra = mExp.getDataCenter().mTopo.getAbGen().getDjikstra();
				djikstra.updateLink(aLink.srcNode.getMatrixNodeId(), aLink.dstNode.getMatrixNodeId(), aLink.linkRate, 0);
			}
		}
		
		maxMinFairAllocate(new ArrayList<Link>(cachedLinks.values()), activeFlows, time);
		for(Link aLink:links) {
			if(mExp.getExpConfig().getLinkAnimation() == 1) {
				double Utilized = this.calculateUtilization(aLink);
				Pair <Double,Double> pair = new Pair<Double,Double>();
				pair.setFirst(Utilized);
				pair.setSecond(time);
				aLink.UtilTrace.add(pair);
				System.out.println("Link "+ aLink.hashCode()+":"+ pair);
			}
		}
	}
	
	/**
	 * @param finishedFlow
	 *            clean one flow at a time, this could later be optimzied to
	 *            clean a set of flows that finish at the same time
	 */
	public void cleanupFlow(Flow finishedFlow) {
		//now simpley assume that cleanupflow does nothing 
	}

	public void buildCommunications(Task task, Vector<Task> dependingTaskList,
			double time) {
		Job sJob = task.getaJob();
		
		//check routing mode
		if (sJob.dataCenter.getExpConfig().doPacketRouting()) {
			routePackets(task, dependingTaskList, time);
		} else {
			forwardFlows(task, dependingTaskList, time);
		}
	}

	public Link checkLinkBW(DCNode start, DCNode end) {

		Pair<Integer,Integer> pair = new Pair<Integer, Integer>();
		pair.setFirst(start.getNodeId() - 1);
		pair.setSecond(end.getNodeId() - 1);

		if (cachedLinks.get(pair) == null) {
		    return null;
		} else {
			return cachedLinks.get(pair);


		}
	}

	public double getusedBW(Link link)
	{
		double usedBW=0;
		for (Flow aflow : link.allocatedFlows) {
			usedBW+=aflow.getCurBandwidth();
		}
		return usedBW;
	}

	public double additionalAwakeTimeforSwitch(Link link) {
		double avgFlowSize = mExp.getExpConfig().getAvgFlowSize();
            double timeToSleep = -1;
            double  flowRemaining=0;
            Flow slowestCurrentFlow=null;

            double UtilizedBW=0;
            double UtilizedminBW=0;

		double edgeSwitchBW = mExp.getExpConfig().getEdgeSwitchBW();
		double qosTimeConstraint = mExp.getExpConfig().getJobQoS() * (avgFlowSize / edgeSwitchBW);


		for (Flow aflow : link.allocatedFlows) {
			double flowsize=aflow.getFlowSize();
			double dataTransmitted = aflow.dataTransmitted + ((mExp).getCurrentTime() - aflow.lastBWSetTime) * aflow.getCurBandwidth();

			double timeRemaining = (flowsize - dataTransmitted)  / aflow.getCurBandwidth();
			double maxTimeRemaining = (flowsize - dataTransmitted) / flowsize * qosTimeConstraint;
			double minBWrequired= 1/maxTimeRemaining;

			if (timeRemaining > timeToSleep) {
				timeToSleep = timeRemaining;
				slowestCurrentFlow = aflow;
				flowRemaining= flowsize - dataTransmitted;
			}
			UtilizedminBW+=minBWrequired;
			UtilizedBW+=aflow.getCurBandwidth();
		}

		double availBW=((link.linkRate - UtilizedBW) > edgeSwitchBW)? edgeSwitchBW : (link.linkRate - UtilizedBW);
		if ((link.linkRate - UtilizedBW )>  qosTimeConstraint) {

			double timefornewFlow= availBW / avgFlowSize;

			if (timefornewFlow > timeToSleep) return (timefornewFlow - timeToSleep);
			else
				return 0;

		} else {
			// We consider the worstcase
			//System.out.println(link.srcNode.getNodeId() + "  " + link.dstNode.getNodeId());
			//double otherFlowIncreasedSlowTime= (slowestCurrentFlow.getCurBandwidth() - ((avgFlowSize/qosTimeConstraint))/ link.getNumOfFlows()) / flowRemaining ;



			//if (otherFlowIncreasedSlowTime <0) otherFlowIncreasedSlowTime=0; //assert this.

//			double timefornewFlow= qosTimeConstraint;
			double timefornewFlow = link.linkRate - UtilizedminBW;

			if (timefornewFlow > timeToSleep) return timefornewFlow-timeToSleep;
			else {
				return 0;
			}
		}



	}

	public double additionalTimeOtherFlows(Link link) {
		double avgFlowSize = mExp.getExpConfig().getAvgFlowSize();
		double time=0;
		double time2;
		for (Flow aflow : link.allocatedFlows) {
			 if (aflow.getAggLink().equals(link)) {

			 	double flowsize= aflow.getFlowSize() ;
			 	double datatransmitted= aflow.dataTransmitted;

			 	datatransmitted+= ((mExp).getCurrentTime() - aflow.lastBWSetTime) * aflow.getCurBandwidth();

			 	double flowsizeRemaining= avgFlowSize - datatransmitted;

			 	time2 = flowsizeRemaining /(aflow.getCurBandwidth() - link.linkRate / (link.getNumOfFlows() + 1)) ;

			 	if (time2>0) time+=time2 ; //Additional time for all switches to stay active.
			 }
		}
			return time;
	}
	
	
	public double calculateUtilization(Link link) {
		double Utilization;
		
		double UtilizedBW=0;
		if(link.getCurrentFlows() != null) {
			
			for(Flow aflow: link.getCurrentFlows()) {
				//System.out.println(aflow.getCurBandwidth());
				UtilizedBW = UtilizedBW + aflow.getCurBandwidth();
			}
		}
		Utilization = UtilizedBW/link.linkRate;
		//System.out.println("UtilTrace: "+link.UtilTrace);
		return Utilization;
	}
}
