package event;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.LCSwitch;
import infrastructure.Port;
import infrastructure.SwitchSleepController;
import infrastructure.LineCard;
import constants.Constants;
//import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;;

public class PortEnterLPIEvent extends AbstractEvent{
	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LinecardNetworkExperiment lcNetworkExp;
	private DCNode mNode;
	private int portId;
	private int linecardId;
	private boolean lcChange = false;
	
	public PortEnterLPIEvent(final double time, final Experiment experiment,
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
		port.enterLPI(getTime());
		port.setLPIstartTime(time);
		if (mNode.getNodeTypeName() == "switch") {
			LineCard linecard = mNode.getLinecardById(linecardId);
			int i=1;
			int portsPerLineCard = experiment.getExpConfig().getPortsPerLineCard();
			for (i = (linecardId-1) * portsPerLineCard + 1; i < mNode.getDataCenter().mTopo.getSwitchPortNum(mNode.getNodeId()); i++) {
				Port portwithin = mNode.getPortById(i);
				if (portwithin.getPortState()==Port.PortPowerState.LOW_POWER_IDLE) {
					continue;
				} else {
					break;
				}
			}
			
			// If all ports in line card are in LPI, line card transitions to SLEEP
			if(mNode.allPortsLPI(linecardId)) {
				LCSwitch lcSwitch = (LCSwitch)mNode;
				lcSwitch.unsetScheduledActive();
				SwitchSleepController switchSleepController = linecard.getSleepController();
				
				if(switchSleepController.getTransitionEvent() == null) {
					LineCardTransitiontoSleepEvent transitiontosleepevent = new LineCardTransitiontoSleepEvent(time, mNode.getDataCenter().experiment, mNode, linecardId, switchSleepController);
					mNode.getDataCenter().experiment.addEvent(transitiontosleepevent);
					switchSleepController.setTransitionEvent(transitiontosleepevent);
				}
			}
		}
	}
	
	@Override
	public void printEventInfo() {
		if (mNode != null) {
//			if (mNode.getNodeTypeName() == "switch") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of linecard " + linecardId + " in " + mNode.getNodeTypeName() + " "
//				+ mNode.getNodeId() + " enters LPI mode.");
//			} else if (mNode.getNodeTypeName() == "server") {
//				Sim.debug(3, "Time: " + this.getTime() + ", [PORT_EVENT], port " + portId + " of " + mNode.getNodeTypeName() + " "
//						+ mNode.getNodeId() + " enters LPI mode.");
//			}
		}	
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}
}
