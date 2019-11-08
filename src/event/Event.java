

package event;

import experiment.Experiment;

/**
 * An Event is the core class around which the
 * discrete-event simulation is run. All state change
 * should happen by processing these events.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public interface Event {

	public enum EVENT_TYPE{
		ALL_EVENT,
		SWITCH_EVENT,
		SERVER_EVENT,
		JOB_EVENT,
		TASK_EVENT,
		PACKET_EVENT,
		PORT_EVENT,
		FLOW_EVENT,
		LINECARD_EVENT,
		UNKNOWN
	}
	
	
    /**
     * Get the time of the event.
     * @return the time the event occurs
     */
    double getTime();

    /**
     * Get the experiment the event is in.
     * @return The experiment the event is in.
     */
    Experiment getExperiment();

    /**
     * Checks if an event takes place before or after this one.
     * @param otherEvent - the event to compare to this one
     * @return the value of
     * {@link java.lang.Double#compareTo(Double anotherDouble)}
     * comparing the times of the two events
     */
    int compareTo(Event otherEvent);

    /**
     * This function is called when the event occurs.
     * It is overridden by implementing classes to implement
     * what a given event should do.
     */
    void process();
    
    void verbose();
    
    EVENT_TYPE getEventType();

}
