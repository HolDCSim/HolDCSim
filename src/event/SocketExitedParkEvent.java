package event;

import infrastructure.Socket;
import experiment.Experiment;

public final class SocketExitedParkEvent extends AbstractEvent {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The socket which will be parked.
	 */
	private Socket socket;

	/**
	 * Creates a new SocketExitedParkEvent.
	 * 
	 * @param time
	 *            - The time the socket exits park
	 * @param experiment
	 *            - The experiment the event takes place in
	 * @param theSocket
	 *            - The socket being taken out of park
	 */
	public SocketExitedParkEvent(final double time,
			final Experiment experiment, final Socket theSocket) {
		super(time, experiment);
		this.socket = theSocket;
	}

	/**
	 * Takes the socket out of park.
	 */
	@Override
	public void process() {
		verbose();

		this.socket.exitPark(this.getTime());
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.SERVER_EVENT;
	}
}
