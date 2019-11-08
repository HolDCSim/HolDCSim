package processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class IntelPentiumPState extends CorePState {

	private static enum PStateTrans {
		P0_P1, P1_P2, P2_P3, P3_P4, P4_P5
	}

	Map<Integer, Vector<Double>> availablePStates;
	Map<PStateTrans, Double> transitionLatency;


	
	int stateIndex = 0;
	String mPStateName = "P0";
	float dPowerLevel;

	public IntelPentiumPState(Map<Integer, Vector< Double>> availablePStates,Map<PStateTrans, Double> transitionLatency){
		
		if(availablePStates!= null && transitionLatency != null){
			this.availablePStates = availablePStates;
			this.transitionLatency = transitionLatency;
		
		}
		
		
		else{
		Vector< Double> p0 = new Vector<Double>(Arrays.asList(1.6, 1.484));
		Vector< Double> p1 = new Vector<Double>(Arrays.asList(1.4, 1.42));
		Vector< Double> p2 = new Vector<Double>(Arrays.asList(1.2, 1.276));
		Vector< Double> p3 = new Vector<Double>(Arrays.asList(1.0, 1.164));
		Vector< Double> p4 = new Vector<Double>(Arrays.asList(0.8, 1.036));
		Vector< Double> p5 = new Vector<Double>(Arrays.asList(0.6, 0.956));
		availablePStates = new HashMap<Integer, Vector<Double>>();
		availablePStates.put(0, p0);availablePStates.put(1, p1);availablePStates.put(2, p2);
		availablePStates.put(3, p3);availablePStates.put(4, p4);availablePStates.put(5, p5);
		
//		transitionLatency.put(PStateTrans.P0_P1, 0.0);transitionLatency.put(PStateTrans.P1_P2, 0.0);
//		transitionLatency.put(PStateTrans.P2_P3, 0.0);transitionLatency.put(PStateTrans.P3_P4, 0.0);
//		transitionLatency.put(PStateTrans.P4_P5, 0.0);
		}
		
		this.peakFrequency =  (float)1.6;
		this.peakVoltage = (float)1.484;
		
		

		
	}


	public void setCorePstate(String stateName) {

	}
}
