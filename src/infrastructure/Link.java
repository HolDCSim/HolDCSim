package infrastructure;

import java.util.*;

import javax.swing.text.html.HTMLEditorKit.LinkController;

import utility.Pair;
import communication.Flow;
import debug.Sim;

/**
 * @author fanyao
 * Link class models a basic link between source and destination
 * source and destination could be either switch or servers
 */
/**
 * @author fan
 * 
 */
public class Link {

	private static int linkIDCounter = 0;

	// unique id for each link
	private int linkID;
	public DCNode srcNode;
	public DCNode dstNode;

	private int srcNodeIndex;
	private int dstNodeIndex;
	private Port srtPort;
	private Port dstPort;

	/* link rate in Mbps */
	public  double linkRate;

	// current capacity of this link
	public double currentCap = linkRate;

	// public int numOfUnallocFlows = 0;

	// current active flows through this link
	private Vector<Flow> currentFlows;
	public Vector<Flow> allocatedFlows;
	public Vector<Flow> unAllocatedFlows;
	private int numOfFlows;
	
	public Vector<Pair<Double, Double>> UtilTrace;

	public void prepareAllocation() {
		if (allocatedFlows == null)
			allocatedFlows = new Vector<Flow>();
		else
			allocatedFlows.clear();// remove all old elements as flows have to
									// be rescheduled again

		unAllocatedFlows = new Vector<Flow>(currentFlows);
		currentCap = linkRate;
	}

	private static int getUniqueID() {
		linkIDCounter++;
		return linkIDCounter;
	}

	@Override
	public int hashCode() {
		return linkID;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Link other = (Link) obj;
		if (other.linkID == this.linkID) {
			return true;
		} else
			return false;
	}

	public Link(DCNode source, DCNode destination, int srcNodeIndex,
			int dstNodeIndex, Port sourcePort, Port destinationPort) {
		this.srcNode = source;
		this.dstNode = destination;
		this.srcNodeIndex = srcNodeIndex;
		this.dstNodeIndex = dstNodeIndex;
		this.srtPort = sourcePort;
		this.dstPort = destinationPort;
		this.linkID = getUniqueID();

		currentFlows = new Vector<Flow>();
		numOfFlows = 0;

		this.UtilTrace = new Vector<Pair<Double,Double>>();
		

		// Link rate is lowest bw of 2 connected switches
		if(srcNode instanceof LCSwitch && dstNode instanceof LCSwitch) {
			if(((LCSwitch)srcNode).getType() == LCSwitch.Type.EDGE || ((LCSwitch)dstNode).getType() == LCSwitch.Type.EDGE) {
				linkRate = srcNode.mDataCenter.getExpConfig().getEdgeSwitchBW();
			}
			else if(((LCSwitch)srcNode).getType() == LCSwitch.Type.AGGREGATE || ((LCSwitch)dstNode).getType() == LCSwitch.Type.AGGREGATE) {
				linkRate = srcNode.mDataCenter.getExpConfig().getAggregateSwitchBW();
			}
			else {
				if(((LCSwitch)srcNode).getType() != LCSwitch.Type.CORE || ((LCSwitch)dstNode).getType() != LCSwitch.Type.CORE) {
					Sim.fatalError("Expect both src and dst nodes to be core switches");
				}
				
				linkRate = srcNode.mDataCenter.getExpConfig().getCoreSwitchBW();
			}
		}
		// If switch is connected to server, link rate is bw of edge switch
		else {
			if(srcNode instanceof LCSwitch) {
				if(((LCSwitch)srcNode).getType() != LCSwitch.Type.EDGE) {
					Sim.fatalError("Expect src node to be edge switch");
				}
			}
			else if(dstNode instanceof LCSwitch) {
				if(((LCSwitch)dstNode).getType() != LCSwitch.Type.EDGE) {
					Sim.fatalError("Expect dst node to be edge switch");
				}
			}
			else {
				Sim.fatalError("Expect either src or dst node to be LCSwitch");
			}
			
			linkRate = srcNode.mDataCenter.getExpConfig().getEdgeSwitchBW();
		}
	}

	/**
	 * @param oneFlow
	 * @return the adjusted bandwidth for each flow passing this link this may
	 *         not be the final since the final bandwidth has to be determined
	 *         by multiple links along the flow path
	 */
	public void addFlow(Flow oneFlow) {
		this.currentFlows.add(oneFlow);
		numOfFlows++;
		// can be extended for more sophiscated flow schedulig algorithms
		// return linkRate / numOfFlows;
	}

	public void removeFlow(Flow oneFlow) {
		boolean removed = this.currentFlows.remove(oneFlow);

		// the flow should be successfully removed
		if (!removed) {
			Sim.fatalError("the flow to be removed does not exist on link: "
					+ this.linkID);
		}
		numOfFlows--;
		// return linkRate / numOfFlows;
	}

	public void setCurrentCap(double cap) {
		this.currentCap = cap;
	}

	public double getCurrentCap() {
		return this.currentCap;
	}

	public int getNumOfFlows() {
		return currentFlows.size();
	}

	public Vector<Flow> getCurrentFlows() {
		return this.currentFlows;
	}

	public int getSrcNodeIndex() {
		return srcNodeIndex;
	}

	public int getDstNodeIndex() {
		return dstNodeIndex;
	}

}
