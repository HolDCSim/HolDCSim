package beans;

public class SwitchPowerRecord implements Writable {
	private long switchId;
	private double time;
	private double switchPower;

	public SwitchPowerRecord(long switchId, double time, double switchPower){
		this.switchId = switchId;
		this.time = time;
		this.switchPower = switchPower;
	}

	public long getSwitchId() {
		return switchId;
	}

	public double getTime() {
		return time;
	}

	public double getSwitchPower() {
		return switchPower;
	}

	@Override
	public void writeToPersist() {
		// TODO Auto-generated method stub
		
	}
	
	

}
