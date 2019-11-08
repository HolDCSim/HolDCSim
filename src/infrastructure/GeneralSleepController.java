package infrastructure;

import infrastructure.Core.PowerState;
import utility.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import constants.Constants;

import debug.Sim;

import event.StartSleepEvent;
import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * @author fanyao
 * 
 */
public class GeneralSleepController extends AbstractSleepController {


	public GeneralSleepController(Experiment experiment) {
		super(experiment);
		deepiestState = 5;
		// initialize sleep state event map
		/*******************************************************/
		stateToEventsMap = new HashMap<Integer, StartSleepEvent>();
		stateToEventsMap.put(1, enterC0S0);
		stateToEventsMap.put(2, enterC1S0);
		stateToEventsMap.put(3, enterC3S0);
		stateToEventsMap.put(4, enterC6S0);
		stateToEventsMap.put(5, enterC6S3);
		/*******************************************************/

		// TODO fix the magic numbers
		// fanyao modified: sleep states considered
		// initial states, core is active
		// set idle based on sleepscale model

		/*******************************************************/
	}
	


	/**
	 * ideally this function should model a state transition graph
	 * 
	 * @param currentState
	 *            , the current sleep state
	 * @return next sleep state
	 */
	public int generateNextSleepState(int currentState){
		//
		if (currentState == deepiestState) {
			Sim.fatalError("it is already the deepest sleep state");
		}
		return ++currentState;
	}


	/**
	 * subclass should implement this method for custom sleep state transitions
	 * 
	 * @param nextSleepEvent
	 * @param currentSS
	 */
	public void setNextSleepEvent(StartSleepEvent nextSleepEvent, int currentSS) {

		// TODO Auto-generated method stub
		switch (currentSS) {
		case 1:
			enterC1S0 = nextSleepEvent;
			break;
		case 2:
			enterC3S0 = nextSleepEvent;
			break;
		case 3:
			enterC6S0 = nextSleepEvent;
			break;
		case 4:
			enterC6S3 = nextSleepEvent;
			break;
		default:
			System.err.println("setting wrong sleep states");
			System.exit(0);
		}

	}

	public StartSleepEvent getNextSleepEvent() {
		StartSleepEvent event = null;

		switch (currentSleepState) {
		case 1:
			event = enterC1S0;
			break;
		case 2:
			event = enterC3S0;
			break;
		case 3:
			event = enterC6S0;
			break;
		case 4:
			event = enterC6S3;
			break;
		default:
			System.err.println("setting wrong sleep states");
			System.exit(0);
		}
		return event;

	}

	@Override
	public void cancelAllFutureSleepEvents(Experiment mExperiment) {

	}


