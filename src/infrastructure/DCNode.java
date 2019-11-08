package infrastructure;

import java.util.*;

import constants.Constants;


/**
 * @author Fan Yao
 * 
 *         An abstraction of all nodes in datacenter including network node and
 *         server node
 */
public abstract class DCNode {
	protected Map<Integer, Port> portMap;
	protected Map<Integer, LineCard> linecardMap;
	protected int nodeId;
	protected int matrixNodeId;
	
	protected DataCenter mDataCenter;

	/**
	 * node id always begins with 1, no matter what the data structure is 
	 * in nodeconnectivity matrix
	 * @return
	 */
	public int getNodeId() {
		return nodeId;
	}

//	public void setMatrixNodeId(int matrixNodeId){
//		this.matrixNodeId = matrixNodeId;
//	}
//	
//	public int getMatrixNodeId(){
//		return this.matrixNodeId;
//	}
	 
	public void addPortMap(Map<Integer, Port> _portMap) {
		this.portMap = _portMap;
	}
	
	public Map<Integer, Port> getPortMap() {
		return this.portMap;
	}
	
	public void addLinecardMap(Map<Integer, LineCard> _linecardMap) {
		this.linecardMap = _linecardMap;
	}
	public DataCenter getDataCenter() {
		return this.mDataCenter;
	}

	public void setIds(int nodeId, int matrixNodeId) {
		this.nodeId = nodeId;	// servers: 1 - 16, switches: 1 - 20
		this.matrixNodeId = matrixNodeId;	// servers: 21 - 36, switches: 1 - 20
	}

	public DCNode(DataCenter dataCenter) {
		mDataCenter = dataCenter;
	}

	public Port getPortById(int portId){
		//System.out.println("portId"+portId);
		//System.out.println("nodeId"+nodeId);
		//System.out.println("portmap"+portMap);
		return portMap.get(portId);
	}
	
	public LineCard getLinecardById(int linecardId){
		return linecardMap.get(linecardId);
	}
	
    public String getNodeTypeName(){
    	return mDataCenter.getNodeTypeName(matrixNodeId);
    }

	public int getIdByPort(Port _port) {
		if (_port == null) {
			System.err.println("could not get port by id: port is null\n");
			return -1;
		}

		Set<Integer> keys = portMap.keySet();
		for (int aKey : keys) {
			// FIXME: here we are comparing java object reference, might be
			// dangerous
			if (portMap.get(aKey) == _port)
				return aKey;
		}

		return -1;

	}
	
	public int getIdByLinecard(LineCard _linecard) {
		if (_linecard == null) {
			System.err.println("could not get linecard by id: port is null\n");
			return -1;
		}

		Set<Integer> keys = linecardMap.keySet();
		for (int aKey : keys) {
			// FIXME: here we are comparing java object reference, might be
			// dangerous
			if (linecardMap.get(aKey) == _linecard)
				return aKey;
		}

		return -1;

	}

	public String nodeName = null;

	public abstract Constants.NodeState queryPowerState();
	
	/*
	 * Return true if all ports in line card are in LPI state, false otherwise
	 */
	public boolean allPortsLPI(int lineCardId) {
		int portsPerLineCard = mDataCenter.getExpConfig().getPortsPerLineCard();
		int endIndex = lineCardId * portsPerLineCard;
		int startIndex = endIndex - portsPerLineCard + 1;
		
		for(int i = startIndex; i <= endIndex; i++) {
			if(portMap.get(i).getPortState() != Port.PortPowerState.LOW_POWER_IDLE) {
				return false;
			}
		}
		
		return true;
	}
	
	public int getMatrixNodeId() {
		return matrixNodeId;
	}
}
