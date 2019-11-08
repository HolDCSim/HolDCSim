package event;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.Port;
import infrastructure.LineCard;
import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LPIBaseExperiment;

public class PortWakeupEvent extends AbstractEvent{

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LPIBaseExperiment lpiBaseExp;
	private DCNode mNode;
	private int portId;
	private int linecardId;
	
	public PortWakeupEvent(final double time, final Experiment experiment,
			final DCNode node, final int portId, final int linecardId ){
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
	}


	@Override
	public void process() {
		verbose();
		Port port = mNode.getPortById(portId);
		port.TransitiontoActive();
		port.setLPIendTime(time);
		port.updatePortLPITime();
		port.setLPIstartTime(time);
		port.setNextWakeUpevent(null);
		
		//linecard.setNextWakeUpevent(null);
		
		//double tw = 0.0;
		double tw = 0.1;
		tw = Port.sleepStateWakeups[1];
		PortEnterActiveEvent enterAEvent = new PortEnterActiveEvent(
				time + tw, mNode.getDataCenter().experiment,
				mNode, portId, linecardId);
		mNode.getDataCenter().experiment.addEvent(enterAEvent);
		
	}
	
	@Override
	public void printEventInfo() {
		Port port = mNode.getPortById(portId);
		if (mNode != null && (port.getLPIstartTime()<time)) {
//			if (mNode.getNodeTypeName() == "server") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of " + mNode.getNodeTypeName() 
//				+ " " + mNode.getNodeId() + " begins waking up.");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of linecard " + linecardId + " in " + mNode.getNodeTypeName() + " "
//				+ mNode.getNodeId() + " begins waking up.");
//			}
			
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}
}
