package event;

import experiment.ERoverExperiment;
import experiment.Experiment;
import experiment.LinecardNetworkExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

import java.util.Vector;

import debug.Sim;
import infrastructure.LineCard;
import infrastructure.SwitchSleepController;
import infrastructure.DCNode;
import infrastructure.ERoverStates;
import infrastructure.LCSwitch;

public class LineCardTransitiontoSleepEvent extends AbstractEvent{
	private static final long serialVersionUID = 1L;
	
	protected LinecardNetworkExperiment linecardNetworkExp = null;
	private DCNode mNode;
	//private int portId;
	private int linecardId;
	//private static int PORT_PER_LINECARD = 8;
	
	protected SwitchSleepController sleepable;
	protected int sleepState;

	public LineCardTransitiontoSleepEvent(final double time, final Experiment experiment,
			final DCNode node, final int linecardId, final SwitchSleepController switchSleepController) {
		super(time, experiment);
		this.mNode = node;
		this.linecardId = linecardId;
		this.sleepable = switchSleepController;
	}

	@Override
	public void process() {
		verbose();
		//experiment.cu
		this.sleepState = sleepable.getCurrentSleepState();
		
		if(sleepState < sleepable.getDeepestSleepState() && sleepable.getNextSleepEvent() == null) {
			double waitingTime = experiment.getExpConfig().getLcWaitTime(sleepState);
			
			if(sleepState < sleepable.getDeepestSleepState()) {
				// Enter deeper sleep state
				if (Double.compare(sleepable.getLastEnterSleepStateTime(), 0.0) == 0) {
					sleepable.setLastEnterSleepStateTime(time);
				}

				if (sleepable.doUseMultipleSleepStates()) {
					LineCardEnterSleepEvent nextSleepEvent = new LineCardEnterSleepEvent(time + waitingTime, mNode.getDataCenter().experiment, mNode, linecardId);
					experiment.addEvent(nextSleepEvent);
					sleepable.setNextSleepEvent(nextSleepEvent, sleepState);
				}
			}
			else {
				Sim.fatalError("Invalid line card state");
			}	
		}
		
		sleepable.setTransitionEvent(null);
	}
	
	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.LINECARD_EVENT;
	}
	
	@Override
	public void printEventInfo() {
		if (mNode != null) {
			Sim.debug(3, "Time: " + this.getTime() + ", [LINECARD_EVENT], linecard " + linecardId + " of " + mNode.getNodeTypeName() + " "
					+ mNode.getNodeId() + " starts transitioning to Sleep State.");
		}
	}

}
