package event;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.EnergyServer;
import infrastructure.GeneralSleepController;
import infrastructure.IActivityUnit;
import infrastructure.LCSwitch;

import java.util.Vector;

import debug.Sim;

import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;

public class StartSleepEvent extends AbstractEvent {

	/**
	 * The serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The core which will be put into park.
	 */
	protected AbstractSleepController sleepable;

	protected IActivityUnit activityUnit;
	
	protected EnergyServer mServer;
	
	protected int sleepState;


	/**
	 * Constructs an Event representing a CPU core entering core parking.
	 * 
	 * @param time
	 *            - the time at which the core enters the park state.
	 * @param experiment
	 *            - the experiment of the event.
	 * @param sleepable
	 *            - the core which is entering core parking.
	 */
	public StartSleepEvent(final double time, final Experiment experiment,
			final EnergyServer server, int sleepState) {
		super(time, experiment);
		this.sleepable = server.getSleepController();
		this.activityUnit = server.getActivityUnit();
		this.mServer = server;
		this.sleepState = sleepState;
	}

	// TODO: Implement constructor
	public StartSleepEvent(final double time, final Experiment experiment,
			LCSwitch lcSwitch, int theSleepState) {
		super(time, experiment);
		// TODO Auto-generated constructor stub
	}

	@Override
	public EVENT_TYPE getEventType() {
		return EVENT_TYPE.SERVER_EVENT;
	}

	@Override
	public void printEventInfo() {
		System.out.println("Time :" + this.getTime()
				+ " *** core enter low power state " + sleepState
				+ " server : " + sleepable.getNodeId());

	}

	/**
	 * Puts the core into park.
	 */
	@Override
	public void process() {
		verbose();

		// fanyao added
		// multiple sleep states may be entered, this should only be set once
		// to ensure this, we compare this with 0.0
		if (Double.compare(sleepable.getLastEnterSleepStateTime(), 0.0) == 0) {
			sleepable.setLastEnterSleepStateTime(time);
		}

		// accumulate idle distribution statistics for single sleepstate
		// experiment

		// SleepScaleExperiment ssExp = (SleepScaleExperiment) experiment;

		// fanyao added: enter next low power state
		/*****************************************************/
		// if (ssExp.isUseMultipleSS()) {
		if (sleepable.doUseMultipleSleepStates()) {

			// idle distribution does not distinguish multiple sleep states
			// if (sleepState == 1) {

			/*
			 * use getInitialSleepState() now because the start sleep state of a
			 * core with multiple sleep state may not be 1
			 */
			if (sleepState == sleepable.getInitialSleepState()) {
				Vector<IdleRecord> idleDistribution = sleepable
						.getIdleDistribution();
				IdleRecord idleRecord = new IdleRecord();
				idleRecord.startIdleTime = time;
				idleRecord.sleepstate = sleepState;
				idleDistribution.add(idleRecord);
			}

			if (sleepState != sleepable.getDeepestSleepState()) {

				int nextState = sleepable.generateNextSleepState(sleepState);
				double nextWaitingTime = sleepable.getNextWaitingTime(sleepState);
				StartSleepEvent nextSleepEvent = new StartSleepEvent(
						time + nextWaitingTime, this.experiment, mServer,
						nextState);
				Sim.debug(3, "@@@ time: " + (time + nextWaitingTime)
						+ " next low powerstate " + nextState);
				sleepable.setNextSleepEvent(nextSleepEvent, sleepState);
				experiment.addEvent(nextSleepEvent);

			}
		}

		else {

			Vector<IdleRecord> idleDistribution = sleepable.getIdleDistribution();
			IdleRecord idleRecord = new IdleRecord();
			idleRecord.startIdleTime = time;
			idleRecord.sleepstate = sleepState;
			idleDistribution.add(idleRecord);

		}
		/****************************************************/

		/* core.setCurrentSleepState(sleepState); */
		this.activityUnit.startSleep(this.time, sleepState);
	}

	// public int getSleepState(){
	// return this.sleepState;
	// }

	public void setSleepState(int deepState) {
		// TODO Auto-generated method stub
		this.sleepState = deepState;
	}

}
