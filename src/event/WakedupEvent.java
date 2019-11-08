package event;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.IActivityUnit;
import infrastructure.SwitchSleepController;
import experiment.Experiment;

/**
 * Represents a core leaving parking.
 * 
 * @author David Meisner (meisner@umich.edu)
 */
public final class WakedupEvent extends AbstractEvent {

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The core to take out of park.
	 */
	private AbstractSleepController sleepable;
	
	// TODO: Use this variable
	private SwitchSleepController switchSleepable;

	/**
	 * @prevSleepState, which sleep state is the core waking up from
	 */
	private int lastSleepState;
	
	private IActivityUnit activityUnit;

	/**
	 * Constructs a CoreExitedParkEvent.
	 * 
	 * @param time
	 *            - The time the core exits parking.
	 * @param experiment
	 *            - The experiment in which the event occurs.
	 * @param aCore
	 *            - The core which is exiting parking.
	 */
	public WakedupEvent(final double time, final Experiment experiment,
			final AbstractSleepController sleepable, final IActivityUnit activityUnit, int prevSleepState) {
		super(time, experiment);
		this.sleepable = sleepable;
		this.activityUnit = activityUnit;
		this.lastSleepState = prevSleepState;
	}

	public WakedupEvent(final double time, final Experiment experiment,
			final SwitchSleepController sleepable, final IActivityUnit activityUnit, int prevSleepState) {
		super(time, experiment);
		this.switchSleepable = switchSleepable;
		this.activityUnit = activityUnit;
		this.lastSleepState = prevSleepState;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.SERVER_EVENT;
	}

	@Override
	public void printEventInfo() {
		System.out.println("Time :" + this.getTime()
				+ " *** core exit low power state " + lastSleepState
				+ " server : " + sleepable.getNodeId());

	}

	/**
	 * Takes the core out of park.
	 */
	@Override
	public void process() {
		verbose();

		// set wakeup time statistics for single server sleepscale experiment
		/***
		 * // fanyao added: calcuate wakeup times
		 */
		// this.core.setWakeupTime(core.getWakeupTime() +
		// core.getTransitionToActiveTime());
		// ssExp.setWakeupTime(ssExp.getWakeupTime() +
		// core.getTransitionToActiveTime());

		// set wakeup time statistics for cores

		sleepable.setWakeupTime(sleepable.getWakeupTime()
				+ sleepable.getTransitionToActiveTime());

		// fanyao added: for multiple sleepstates
		/************************************************************************/
		// int sleepState = core.getCurrentSleepState();
		sleepable.accumulateSSTimes(lastSleepState,
				time - sleepable.getLastEnterSleepStateTime());

		sleepable.setLastEnterSleepStateTime(0.0);
		/************************************************************************/

		this.activityUnit.wakedupFromSleep(this.time);
	}

}
