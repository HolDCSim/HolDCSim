package event;

import controller.WindowedSSEnforcer;
import experiment.Experiment;

public class RecalculateSSEevent extends AbstractEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	WindowedSSEnforcer ssEnforcer;

	public RecalculateSSEevent(double theTime, Experiment anExperiment,
			WindowedSSEnforcer ssEnforcer) {
		super(theTime, anExperiment);
		this.ssEnforcer = ssEnforcer;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void printEventInfo() {
		System.out.println("$$$ Time :" + time + " frequency set to 1.0");
	}

	@Override
	public void process() {
		verbose();
		System.out.println("recalculate frequency and sleepscale");
		// TODO Auto-generated method stub
		this.ssEnforcer.enforceSleepScale(this.time);
	}

}
