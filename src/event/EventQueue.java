

package event;

import java.io.Serializable;
import java.util.PriorityQueue;

import debug.Sim;

/**
 * The EvenQueue manages events in the discrete event simulation.
 * The events are ordered by when they occur in time, so the
 * head of the queue represents the next event to occur.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class EventQueue implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The queue of events. Events are time ordered in a priority queue.
     */
    private PriorityQueue<Event> queue;

    /**
     * Creates a new EventQueue.
     */
    public EventQueue() {
        this.queue = new PriorityQueue<Event>();
    }

    public void removeEvent(Event event){
    	queue.remove(event);
    }
    /**
     * Get and remove the next event from the queue.
     * @return the next event
     */
    public Event nextEvent() {
        return this.queue.poll();
    }

    /**
     * Add an event to the event queue.
     * This event will now happen sometime in the future.
     * @param event - the event to add
     */
    public void addEvent(final Event event) {
        this.queue.add(event);
    }

    /**
     * Remove an event from the event queue.
     * @param event - the event to remove
     */
    public void cancelEvent(final Event event) {
    	boolean removed = true;
    	
    	if(this.queue.contains(event)) {
    		removed = this.queue.remove(event);
    	}
    	
        if (!removed || this.queue.contains(event)) {
            Sim.fatalError("Tried to remove an event" + "and it failed: "
                    + event.getClass());
        }
    }
   
    
    /**
     * Get the size of the event queue.
     * @return the size of the event queue
     */
    public int size() {
        return this.queue.size();
    }

}
