package beans;

public class PortPowerRecord implements Writable {
	
	private long portId;
	private long nodeId;
	private double time;
	private double portPower;
	
	public PortPowerRecord(long portId, long nodeId, double time, double portPower){
		this.portId = portId;
		this.nodeId = nodeId;
		this.time = time;
		this.portPower = portPower;
	}

	public long getPortId() {
		return portId;
	}

	public long getNodeId() {
		return nodeId;
	}

	public double getTime() {
		return time;
	}

	public double getPortPower() {
		return portPower;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}
	
	

}
