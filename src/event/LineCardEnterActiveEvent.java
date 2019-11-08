package event;
//import communication.Packet;
import debug.Sim;
import infrastructure.*;
import infrastructure.LineCard.LineCardState;
//import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;

public class LineCardEnterActiveEvent extends AbstractEvent{

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LinecardNetworkExperiment lcNetworkExp;
	private DCNode mNode;
	private int linecardId;
	public LineCardEnterActiveEvent(final double time, final Experiment experiment,
			final DCNode node, final int linecardId ) {
		super(time, experiment);
		this.mNode = node;
		this.linecardId = linecardId;
	}
	@Override
	public void process() {
		verbose();
		
		LineCard linecard = mNode.getLinecardById(linecardId);
		SwitchSleepController switchSleepController = linecard.getSleepController();
		
		linecard.exitSleep(getTime());
		
		// Wake line card
		switchSleepController.wakedupFromSleep(getExperiment().getCurrentTime());
		
		linecard.setLinecardstate(LineCardState.ACTIVE);
		linecard.setNextWakeUpevent(null);
		System.out.println(linecard.getNextLinecardSleepEvent());

//		if (linecard.getNextLinecardSleepEvent()!= null) {
//			DataCenter. linecard.getNextLinecardSleepEvent()
//		}
		// Transition into deeper SLEEP state after delay
		//LineCardTransitiontoSleepEvent lineCardTransitiontoSleepEvent = new LineCardTransitiontoSleepEvent(time  , mNode.getDataCenter().experiment, mNode, linecardId, switchSleepController);
		//this.getExperiment().addEvent(lineCardTransitiontoSleepEvent);
	}
	@Override
	public void printEventInfo() {	
		if (mNode != null) {
			Sim.debug(3, "Time: " + this.getTime() + ", [LINECARD_EVENT], linecard " + linecardId + " of " + mNode.getNodeTypeName() + " "
					+ mNode.getNodeId() + " enters Active State.");
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.LINECARD_EVENT;
	}
}