	/**
	 * @param theSleepState
	 *            set the current sleepstate
	 */
	public void setCurrentSleepState(int theSleepState) {
		// TODO Auto-generated method stub
		// this.fixedSleepState = theSleepState;
		Pair<Double, Integer> pair = new Pair<Double,Integer>();
		pair.setFirst(mExp.getCurrentTime());
		pair.setSecond(theSleepState);
		this.getServer().SSTrace.add(pair);
		this.currentSleepState = theSleepState;
		if (currentSleepState == 0) {
			transitionToActiveTime = 0.0;
			return;
		}

		this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];

	}

	/**
	 * initial sleep state when simulation starts
	 * 
	 * @return
	 */
	public int getInitialSleepState() {
		return 1;
	}
	


	/**
	 * default behavior collects sleep state durations for all the states
	 * predefined
	 * 
	 * @param sleepState
	 * @param time
	 */
	public void accumulateSSTimes(int sleepState, double time) {
		// TODO Auto-generated method stub
		this.updateWakeupSplits(sleepState);
		if (useMultipleState) {

			C0S0Count++;
			if (sleepState > 1) {
				C1S0Count++;
				C0S0Time += getTimeBetweenStates(1);
			} else {
				C0S0Time += time - sleepStateWaitings[sleepState - 1]
						- sleepStateWakeups[sleepState - 1];
				return;

			}

			if (sleepState > 2) {
				C3S0Count++;
				C1S0Time += getTimeBetweenStates(2);
			} else {
				C1S0Time += time - sleepStateWaitings[sleepState - 1]
						- sleepStateWakeups[sleepState - 1];
				return;

			}

			if (sleepState > 3) {
				C6S0Count++;
				C3S0Time += getTimeBetweenStates(3);
			} else {
				C3S0Time += time - sleepStateWaitings[sleepState - 1]
						- sleepStateWakeups[sleepState - 1];
				return;

			}

			if (sleepState > 4) {
				C6S3Count++;
				C6S0Time += getTimeBetweenStates(4);
				C6S3Time += time - sleepStateWaitings[sleepState - 1]
						- sleepStateWakeups[sleepState - 1];

			} else {
				C6S0Time += time - sleepStateWaitings[sleepState - 1]
						- sleepStateWakeups[sleepState - 1];

			}
		}

		// fixed sleepstate
		else {

			double duration = time - sleepStateWakeups[sleepState - 1];
			switch (sleepState) {
			case 1:
				C0S0Count++;
				C0S0Time += duration;
				break;
			case 2:
				C1S0Count++;
				C1S0Time += duration;
				break;
			case 3:
				C3S0Count++;
				C3S0Time += duration;
				break;
			case 4:
				C6S0Count++;
				C6S0Time += duration;
				break;
			case 5:
				C6S3Count++;
				C6S3Time += duration;
				break;

			default:
				Sim.fatalError("unsupported sleep state " + currentSleepState);
			}

		}

	}
	
	public Vector<IdleRecord> getIdleDistributions() {
		return idleDistribution;
	}	
	
	/**
	 * should be called after genPerf function
	 * 
	 * @return
	 */
	public Map<Integer, Double> getStateDurations() {
		Map<Integer, Double> stateDurations = new HashMap<Integer, Double>();

		stateDurations.put(0, this.serviceTime);
		stateDurations.put(1, C0S0Time);
		stateDurations.put(2, C1S0Time);
		stateDurations.put(3, C3S0Time);
		stateDurations.put(4, C6S0Time);
		stateDurations.put(5, C6S3Time);
		// -1 denotes wakeup
		stateDurations.put(-1, this.wakeupTime);
		return stateDurations;
	}
	
	@Override
	public void prepareStats(){
		this.activeTime = this.serviceTime + this.wakeupTime;
		this.idleTime = mExp.getCurrentTime() - this.activeTime;
		this.collectedWakeupTime = C0S0Wakeup + C1S0Wakeup + C3S0Wakeup + C6S0Wakeup + C6S3Wakeup;
		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
				+ C6S3Time;
		this.jobProcessingTime = activeTime - getWakeupTime();
	}
	
	/**
	 * check whether the time stats for each states are collected correctly by
	 * compare three stats: tempIdle time, expected Idle time, accumulated idle
	 * time in idleDistributions
	 * 
	 * @return
	 */
	public  boolean timingCheck(){
		if (Math.abs(collectedIdleTime - idleTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, expected idle != collected Idle for server : "
					+ this.getNodeId());
			return false;
		}
		
		if (Math.abs(collectedWakeupTime - wakeupTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, collected wakeup time != wakeupTime");
			return false;
		}

		if (useMultipleState) {
			Sim.warning("no distribution check in for cores with multiple states");
			return true;
		}

		// might be a FakeRecordVector, ignore distribution check
		if (idleDistribution.size() <= 1) {
			Sim.debug(3, "FakeRecordVector used, ignore distribution check");
			return true;
		}

		// distribution check
		double collectedC0S0Time = 0.0;
		double collectedC1S0Time = 0.0;
		double collectedC3S0Time = 0.0;
		double collectedC6S0Time = 0.0;
		double collectedC6S3Time = 0.0;

		for (IdleRecord record : idleDistribution) {
			switch (record.sleepstate) {
			case 1:
				collectedC0S0Time += record.duration;
				break;
			case 2:
				collectedC1S0Time += record.duration;
				break;
			case 3:
				collectedC3S0Time += record.duration;
				break;
			case 4:
				collectedC6S0Time += record.duration;
				break;
			case 5:
				collectedC6S3Time += record.duration;
				break;
			default:
				Sim.fatalError("unknown sleep state when performing timing check");
			}
		}

		if (Math.abs(collectedC0S0Time - C0S0Time) > Constants.TIMING_RRECISION
				|| Math.abs(collectedC1S0Time - C1S0Time) > Constants.TIMING_RRECISION
				|| Math.abs(collectedC3S0Time - C3S0Time) > Constants.TIMING_RRECISION
				|| Math.abs(collectedC6S0Time - C6S0Time) > Constants.TIMING_RRECISION
				|| Math.abs(collectedC6S3Time - C6S3Time) > Constants.TIMING_RRECISION) {
			Sim.warning("collected sleep state distribution incorrect");
			return false;
		}

		else
			return true;

	}
	
//	public abstract void setActive();
//	
//	public abstract boolean isActive();
//	
//	public abstract boolean isTransitionToActive();
//	
//	public abstract void setTransitionToActive();

}
