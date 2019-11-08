package event;
//import communication.Packet;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.Port;
import infrastructure.LineCard;
//import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LPIBaseExperiment;
import infrastructure.LCSwitch;

public class PortEnterActiveEvent extends AbstractEvent{

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LPIBaseExperiment lpiBaseExp;
	private DCNode mNode;
	private int portId;
	private int linecardId;
	public PortEnterActiveEvent(final double time, final Experiment experiment,
			final DCNode node, final int portId, final int linecardId ) {
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
	}
	@Override
	public void process() {
		verbose();
		Port port = mNode.getPortById(portId);
		port.exitLPI(getTime());
		if (mNode.getNodeTypeName()=="switch") {
			// Calculate # of linecards/ports in each state and set corresponding variables
			((LCSwitch)mNode).calculateLineCardStates();
			((LCSwitch)mNode).calculatePortStates();
		}
	}
	@Override
	public void printEventInfo() {	
		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of " + mNode.getNodeTypeName() 
//				+ " " + mNode.getNodeId() + " enters Active State.");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of linecard " + linecardId + " in " + mNode.getNodeTypeName() 
//				+ " " + mNode.getNodeId() + " enters Active State.");
//			}
			
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}
}
