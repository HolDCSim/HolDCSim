package event;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.LCSwitch;
import infrastructure.LineCard;
import infrastructure.LineCard.LineCardState;
import infrastructure.Port;
import infrastructure.SwitchSleepController;
import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;

public class LineCardWakeupEvent extends AbstractEvent{

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LinecardNetworkExperiment lcNetworkExp;
	private DCNode mNode;
	private int linecardId;
	
	public LineCardWakeupEvent(final double time, final Experiment experiment,
			final DCNode node, final int linecardId ){
		super(time, experiment);
		this.mNode = node;
		this.linecardId = linecardId;
	}


	@Override
	public void process() {
		verbose();
//		LineCard linecard = mNode.getLinecardById(linecardId);
//		linecard.TransitiontoActive();
//		linecard.setSleepEndTime(time);
//		linecard.updateLinecardSleepTime();
//		linecard.setSleepStartTime(time);
//		linecard.setNextWakeUpevent(null);
//		double tw = 0.0;
//		tw = LineCard.sleepStateWakeups[1];
//		LineCardEnterActiveEvent enterAEvent = new LineCardEnterActiveEvent(
//				time + tw, mNode.getDataCenter().experiment,
//				mNode, linecardId);
////		double twPort = 0.0;
////		twPort = Port.sleepStateWakeups[1];
////		for (int i = 0; i < linecard.getportNo(); i++) {
////			Port port = mNode.getPortById(i+1);
////			port.TransitiontoActive();
////			PortEnterActiveEvent enterAEventPort = new PortEnterActiveEvent (time+twPort, mNode.getDataCenter().experiment, mNode, i+1, linecardId);
////			mNode.getDataCenter().experiment.addEvent(enterAEventPort);
////		}
//		mNode.getDataCenter().experiment.addEvent(enterAEvent);
		
		
		
		
		
		
		
		
		LCSwitch lcSwitch = ((LCSwitch)mNode);
		SwitchSleepController switchSleepController = lcSwitch.getSwitchSleepController();
		double wakeupTransitionTime = switchSleepController.getTransitionToActiveTime();
		LineCardEnterActiveEvent lineCardEnterActiveEvent = new LineCardEnterActiveEvent(
				time + wakeupTransitionTime, mNode.getDataCenter().experiment,
				mNode, linecardId);
		mNode.getDataCenter().experiment.addEvent(lineCardEnterActiveEvent);
		
		LineCard lineCard = mNode.getLinecardById(linecardId);
		lineCard.setLinecardstate(LineCardState.TRANSITION_TO_ACTIVE);
	}
	
	@Override
	public void printEventInfo() {
		LineCard linecard = mNode.getLinecardById(linecardId);
		if (mNode != null && (linecard.getSleepStartTime()<time)) {
			Sim.debug(3, "Time: " + this.getTime() + ", [LINECARD_EVENT], linecard " + linecardId + " of "  + mNode.getNodeTypeName() + " "
					+ mNode.getNodeId() + " begins waking up. All its ports can be waked up in the future.");
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.LINECARD_EVENT;
	}
}
