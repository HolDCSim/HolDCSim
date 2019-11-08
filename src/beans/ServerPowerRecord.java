package beans;

public class ServerPowerRecord implements Writable{
	private long serverId;
	private double time;
	private double serverPower;

	public ServerPowerRecord(long serverId, double time, double serverPower) {
		this.serverId = serverId;
		this.time = time;
		this.serverPower = serverPower;
	}

	public long getServerId() {
		return serverId;
	}

	public void setServerId(long serverId) {
		this.serverId = serverId;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getServerPower() {
		return serverPower;
	}

	public void setServerPower(double serverPower) {
		this.serverPower = serverPower;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}

}
