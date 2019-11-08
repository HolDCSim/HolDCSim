package event;

import experiment.Experiment;

public class LineCardSleepEvent extends AbstractEvent{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LineCardSleepEvent(double theTime, Experiment anExperiment) {
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
		return EVENT_TYPE.LINECARD_EVENT;
	}

}
