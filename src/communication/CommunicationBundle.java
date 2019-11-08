package communication;

import java.util.ArrayList;
import java.util.Vector;

/**
 * This class will describe the status of a bundle of packets to be transmitted
 * between two tasks. At runtime it will update the number of packets
 * transmitted. If flow based communication is used, this would store the amount 
 * of data to transimit instead of individual packets
 */
public class CommunicationBundle {
	//FIXME: list of packets are not required, bookmarking number of packets
	//transmitted is good enough
	/**
	 * stores the list of packets to be transmitted between two tasks.
	 */
	private ArrayList<Packet> listOfPackets;
	private long totalNumOfPackets;
	private long packetsTransmitted;
	private double flowSize;
	Vector<Vector<Integer>> path;
	

	public CommunicationBundle(ArrayList<Packet> _listOfPackets) {
		this.packetsTransmitted = 0;
		if (_listOfPackets == null || _listOfPackets.size() == 0) {
			System.err.println("List of packets not initialized \n");
			System.exit(0);
		}
		this.listOfPackets = _listOfPackets;
		this.totalNumOfPackets = _listOfPackets.size();
		//flowSize is set to default
		flowSize = 0;
	}
	
	//constructor for flow based communication
	public CommunicationBundle(double flowSize, Vector<Vector<Integer>> path){
		this.flowSize = flowSize;
		this.path = path;
	}
	

	/**
	 * Gets the PacketList between two tasks.
	 * 
	 * @return the listOfPackets.
	 */
	public ArrayList<Packet> getPacketsList() {
		return this.listOfPackets;
	}

	public void updateTransmittedCount() {
		this.packetsTransmitted++;
	}

	public Vector<Vector<Integer>> getPath(){
		return this.path;
	}
	
	public double getFlowSize(){
		return this.flowSize;
	}
	/**
	 * A flag to determine whether all the packets have been transmitted or not.
	 */
	public boolean areAllPacketsTransmitted() {
		return this.packetsTransmitted == this.totalNumOfPackets ? true : false;
	}
}