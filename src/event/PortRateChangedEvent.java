package event;

import experiment.Experiment;

public class PortRateChangedEvent extends AbstractEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PortRateChangedEvent(double theTime, Experiment anExperiment) {
		super(theTime, anExperiment);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process() {
		verbose();
		// TODO Auto-generated method stub

	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}

}
