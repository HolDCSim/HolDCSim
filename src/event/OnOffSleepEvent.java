package event;

import java.util.Vector;

import debug.Sim;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.Core.PowerState;
import infrastructure.EnergyServer;
import infrastructure.GeneralSleepController;
import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * @author fan CoreStartSleepEvent for delay off (DO) core
 */
public class OnOffSleepEvent extends StartSleepEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	public OnOffSleepEvent(double time, Experiment experiment, EnergyServer server,
			int sleepState) {
		super(time, experiment, server, sleepState);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void printEventInfo() {
		System.out.println("Time :" + this.getTime()
				+ " *** OnOffCore enter low power state " + sleepState
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
		/*****************************************************/

		// if sleepstate is 1 or server is turned back off directly to 6
		/*
		 * both OnOffCore and DelayOffCore share the same event. For OnOffCore,
		 * sleepstate 6 event following sleepstate 1 would not be generated
		 * since OnOffCore would only be in sleepstate 1 at the beginning
		 */
		if (sleepState == 1 || ((EnergyServer)sleepable.getServer()).isTransitionToActive()) {

			// update idle record
			Vector<IdleRecord> idleDistribution = sleepable.getIdleDistribution();
			IdleRecord idleRecord = new IdleRecord();
			idleRecord.startIdleTime = time;
			idleRecord.sleepstate = sleepState;
			idleDistribution.add(idleRecord);
		}

			/*
			 * for OnOffCore, there would not be CoreStartSleepEvent with
			 * sleepstate 1, use this to distinguish from DelayOffCore
			 */

	   if (sleepable.doUseMultipleSleepStates() && sleepState != sleepable.getDeepestSleepState()) {
				
				// generate event to turn off server
				//double delayOff = ((DelayOffExperiment) experiment).getNextDelayTime();
				double delayOff = sleepable.getNextWaitingTime(sleepState);
				int nextState = sleepable.generateNextSleepState(sleepState);
				OnOffSleepEvent nextSleepEvent = new OnOffSleepEvent(
						time + delayOff, this.experiment, mServer, nextState);
				Sim.debug(3, "@@@ time: " + (time + delayOff)
						+ " next low powerstate : " + nextState + " server : "+ sleepable.getNodeId());
				sleepable.setNextSleepEvent(nextSleepEvent, sleepState);
				experiment.addEvent(nextSleepEvent);
		}
		
		/****************************************************/

		/*should use activityunit not sleepable*/
		this.activityUnit.startSleep(this.time, sleepState);
		
	}

}
