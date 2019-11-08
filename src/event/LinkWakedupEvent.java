package event;

import infrastructure.AbstractLinkSleepController;
import experiment.Experiment;


public class LinkWakedupEvent extends AbstractEvent{

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	private AbstractLinkSleepController sleepable;

	/**
	 * @prevSleepState, which sleep state is the link waking up from
	 */
	private int lastSleepState;

	/**
	 * Constructs a LinkExitedLPIEvent.
	 * 
	 * @param time
	 *            - The time the link exits LPI.
	 * @param experiment
	 *            - The experiment in which the event occurs.
	 * @param aLink
	 *            - The link which is exiting LPI.
	 */
	public LinkWakedupEvent(final double time, final Experiment experiment,
			final AbstractLinkSleepController sleepable, int prevSleepState) {
		super(time, experiment);
		this.sleepable = sleepable;
		this.lastSleepState = prevSleepState;
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.PORT_EVENT;
	}

	@Override
	public void printEventInfo() {
		System.out.println("Time :" + this.getTime() + " *** link exit low power idle state " 
				+ lastSleepState + " link : " + sleepable.getLinkId());
	}

	/**
	 * Takes the link out of LPI.
	 */
	@Override
	public void process() {
		verbose();

		// set wakeup time statistics for single server sleepscale experiment
		/***
		 * // fanyao added: calcuate wakeup times
		 */

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

		//this.activityUnit.wakedupFromSleep(this.time);
	}

}
