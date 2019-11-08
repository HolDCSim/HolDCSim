package communication;


import infrastructure.*;

import java.util.*;

import job.Job;
import job.Task;

public class Packet {

	/**
	 * size of a packet
	 */
	public static final int MAX_SIZE = 1 * 1024;
	/**
	 * store the route information of this message format [switchID,
	 * in_port_number, out_port_nunber] ...
	 */
	private Route mRoute;
	public static int packet_size = MAX_SIZE;
	private Task _sourceTask;
	private Task _destinationTask;
	private DCNode sourceNode, destinationNode;
	//private DataCenter dataCenter;
	public Task tSource, tDestination;
	//private double generateTime;
	//private double arriveTime;
	private int currentHop;
	private int currentPortId;

	private int lastHop;
	private int lastPortId;
	private int packetId;

	public Packet(DCNode _sourceNode, DCNode _destinationNode,
			Task _sourceTask, Task _destinationTask,
			Vector<Vector<Integer>> path, DataCenter _dataCenter, int packetId) {
		this.sourceNode = _sourceNode;
		this.destinationNode = _destinationNode;
		//this.dataCenter = _dataCenter;
		this._sourceTask = _sourceTask;
		this._destinationTask = _destinationTask;
		this.currentHop = -1;
		this.currentPortId = -1;
		this.lastHop = -1;
		this.lastPortId = -1;
		this.packetId = packetId;
		// printing out source and destination server for debugging purpose
		// System.err.println("Source Server " + sourceServer.getNodeID() +
		// " Destination server " + destinationServer.getNodeID());
		this.mRoute = new Route(path);
		// System.out.println(dataCenter.mTopology.getSinglePathRoute(
		// sourceNode.getNodeId(), destinationNode.getNodeId()));

	}

	public Route getRoutes() {
		return this.mRoute;
	}

	public int getPacketId() {
		return this.packetId;
	}

	public void setPacketId(int packetId) {
		this.packetId = packetId;
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

	public int getPacketSize() {
		return packet_size;
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

	public static void setPacketSize(int size) {
		packet_size = size;
	}

	public Task get_sourceTask() {
		return _sourceTask;
	}

	public void set_sourceTask(Task _sourceTask) {
		this._sourceTask = _sourceTask;
	}

	public Task get_destinationTask() {
		return _destinationTask;
	}

	public void set_destinationTask(Task _destinationTask) {
		this._destinationTask = _destinationTask;
	}

	public long get_jobid() {
		return _sourceTask.getJobId();

	}
	
	public Job getJob(){
		return _sourceTask.getaJob();
	}

	public boolean isNextHopIngress() {
		return mRoute.isNextHopIngress();
	}

	public boolean isLastHopIngress() {
		return mRoute.isLastHopIngress();
	}

	public DCNode getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(DCNode sourceServer) {
		this.sourceNode = sourceServer;
	}

	public DCNode getDestinationNode() {
		return destinationNode;
	}

	public void setDestinationNode(Server destinationServer) {
		this.destinationNode = destinationServer;
	}

	public boolean isZeroHop() {
		// TODO Auto-generated method stub
		return mRoute.getStartPortIndex() == 0 ? true : false;
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
