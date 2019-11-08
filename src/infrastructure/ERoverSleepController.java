package infrastructure;

import java.util.HashMap;
import java.util.Map;

import constants.Constants;

import debug.Sim;

import event.MSStartSleepEvent;
import event.StartSleepEvent;
import experiment.Experiment;
import utility.Pair;

public class ERoverSleepController extends AbstractSleepController {
	

	public ERoverSleepController(Experiment experiment) {
		super(experiment);
		deepiestState = ERoverStates.DEEPEST_SLEEP;
		// TODO Auto-generated constructor stub
	}

//	@Override
//	public void setNextSleepEvent(StartSleepEvent nextSleepEvent, int currentSS) {
//		// TODO Auto-generated method stub
//
//	}

	@Override
	public void cancelAllFutureSleepEvents(Experiment mexp) {
		if (enterC1S0!= null) {
			mexp.cancelEvent(enterC1S0);


		}
		if (enterC6S0 != null)
			mexp.cancelEvent(enterC6S0);
		if (enterC6S3 !=null) {
			mexp.cancelEvent(enterC6S3);
		}

	}

	@Override
	public StartSleepEvent getNextSleepEvent() {
		// TODO Auto-generated method stub
		StartSleepEvent event = null;

		switch (currentSleepState) {
		case 0:
		case 1:
			event = enterC1S0;
			break;
		case ERoverStates.DEEP_SLEEP:
			event = enterC6S3;
			break;
//		case ERoverStates.DEEP_SLEEP:
//			event = enterC6S3;
//			break;
		default:
			Sim.fatalError("unexpected sleep state " + currentSleepState);
			break;
//		case 2:
//		case 3:
//		case 5:
//			Sim.fatalError("unexpected sleep state " + currentSleepState);
//			break;

		}
		return event;
	}




	@Override
	public void prepareStats() {
		this.activeTime = this.serviceTime + this.wakeupTime;
		this.idleTime = mExp.getCurrentTime() - this.activeTime;
		this.collectedWakeupTime = C0S0Wakeup + C6S0Wakeup + C6S3Wakeup;
		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
				+ C6S3Time;
		this.jobProcessingTime = activeTime - getWakeupTime();

	}
	
	@Override
	public StartSleepEvent generateSleepEvent(double time, Experiment experiment, int theSleepState){
		return new MSStartSleepEvent(time, experiment, this.getServer(), theSleepState);
	}



	@Override
	public int generateNextSleepState(int currentState) {
		int nextState = -1;
		switch (currentState) {
		case 1:
			nextState = ERoverStates.DEEP_SLEEP;
			break;
		case ERoverStates.DEEP_SLEEP:
			nextState = ERoverStates.DEEPEST_SLEEP;
			break;
		default:
			Sim.fatalError("unexpected currentstate: " + currentState);
			break;

		}
		return nextState;
	}

	@Override
	public boolean timingCheck() {
		if (Math.abs(collectedIdleTime - idleTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, expected idle != collected Idle for server: "
					+ this.getNodeId());
			return false;
		}
		
		if (Math.abs(collectedWakeupTime - wakeupTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, collected wakeup time != wakeupTime");
			return false;
		}
		
		if (Math.abs(collectedWakeupTime - wakeupTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, collectedWakeupTime!= wakeupTime: "
					+ this.getNodeId());
			return false;
		}

		return true;
	}

	@Override
	public void setCurrentSleepState(int theSleepState) {
		// TODO Auto-generated method stub
		// this.fixedSleepState = theSleepState;
		this.currentSleepState = theSleepState;
		Pair<Double, Integer> pair = new Pair<Double,Integer>();
		pair.setFirst(mExp.getCurrentTime());
		pair.setSecond(theSleepState);
		this.getServer().SSTrace.add(pair);
		if (currentSleepState == 0) {
			transitionToActiveTime = 0.0;
			return;
		}

		this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];

	}

	@Override
	public int getInitialSleepState() {
		// TODO Auto-generated method stub
		// hard code function,
		if (!useMultipleState) {
			return 1;
		} else {
			return ERoverStates.DEEP_SLEEP;
		}
	}
	
	@Override
	public int getDeepestSleepState(){
		return ERoverStates.DEEPEST_SLEEP;
	}

	@Override
	public double getNextWaitingTime(int currentSS){
		// TODO Auto-generated method stub
		return mExp.getExpConfig().getServerSleepWaitTime(currentSS); 
		}
	
	@Override
	public void accumulateSSTimes(int sleepState, double time) {
		
		this.updateWakeupSplits(sleepState);
		// TODO Auto-generated method stub
		/*
		 * for MultiStateCore we know whether it is using multiple state or not
		 * by looking at the current sleeps state. we do not
		 * useMultipleSleepState here before it might be changed by
		 * EnergyAwareScheduler
		 */
		if (sleepState != 1) {

			C6S0Count++;
			if (sleepState > ERoverStates.DEEP_SLEEP) {
				C6S3Count++;
				C6S0Time += AbstractSleepController.getTimeBetweenStates(ERoverStates.DEEP_SLEEP);
				C6S3Time += time - AbstractSleepController.getTimeBetweenStates(ERoverStates.DEEP_SLEEP)
						- AbstractSleepController.sleepStateWakeups[sleepState - 1];
			} else {
				/*
				 * C6S0Time += time - Core.sleepStateWaitings[sleepState - 1] -
				 * Core.sleepStateWakeups[sleepState - 1];
				 */
				C6S0Time += time - AbstractSleepController.sleepStateWakeups[sleepState - 1];
				return;
			}
		}

		// fixed sleepstate = 1
		else {

			double duration = time - AbstractSleepController.sleepStateWakeups[sleepState - 1];

			C0S0Count++;
			C0S0Time += duration;
		}

	}

	@Override
	public void initializeSleepStateMaps(){
		this.stateToSleepCounts = new HashMap<Integer, Integer>();
		this.stateToSleepTimes = new HashMap<Integer, Double>();

		stateToSleepCounts.put(1, 0);
		stateToSleepCounts.put(ERoverStates.DEEP_SLEEP, 0);
		stateToSleepCounts.put(ERoverStates.DEEPEST_SLEEP, 0);

		stateToSleepTimes.put(1, 0.0);
		stateToSleepTimes.put(ERoverStates.DEEP_SLEEP, 0.0);
		stateToSleepTimes.put(ERoverStates.DEEPEST_SLEEP, 0.0);
	}
	
	@Override
	public Map<Integer, Double> getStateDurations() {
		Map<Integer, Double> stateDurations = new HashMap<Integer, Double>();

		stateDurations.put(0, this.serviceTime);
		stateDurations.put(1, C0S0Time);
		stateDurations.put(ERoverStates.DEEP_SLEEP, C6S0Time);
		stateDurations.put(ERoverStates.DEEPEST_SLEEP, C6S3Time);

		// -1 denotes wakeup
		stateDurations.put(-1, this.wakeupTime);
		return stateDurations;
	}
	

}
