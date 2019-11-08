package communication;

import java.util.ArrayList;
import java.util.Vector;

import event.DestinationReceivedFlowEvent;
import event.FlowArriveEvent;
import experiment.Experiment;
import job.Job;
import job.Task;
import infrastructure.*;

public class Flow {
	
	private double startTime;
	private double stopTime;
	public double lastBWSetTime;
	// flowsize is meaured in Megabits
	private double flowSize;
	public double dataTransmitted;

	private Task srcTask;
	private Task dstTask;
	private Job mJob;

	private double curBandwidth;
	private DCNode srcNode;
	private DCNode dstNode;
	private Experiment mExp;
	
	private FlowArriveEvent nextFlowArrival;
	private DestinationReceivedFlowEvent lastFlowArrival;
	
	private ArrayList<Link> links;
	private Route mRoute;
	
	private int currentHop;
	private int currentPortId;

	private int lastHop;
	private int lastPortId;
	
	private boolean waitingForLineCardWakeup = false;
	
	
	public double getStartTime() {
		return startTime;
	}

//	public void setStartTime(double startTime) {
//		this.startTime = startTime;
//	}

	public double getStopTime() {
		return stopTime;
	}

	public void setStopTime(double stopTime) {
		this.stopTime = stopTime;
	}

	public double getFlowSize() {
		return flowSize;
	}

//	public void setFlowSize(double flowSize) {
//		this.flowSize = flowSize;
//	}

//	public Task getSrcTask() {
//		return srcTask;
//	}
//
//	public void setSrcTask(Task srcTask) {
//		this.srcTask = srcTask;
//	}
//
//	public Task getDstTask() {
//		return dstTask;
//	}
//
//	public void setDstTask(Task dstTask) {
//		this.dstTask = dstTask;
//	}

	public double getCurBandwidth() {
		return curBandwidth;
	}

	public void setCurBandwidth(double newBandwidth, double time) {
//		if(mFCEvent ==  null){
//			Sim.fatalError("FlowCompletedEvent is null");
//		}
		double duration = time - lastBWSetTime;
		lastBWSetTime = time;
		dataTransmitted += duration * this.curBandwidth;
		this.curBandwidth = newBandwidth;
		
		double newEstimatedTime = time + (flowSize - dataTransmitted)/ newBandwidth;
		
		// Bandwidth changed so time arrival of flow also changes
		if(nextFlowArrival != null) {
			// Get parameters of flow event
			DCNode node = nextFlowArrival.getMNode();
			int portId = nextFlowArrival.getPortId();
			int linecardId = nextFlowArrival.getLinecardId();
			
			mExp.cancelEvent(nextFlowArrival);
			
			nextFlowArrival = new FlowArriveEvent(newEstimatedTime, mExp, this, node, portId, linecardId);
			mExp.addEvent(nextFlowArrival);
			
			
		}
		
		if(lastFlowArrival != null) {
			// Get parameters of flow event
			DCNode node = lastFlowArrival.getNode();
			int portId = lastFlowArrival.getPortId();
			
			mExp.cancelEvent(lastFlowArrival);
			
			lastFlowArrival = new DestinationReceivedFlowEvent(newEstimatedTime, mExp, this, node, portId);
			mExp.addEvent(lastFlowArrival);
		}
	}
	
	public void finishFlow(double time){
		this.setStopTime(time);
	}
	
	public void setFlowLinks(ArrayList<Link> links){
//		System.out.println("1111111111111111111"+links.get(0)+"2222222"+links.get(0).getDstNodeIndex());
//		System.out.println("1111111111111111111"+links.get(1).getSrcNodeIndex()+"2222222"+links.get(1).getDstNodeIndex());
//		System.out.println("1111111111111111111"+links.get(links.size()-1).getSrcNodeIndex()+"2222222"+links.get(links.size()-1).getDstNodeIndex());
		this.links = links;
	}

//	public DCNode getSrcNode() {
//		return srcNode;
//	}
//
//	public void setSrcNode(DCNode srcNode) {
//		this.srcNode = srcNode;
//	}
//
//	public DCNode getDstNode() {
//		return dstNode;
//	}
//
//	public void setDstNode(DCNode dstNode) {
//		this.dstNode = dstNode;
//	}
	
