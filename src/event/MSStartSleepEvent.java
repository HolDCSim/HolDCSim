package event;

import infrastructure.ERoverStates;
import infrastructure.EnergyServer;
import java.util.Vector;

import debug.Sim;

import experiment.Experiment;
import experiment.ERoverExperiment;
import experiment.SingletonJobExperiment.IdleRecord;
import topology.Topology;

public class MSStartSleepEvent extends StartSleepEvent {

	protected ERoverExperiment energyAwareExp = null;
	//private MultiStateCore msCore = null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an Event representing a CPU core entering core parking.
	 * 
	 * @param time
	 *            - the time at which the core enters the park state.
	 * @param experiment
	 *            - the experiment of the event.
	 * @param aCore
	 *            - the core which is entering core parking.
	 */
	public MSStartSleepEvent(final double time,
			final Experiment experiment, final EnergyServer server, int sleepState) {
		super(time, experiment, server, sleepState);
		this.sleepState = sleepState;
		this.energyAwareExp = (ERoverExperiment) experiment;
	}

	@Override
	public void printEventInfo() {
		Sim.debug(5,"Time :" + this.getTime()
				+ " *** server enter low power state " + sleepState
				+ " server : " + sleepable.getNodeId());

	}
	
	@Override
	public void process() {

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

				// if the core is about to enter s4, update the number of s4
				// servers
				if (sleepState == ERoverStates.DEEP_SLEEP) {
					verbose();
					energyAwareExp.currentNumOfS4Servers++;

					double nextWaitingTime = sleepable
							.getNextWaitingTime(sleepState);
					StartSleepEvent nextSleepEvent = new MSStartSleepEvent(time
							+ nextWaitingTime, this.experiment, mServer,
							nextState);
					Sim.debug(3, "@@@ time: " + (time + nextWaitingTime)
							+ " next low powerstate " + nextState);
					sleepable.setNextSleepEvent(nextSleepEvent, sleepState);
					experiment.addEvent(nextSleepEvent);

					Sim.debug(5,"-- MSStartSleepEvent -- server : " + sleepable.getNodeId() + " going to sleep "+ experiment.printStateofServers());
					Sim.debug(3, "server : " + sleepable.getNodeId()
							+ " entered s4 : current s4 servers : "
							+ energyAwareExp.currentNumOfS4Servers
							+ " s4 provision: "
							+ energyAwareExp.numOfMinS4Servers);
				}

			}

			else {
				// if current number of s4 servers is less than provision
				// requirements
				if (energyAwareExp.currentNumOfS4Servers > energyAwareExp.numOfMinS4Servers) {
					energyAwareExp.currentNumOfS4Servers--;
					Sim.debug(3, "s4 server : " + sleepable.getNodeId()
							+ " put to s5 : current s4 servers : "
							+ energyAwareExp.currentNumOfS4Servers
							+ " s4 provision: "
							+ energyAwareExp.numOfMinS4Servers);

				} else {
					Sim.debug(1, "s4 server : " + sleepable.getNodeId()
							+ " not put to s5 : current s4 servers : "
							+ energyAwareExp.currentNumOfS4Servers
							+ " s4 provision: "
							+ energyAwareExp.numOfMinS4Servers);

					// if not enter deepest sleep state because of s4 provision,
					// we
					// need to set enterC6S3 to null, otherwise the Scheduler
					// will try
					// to cancel an event that is not in the queue!
					sleepable
							.setNextSleepEvent(null, ERoverStates.DEEP_SLEEP);

					// need to ignore this event
					return;
				}
			}
		}

		else {




			if(sleepState == sleepable.getInitialSleepState()) {
				Vector<IdleRecord> idleDistribution = sleepable
						.getIdleDistribution();
				IdleRecord idleRecord = new IdleRecord();
				idleRecord.startIdleTime = time;
				idleRecord.sleepstate = sleepState;
				idleDistribution.add(idleRecord);
			//	System.out.println("idleDistribution.size() = " + idleDistribution.size());
			}
			if (getExperiment().getExpConfig().getNetworkRoutingAlgorithm() != Topology.NetworkRoutingAlgorithm.WASP) {
				if (sleepState != sleepable.getDeepestSleepState()) {

//				if(sleepable.getServer().getNodeId() == 1) {
//					System.out.println("dfd");
//				}


					int nextState = sleepable.generateNextSleepState(sleepState);

					// if the core is about to enter s4, update the number of s4
					// servers

					verbose();
					printEventInfo();
					energyAwareExp.currentNumOfS4Servers++;

					double nextWaitingTime = sleepable
							.getNextWaitingTime(sleepState);
					StartSleepEvent nextSleepEvent = new MSStartSleepEvent(time
							+ nextWaitingTime, this.experiment, mServer,
							nextState);
					Sim.debug(3, "@@@ time: " + (time + nextWaitingTime)
							+ " next low powerstate " + nextState);
					sleepable.setNextSleepEvent(nextSleepEvent, sleepState);
					experiment.addEvent(nextSleepEvent);

					Sim.debug(5, "-- MSStartSleepEvent -- server : " + sleepable.getNodeId() + " going to next sleep " + experiment.printStateofServers());
					Sim.debug(3, "server : " + sleepable.getNodeId()
							+ " entered s4 : current s4 servers : "
							+ energyAwareExp.currentNumOfS4Servers
							+ " s4 provision: "
							+ energyAwareExp.numOfMinS4Servers);


//			Vector<IdleRecord> idleDistribution = sleepable
//					.getIdleDistribution();
//			IdleRecord idleRecord = new IdleRecord();
//			idleRecord.startIdleTime = time;
//			idleRecord.sleepstate = sleepState;
//			idleDistribution.add(idleRecord);
//			
//			Sim.debug(3, "server : " + sleepable.getNodeId()
//					+ " entered fixed sleep state " + sleepState);

				}
			}
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
