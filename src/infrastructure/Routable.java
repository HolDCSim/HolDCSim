package infrastructure;

import communication.Flow;
import communication.Packet;
import communication.Flow;
import experiment.Experiment;

/**
 * @author Fan Yao
 * interface for nodes that could perform routing
 * could be both servers or switches
 */
public interface  Routable {
	
	public abstract void routePacket(final double time, final Experiment experiment,
			Packet packet, final int portId);
	
	public abstract void routeFlow(final double time, final Experiment experiment,
			Flow flow, final int portId);

}
