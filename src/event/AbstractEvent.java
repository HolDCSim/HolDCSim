package event;

import java.io.Serializable;

import experiment.Experiment;

/**
 * An Abstract implementation of the Event interface. Provides basic time and
 * experiment methods.
 * 
 * @author David Meisner (meisner@umich.edu)
 */

public abstract class AbstractEvent implements Event, Comparable<Event>,
		Serializable {

	/**
	 * Serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/** The time the event takes place. */
	protected double time;

	/** The experiment the event is associated with. */
	protected Experiment experiment;

	/**
	 * A constructor for subclasses to use.
	 * 
	 * @param theTime
	 *            - The time the event occurs at
	 * @param anExperiment
	 *            - The experiment the event happens in
	 */
	public AbstractEvent(final double theTime, final Experiment anExperiment) {
		this.time = theTime;
		this.experiment = anExperiment;
	}

	/**
	 * Get the time the event occurs.
	 * 
	 * @return the time the event occurs
	 */
	public final double getTime() {
		return this.time;
	}

	public void verbose() {
		// use this as the switch to turn on and off information print outs
		if (experiment.getDataCenter().getExpConfig().isVerbose()) {
			printEventInfo();
		}
	}

	public void printEventInfo() {
          
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.UNKNOWN;
	}

	public final Experiment getExperiment() {
		return this.experiment;
	}

	/**
	 * Checks if an event takes place before or after this one.
	 * 
	 * @param otherEvent
	 *            - the event to compare to this one
	 * @return the value of
	 *         {@link java.lang.Double#compareTo(Double anotherDouble)}
	 *         comparing the times of the two events
	 */
	public final int compareTo(final Event otherEvent) {
		Double thisTime = this.time;
		Double otherTime = new Double(otherEvent.getTime());
		return thisTime.compareTo(otherTime);
	}

}
