
package event;

import infrastructure.Socket;
import experiment.Experiment;


public final class SocketEnteredParkEvent extends AbstractEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The socket which enters park.
     */
    private Socket socket;

    /**
     * Creates a new SocketEnteredParkEvent.
     * @param time - The time the socket enters park
     * @param experiment - The experiment the event takes place in
     * @param theSocket - The socket being parked.
     */
    public SocketEnteredParkEvent(final double time,
                                  final Experiment experiment,
                                  final Socket theSocket) {
        super(time, experiment);
        this.socket = theSocket;
    }

    /**
     * Puts the socket in park.
     */
    @Override
    public void process() {
    	verbose();
        this.socket.enterPark(this.time);
    }
    
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.SERVER_EVENT;
	}

}
