package beans;

public class ServerFreqVolRecord implements Writable{
	private long serverId;
	private double time;
	private double mFreq;
	private double mVolt;

	public ServerFreqVolRecord(long serverId, double time, double freq,
			double volt) {
		this.serverId = serverId;
		this.time = time;
		this.mFreq = freq;
		this.mVolt = volt;
	}
	
	public long getServerId() {
		return serverId;
	}

	public double getTime() {
		return time;
	}

	public double getmFreq() {
		return mFreq;
	}

	public double getmVolt() {
		return mVolt;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}

}
