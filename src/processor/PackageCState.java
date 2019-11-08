package processor;

/**
 * @author Fan Yao
 *
 */
public class  PackageCState {
	
	/*
	 * Name of the C state for this package
	 */
	String mPStateName;
	
	/*
	 * level of this C state
	 */
	int stateLevel;

	public int getStateLevel() {
		return stateLevel;
	}

	public void setStateLevel(int stateLevel) {
		this.stateLevel = stateLevel;
	}
		
}