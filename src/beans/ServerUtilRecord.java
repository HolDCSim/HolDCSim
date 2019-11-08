package beans;

public class ServerUtilRecord implements Writable{
	private long serverId;
	private double time;
	private double serverUtil;

	public ServerUtilRecord(long serverId, double time, double serverUtil) {
		this.serverId = serverId;
		this.time = time;
		this.serverUtil = serverUtil;

	}

	public long getServerId() {
		return serverId;
	}

	public double getTime() {
		return time;
	}

	public double getServerUtil() {
		return serverUtil;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}
	
	

}
