
package infrastructure;

import communication.Flow;
import communication.Packet;

import experiment.Experiment;

public class HybridServer extends Server implements Routable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void routePacket(double time, Experiment experiment, Packet packet,
			int portId) {
		// TODO Auto-generated method stub
		Port port = portMap.get(portId);
		if(port == null){
			System.err.println("invalid port number for packet routing\n");
		}
		port.insertPacket(packet, time, false);

	}
	
	public HybridServer(final int theNumberOfSockets, final int theCoresPerSocket,
			final Experiment anExperiment, final DataCenter dataCenter){
		super(theNumberOfSockets, theCoresPerSocket, anExperiment, dataCenter);		
	}

	@Override
	public void routeFlow(double time, Experiment experiment, Flow flow, int portId) {
		// TODO Auto-generated method stub
		
	}

}
