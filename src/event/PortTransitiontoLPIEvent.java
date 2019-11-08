package event;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.Port;
import infrastructure.LineCard;
//import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LPIBaseExperiment;

public class PortTransitiontoLPIEvent extends AbstractEvent{
	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LPIBaseExperiment lpiBaseExp;
	private DCNode mNode;
	private int portId;
	private int linecardId;
	
	public PortTransitiontoLPIEvent(final double time, final Experiment experiment,
			final DCNode node, final int portId, final int linecardId){
		super(time, experiment);
		this.mNode = node;
		this.portId = portId;
		this.linecardId = linecardId;
	}
	
	@Override
	public void process() {
		verbose();
		
		Port port = mNode.getPortById(portId);
		port.TransitiontoLPI(time);
		port.setNextLPIevent(null);
		
		double ts = 0.0;
		ts = Port.sleepStateWakeups[1];
		PortEnterLPIEvent enterLPIEvent = new PortEnterLPIEvent(
				time + ts, mNode.getDataCenter().experiment,
				mNode, portId, linecardId);
		mNode.getDataCenter().experiment.addEvent(enterLPIEvent);
//		if (mNode.getNodeTypeName() == "switch") {
//			LineCard linecard = mNode.getLinecardById(linecardId);
//			int i;
//			for (i = linecardId*experiment.getExpConfig().getPortsPerLineCard()+1; i < (linecardId+1)*experiment.getExpConfig().getPortsPerLineCard(); i++) {
//				Port portwithin = mNode.getPortById(i);
//				if (portwithin.getPortState()==Port.PortPowerState.LOW_POWER_IDLE || portwithin.getPortState()==Port.PortPowerState.TRANSITION_TO_LOW_POWER_IDLE) {
//					continue;
//				} else {
//					break;
//				}
//			}
//			if (i >= (linecardId+1)* experiment.getExpConfig().getPortsPerLineCard()) {
//				double tslc = 0.0;
//				tslc = LineCard.sleepStateWakeups[1];
//				linecard.TransitiontoSleep(time);
//				linecard.setNextLinecardSleepEvent(null);
//				LineCardEnterSleepEvent enterSleepEvent = new LineCardEnterSleepEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//				mNode.getDataCenter().experiment.addEvent(enterSleepEvent);
//			}
//		}
	}
	
	@Override
	public void printEventInfo() {
		
		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "server") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of " + mNode.getNodeTypeName() 
//				+ " " + mNode.getNodeId() + " starts transitioning to LPI.");
//			} else if (mNode.getNodeTypeName() == "switch") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of linecard " + linecardId + " in " + mNode.getNodeTypeName() 
//				+ " " + mNode.getNodeId() + " starts transitioning to LPI.");
//			}
			
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}
}