	public double getThroughput(){
		return this.flowSize/(stopTime - startTime);
	}

	public Flow(DCNode srcNode, DCNode dstNode, Task srcTask, Task dstTask,
			double time, double flowSize, Experiment mExp, Vector<Vector<Integer>> path) {
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		this.srcTask = srcTask;
		this.dstTask = dstTask;
		this.startTime = time;
		this.lastBWSetTime = this.startTime;
		this.flowSize = flowSize;
		//this.curBandwidth = curBandwidth;
		this.mExp = mExp;
		this.links = null;
		this.mRoute = new Route(path);
		this.dataTransmitted = 0.0;
		this.mJob = srcTask.getaJob();
		//creat the flow finished event when transmission is finished
//		double estFinishTime = time + flowSize / curBandwidth;;
//		mFCEvent = new FlowCompletedEvent(estFinishTime, mExp); 
//		mExp.addEvent(mFCEvent);
		this.currentHop = -1;
		this.currentPortId = -1;
		this.lastHop = -1;
		this.lastPortId = -1;
		this.aggLink = null;
	}
	
	/*public Flow(DCNode serverById, DCNode serverById2, Task stask, Task iTask,
			double time, double flowSize2, Experiment mExp2) {
		// TODO Auto-generated constructor stub
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		this.srcTask = srcTask;
		this.dstTask = dstTask;
		this.startTime = time;
		this.lastBWSetTime = this.startTime;
		this.flowSize = flowSize;
		//this.curBandwidth = curBandwidth;
		this.mExp = mExp;
		this.links = null;
		
		this.dataTransmitted = 0.0;
		this.mJob = srcTask.getaJob();
	}*/

	public Job getJob(){
		return mJob;
	}
	
	public Task getSourceTask(){
		return srcTask;
	}
	
	public Task getDestinationTask(){
		return dstTask;
	}
	
	public DCNode getSourceNode(){
		return srcNode;
	}
	
	public DCNode getDestinationNode(){
		return dstNode;
	}
	
	public ArrayList<Link> getPathLinks(){
		return links;
	}
	
	public Route getRoutes() {
		return this.mRoute;
	}
	
	public int getLastNodeId() {
		if (lastHop == -1) {
			System.err.println("last hop is -1");
			return -1;
		} else
			return mRoute.getNodeId(lastHop);

	}

	// the output port of last hop DCNode
	public int getNextHopPortId() {
		return mRoute.getNextPortId();

	}

	public int getNextHopNodeId() {
		return mRoute.getNextNodeId();

	}

	// the output port of last hop DCNode
	public int getLastPortId() {
		return lastPortId;

	}

	public int getCurrentNodeId() {
		return mRoute.getCurrentNodeId();
	}

	public int getCurrentPortId() {
		return currentPortId;
	}
	
	public int getStartPortIndex() {
		if (mRoute == null) {
			System.err.println("no routes generated in this packet\n");
			return -1;

		}

		return mRoute.getStartPortIndex();

	}

	public int getCurrentHop() {
		return currentHop;
	}

	public void forwardCurrentHop() {
		mRoute.forwardCurrentHop();
	}

	public int getDestinationPortId() {
		return mRoute.getDestinationPortId();
	}

	public int getNextNodeId() {
		return mRoute.getNodeId(currentHop + 1);
	}
	
	public boolean isNextHopIngress() {
		return mRoute.isNextHopIngress();
	}

	public boolean isLastHopIngress() {
		return mRoute.isLastHopIngress();
	}
	
	public boolean isZeroHop() {
		// TODO Auto-generated method stub
		return mRoute.getStartPortIndex() == 0 ? true : false;
	}
	
	public boolean getWaitingForLineCardWakeup() {
		return waitingForLineCardWakeup;
	}
	
