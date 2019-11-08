package processor;





abstract class BaseCorePState {

}



/**
 * @author Fan Yao Base package p state all processor should support C0, C1
 *         state
 */
public class CorePState {
	/*
	 * Frequency of the package
	 */
	float frequency;
	float voltage;
	
	float peakFrequency;
	float peakVoltage;
	String mPStateName;
	float dPowerLevel;
	
	public void setFrenAndVol(float freq, float vol){
		frequency = freq;
		voltage = vol;
	}
	public float getFrequency() {
		return frequency;
	}
	public void setFrequency(float frequency) {
		this.frequency = frequency;
	}
	public float getVoltage() {
		return voltage;
	}
	public void setVoltage(float voltage) {
		this.voltage = voltage;
	}
	public String getmPStateName() {
		return mPStateName;
	}
	public void setmPStateName(String mPStateName) {
		this.mPStateName = mPStateName;
	}
	public float getdPowerLevel() {
		return dPowerLevel;
	}
	public void setdPowerLevel(float dPowerLevel) {
		this.dPowerLevel = dPowerLevel;
	}
	public float getPeakFrequency() {
		// TODO Auto-generated method stub
		return this.peakFrequency;
	}
	public float getPeakVoltage() {
		// TODO Auto-generated method stub
		return this.peakVoltage;
	}
}