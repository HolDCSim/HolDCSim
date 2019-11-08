package event;
import debug.Sim;
import infrastructure.DCNode;
import infrastructure.LCSwitch;
import infrastructure.LineCard;
import infrastructure.SwitchSleepController;
import infrastructure.LineCard.LineCardState;
//import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.LPIBaseExperiment;

public class LineCardEnterSleepEvent extends AbstractEvent{
	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	protected LPIBaseExperiment lpiBaseExp;
	private DCNode mNode;
	private int linecardId;
	
	public LineCardEnterSleepEvent(final double time, final Experiment experiment,
			final DCNode node, final int linecardId ){
		super(time, experiment);
		this.mNode = node;
		this.linecardId = linecardId;
	}
	@Override
	public void process() {
		verbose();
		
		// Line card goes to next sleep state
		LineCard linecard = mNode.getLinecardById(linecardId);
		SwitchSleepController switchSleepController = linecard.getSleepController();
		int currentState = switchSleepController.getCurrentSleepState();
		int nextState = switchSleepController.generateNextSleepState(currentState);
		
		if(nextState == switchSleepController.getDeepestSleepState()) {	
			// Line card enters OFF state
			linecard.exitSleep(getTime());
			linecard.enterOff(getTime());
			switchSleepController.setCurrentSleepState(nextState);
		}
		else {
			// Line card enters deeper SLEEP state
			if(currentState == 0) {
				linecard.enterSleep(getTime());
			}
			
			switchSleepController.setCurrentSleepState(nextState);
			
			if(switchSleepController.getTransitionEvent() == null) {
				LineCardTransitiontoSleepEvent lineCardTransitiontoSleepEvent = new LineCardTransitiontoSleepEvent(time, mNode.getDataCenter().experiment, mNode, linecardId, switchSleepController);
				this.getExperiment().addEvent(lineCardTransitiontoSleepEvent);
				switchSleepController.setTransitionEvent(lineCardTransitiontoSleepEvent);
			}
		}
	}
	
	@Override
	public void printEventInfo() {
		
		if (mNode != null) {
			Sim.debug(3, "Time: " + this.getTime() + ", [LINECARD_EVENT], linecard " + linecardId + " of " + mNode.getNodeTypeName() + " "
					+ mNode.getNodeId() + " enters sleep mode.");
		}
	}
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.LINECARD_EVENT;
	}
}