	public void setWaitingForLineCardWakeup(boolean waitingForLineCardWakeup) {
		this.waitingForLineCardWakeup = waitingForLineCardWakeup;
	}
	
	public void setNextFlowArrival(FlowArriveEvent nextFlowArrival) {
		this.nextFlowArrival = nextFlowArrival;
	}
	
	public void setLastFlowArrival(DestinationReceivedFlowEvent lastFlowArrival) {
		this.lastFlowArrival = lastFlowArrival;
	}

	public Link aggLink;
	public void setAggLink(Link aggLink) {
		this.aggLink = aggLink;
	}

	public Link getAggLink() {
		return aggLink;
	}

	protected class Route {
		private final static int NODE_INDEX = 0;
		private final static int INPORT_INDEX = 1;
		private final static int OUTPORT_INDEX = 2;

		private Vector<Vector<Integer>> vRoutes;

		public Route(Vector<Vector<Integer>> routes) {
			this.vRoutes = routes;
		}

		private int getStartPortIndex() {
			if (vRoutes == null) {
				System.err
						.println("Fatal failure: routes vector not generated for this packet\n");
				return -1;
			}
			return vRoutes.get(0).get(OUTPORT_INDEX);
		}

//		private int getOutportId(int i) {
//			if (vRoutes == null) {
//				System.err
//						.println("Fatal failure: routes vector not generated for this packet\n");
//				return -1;
//			}
//			return vRoutes.get(i).get(OUTPORT_INDEX);
//		}

		private int getNodeId(int index) {
			if (index < 0 || index > vRoutes.size() - 1) {
				System.err.println("index of routes vector id out of range\n");
				return -1;
			} else
				return vRoutes.get(index).get(NODE_INDEX);
		}

		private int getDestinationPortId() {
			if (vRoutes == null) {
				System.err
						.println("Fatal failure: routes vector not generated for this packet\n");
				return -1;
			}
			return vRoutes.get(vRoutes.size() - 1).get(INPORT_INDEX);

		}

		private int getNextPortId() {
			if (currentPortId == vRoutes.get(currentHop).get(INPORT_INDEX))
				return vRoutes.get(currentHop).get(OUTPORT_INDEX);
			else
				return vRoutes.get(currentHop + 1).get(INPORT_INDEX);

		}

		private boolean isNextHopIngress() {
			if (currentHop == -1)
				return false;

			if (currentPortId == vRoutes.get(currentHop).get(OUTPORT_INDEX))
				return true;
			return false;
		}

		private boolean isLastHopIngress() {
			if (currentHop == -1 || currentHop == 0) {
				System.err.println("incorrect query: isLastHopIngress "
						+ currentHop);
				System.exit(0);
			}
			if (currentPortId == vRoutes.get(currentHop).get(OUTPORT_INDEX))
				return true;
			else
				return false;

		}

		private int getNextNodeId() {
			if (currentPortId == vRoutes.get(currentHop).get(INPORT_INDEX))
				return vRoutes.get(currentHop).get(NODE_INDEX);
			else
				return vRoutes.get(currentHop + 1).get(NODE_INDEX);

		}

		private int getCurrentNodeId() {
			return vRoutes.get(currentHop).get(NODE_INDEX);
		}

		private void forwardCurrentHop() {
			if (currentHop == -1) {
				currentHop = 0;
				currentPortId = getStartPortIndex();
			} else {
				Vector<Integer> nodeInOut = vRoutes.get(currentHop);
				int inPortid = nodeInOut.get(INPORT_INDEX);
				int outPortid = nodeInOut.get(OUTPORT_INDEX);
				lastHop = currentHop;
				lastPortId = currentPortId;

				if (currentPortId == inPortid) {
					lastPortId = currentPortId;
					currentPortId = outPortid;

				}

				else {

					currentHop++;
					currentPortId = vRoutes.get(currentHop).get(INPORT_INDEX);
				}

			}

		}
	}

}
