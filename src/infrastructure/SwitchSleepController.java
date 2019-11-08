package infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import constants.Constants;
import debug.Sim;
import event.LineCardEnterSleepEvent;
import event.LineCardTransitiontoSleepEvent;
import event.StartSleepEvent;
import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;
import infrastructure.LineCard.LineCardPowerPolicy;
import utility.FakeRecordVector;
import utility.Pair;

public class SwitchSleepController {
	// How long it takes to wake from sleep state
	public static double[] sleepStateWakeups = {1e-4, 1e-3, 1e-2, 1e-1, 1};
	
	// events for multiple sleep states
	protected LineCardEnterSleepEvent enterL0 = null;
	protected LineCardEnterSleepEvent enterL1 = null;
	protected LineCardEnterSleepEvent enterL3 = null;
	protected LineCardEnterSleepEvent enterL5 = null;
	protected LineCardEnterSleepEvent enterL6 = null;

	/************************************************************/

	/**************************************************************/
	// statistics for multiple sleepstates experiments single switch
	public double L0Time = 0.0;
	public double L1Time = 0.0;
	public double L3Time = 0.0;
	public double L5Time = 0.0;
	public double L6Time = 0.0;
	
	//time spent in wakeup due to sleep in a specific state
	public double L0Wakeup = 0.0;
	public double L1Wakeup = 0.0;
	public double L3Wakeup = 0.0;
	public double L5Wakeup = 0.0;
	public double L6Wakeup = 0.0;

	// Number of times line card transitioned into state
	public int L0Count = 0;
	public int L1Count = 0;
	public int L3Count = 0;
	public int L5Count = 0;
	public int L6Count = 0;
	
	/**************************************************************/
	
	/**
	 * fixed sleepstate level for this sleep controller
	 */
	protected int nextFixedSleepState;

	public int deepestState;

	/**
	 * the current sleep state may be different from fixedSleepState when
	 * multiple sleepsates are used
	 */
	protected int currentSleepState;

	/**
	 * the last sleep state before line card is waked up
	 */
	protected int lastStateBeforeWakeup;

	/**
	 * idle time distribution each period record represent the processor from
	 * beginning of sleep to wakeup this does not distinguish multiple sleep
	 * states
	 */
	protected Vector<IdleRecord> idleDistribution;
	
	// use map to manage association with sleep state and its sleep state event
	protected Map<Integer, StartSleepEvent> stateToEventsMap;

	/**
	 * last time when the core first enter sleep state should be the shallowest
	 * if multiple states are used
	 */
	protected double lastEnterSleepState;
	
	protected double lastEnterActiveState;

	/**
	 * whether this sleepable uses multiple sleep state
	 */
	protected boolean useMultipleState;
	
	public double serviceTime = 0.0;

	public double activeTime = 0.0;

	public double idleTime = 0.0;

	public double jobProcessingTime = 0.0;
	public double flowProcessingTime = 0.0;

	public double wakeupTime = 0.0;
	
	public double collectedIdleTime;
	
	public double collectedWakeupTime = 0.0;
	
	protected double lastIdleTime = Double.MAX_VALUE;
	
	protected Experiment mExp;
	
	protected LCSwitch mSwitch;
	
	protected Map<Integer, Double> stateToSleepTimes;
	
	protected Map<Integer, Integer> stateToSleepCounts;
	
	/**
	 * The transition time of the core to halt. SHOULD BE PARKED?
	 */
	protected double transitionToActiveTime;
	
	// events for single sleep state
	protected StartSleepEvent singleStateSleep = null;
	
	// Can only be 1 transition event per sleep controller
	private LineCardTransitiontoSleepEvent transitionEvent = null;
	
	public SwitchSleepController(Experiment experiment){
		this.mExp = experiment;
		stateToEventsMap = new HashMap<Integer, StartSleepEvent>();

		// Deepest state is OFF state
		//deepestState = sleepStateWakeups.length - 1;

		deepestState = mExp.getExpConfig().getSwitchDeepestSleepState();
		this.currentSleepState = getInitialSleepState();
		this.transitionToActiveTime = sleepStateWakeups[1];
		
		this.initializeSleepStateMaps();
		this.useMultipleState = true;
	}
	
	/**
	 * needs to set the server since later the reference to the container server is useful
	 * @param aServer
	 */
	public void setSwitch(LCSwitch lcSwitch){
		this.mSwitch = lcSwitch;
	}
	
	public void initialController(){
		this.useMultipleState = true;
		this.nextFixedSleepState = -1;
//		this.setCurrentSleepState(2);
		this.lastEnterSleepState = 0.0;
		this.initializeSleepStateMaps();
		
		/*
		 * use vector in master branch use FakeRecordVector in MemOpt branch
		 */
		//idleDistribution = new Vector<IdleRecord>();
		idleDistribution = new FakeRecordVector<IdleRecord>();
		IdleRecord firstRecord = new IdleRecord();
		firstRecord.startIdleTime = 0.0;
		firstRecord.sleepstate = 1;

		idleDistribution.add(firstRecord);
	}
	
	public int getCurrentSleepState() {
		return this.currentSleepState;
	}
	
	// public void setTransitionToActiveTime(final double time) {
	// this.transitionToActiveTime = time;
	// }
	
	/**
	 * @return returns the transition to active time based on current sleep state
	 */
	public double getTransitionToActiveTime() {
		return this.transitionToActiveTime;
	}
	
	public int getFixedSleepState() {
		return nextFixedSleepState;
	}

	public void setUseMultipleSleepState(boolean useMS) {
		this.useMultipleState = useMS;
	}

	public int getDeepestSleepState() {
		return deepestState;
	}

	public double getLastEnterSleepStateTime() {
		return this.lastEnterSleepState;
	}

	public void setLastEnterSleepStateTime(double time) {
		this.lastEnterSleepState = time;
	}
	
	public double getLastEnterActiveTime() {
		return this.lastEnterActiveState;
	}
	
	public void setLastEnterActiveTime(double time) {
		this.lastEnterActiveState = time;
	}

	public boolean doUseMultipleSleepStates() {
		return useMultipleState;
	}
	
	public double getWakeupTime() {
		// TODO Auto-generated method stub
		// if(this.nextSleepState == 6){
		// return RSetupTime;
		// }
		return wakeupTime;
	} 

	/**
	 * 
	 * @param theSleepState
	 *            , the sleepstate next time when the core goes sleep
	 */
	public void setNextFixedSleepState(int theSleepState) {
		this.nextFixedSleepState = theSleepState;
	}

	/**
	 * return the idle distributions of current sleepable unit
	 * 
	 * @return
	 */
	public Vector<IdleRecord> getIdleDistribution() {
		return idleDistribution;
	}
	
	public StartSleepEvent getSingleSleepEvent() {
		return singleStateSleep;
	}

	public void setWakeupTime(double d) {
		// TODO Auto-generated method stub
		wakeupTime = d;

	}
	
	public double getLastIdleTime() {
		return this.lastIdleTime;
	}
	
	public int getStateBeforeWakeup() {
		return lastStateBeforeWakeup;
	} 
	
	public LineCardEnterSleepEvent getNextSleepEventToCancel(){
		LineCardEnterSleepEvent event = null;
		
		if (useMultipleState) {
			
			/*
			 * since server now emit StartSleepEvent at the beginning, no need
			 * to avoid the start time (lastEnterSleepState = 0.0)
			 */
			if (currentSleepState != getDeepestSleepState()) {
			//if (currentSleepState != getLastSleepState() && lastEnterSleepState != 0.0) {
				 event = getNextSleepEvent();
			}
		}
		
		return event;
	}
	
	public LineCardTransitiontoSleepEvent generateLineCardSleepEvent(double time, Experiment experiment, DCNode node, int linecardId, SwitchSleepController switchSleepController) {
		return new LineCardTransitiontoSleepEvent(time, experiment, node, linecardId, switchSleepController);
	}
	
	/**
	 * Puts the core into park mode.
	 * should be called by ActivityUnit 
	 * 
	 * @param time
	 *            - the time this sleepable enters park
	 */
	protected void startSleep(final double time, int sleepState){
		setCurrentSleepState(sleepState);
	}
	
	public int getNodeId(){
		return mSwitch.getNodeId();
	}
	
	public void updateServiceTime(double time){
		this.serviceTime += time;
	}
	
	public LCSwitch getSwitch(){
		return mSwitch;
	}
	
	public void setIdleDistribution(Vector<IdleRecord> idleDistribution) {
		this.idleDistribution = idleDistribution;
	}
	
	protected void initializeSleepStateMaps() {
		this.stateToSleepCounts = new HashMap<Integer, Integer>();
		this.stateToSleepTimes = new HashMap<Integer, Double>();

		for (int i = 0; i < sleepStateWakeups.length; i++) {
			stateToSleepCounts.put(i + 1, 0);
			stateToSleepTimes.put(i + 1, 0.0);
		}

	}
	
	public void wakedupFromSleep(double time){
		// Future sleep event is cancelled when switch wakes up
		// fanyao added: set all the sleepstate events to null
		// this should also reset the reference in stateEventMap
		if(enterL0 != null) {
			mExp.cancelEvent(enterL0);
			enterL0 = null;
		}
		if(enterL1 != null) {
			mExp.cancelEvent(enterL1);
			enterL1 = null;
		}
		if(enterL3 != null) {
			mExp.cancelEvent(enterL3);
			enterL3 = null;
		}
		if(enterL5 != null) {
			mExp.cancelEvent(enterL5);
			enterL5 = null;
		}
		if(enterL6 != null) {
			mExp.cancelEvent(enterL6);
			enterL6 = null;
		}
		
		/***********************************************************/

		singleStateSleep = null;

		/***********************************************************/
		
		this.setCurrentSleepState(0);
		this.setLastEnterActiveTime(time);
	}
	
	/**
	 * subclass should implement this method for custom sleep state transitions
	 * 
	 * @param nextSleepEvent
	 * @param currentSS
	 */
	public void setNextSleepEvent(LineCardEnterSleepEvent nextSleepEvent, int currentSS) {

		// TODO Auto-generated method stub
		switch (currentSS) {
		case 0:
			enterL1 = nextSleepEvent;
			break;
		case 1:
			enterL3 = nextSleepEvent;
			break;
		case 2:
			enterL5 = nextSleepEvent;
			break;
		case 3:
			enterL6 = nextSleepEvent;
			break;
		default:
			System.err.println("setting wrong sleep states");
			System.exit(0);
		}

	}
	
	public void updateWakeupSplits(int currentState){
		double wakeupTime = sleepStateWakeups[currentState -1];
		switch(currentState){
		case 1:
			L0Wakeup += wakeupTime;
			break;
		case 2:
			L1Wakeup += wakeupTime;
			break;
		case 3:
			L3Wakeup += wakeupTime;
			break;
		case 4:
			L5Wakeup += wakeupTime;
			break;
		case 5:
			L6Wakeup += wakeupTime;
			break;
		default:
			Sim.fatalError("unsupported currentstate in updateWakeupSplits " + currentState);
				
		}
	}
	
	public void updateWakeupSplits(int currentState, double delta){
//			if(delta < 0){
//				Sim.fatalError("delta time less than 0.0");
//			}
		switch(currentState){
		case 1:
			L0Wakeup += delta;
			break;
		case 2:
			L1Wakeup += delta;
			break;
		case 3:
			L3Wakeup += delta;
			break;
		case 4:
			L5Wakeup += delta;
			break;
		case 5:
			L6Wakeup += delta;
			break;
		default:
			Sim.fatalError("unsupported currentstate in updateWakeupSplits " + currentState);
				
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////
	/* Dividing line for abstract interfaces*/
	//////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Generate next sleep state event, used in multiple sleep state transition scenario
	 * @return
	 */
	public LineCardEnterSleepEvent getNextSleepEvent() {
		LineCardEnterSleepEvent event = null;

		switch (currentSleepState) {
		case 0:
			event = enterL1;
			break;
		case 1:
			event = enterL3;
			break;
		case 2:
			event = enterL5;
			break;
		case 3:
			event = enterL6;
			break;
		default:
			Sim.fatalError("unexpected sleep state " + currentSleepState);
			break;
		}
		return event;
	}
	
	public void prepareStats() {
		this.activeTime = this.serviceTime + this.wakeupTime;
		this.idleTime = mExp.getCurrentTime() - this.activeTime;
		this.collectedWakeupTime = L0Wakeup + L5Wakeup + L6Wakeup;
		this.collectedIdleTime = L0Time + L1Time + L3Time + L5Time
				+ L6Time;
		this.jobProcessingTime = activeTime - getWakeupTime();
	}
	
	/**
	 * ideally this function should model a state transition graph
	 * 
	 * @param currentState
	 *            , the current sleep state
	 * @return next sleep state
	 */
	public int generateNextSleepState(int currentState) {
		int nextState = -1;
		switch (currentState) {
		case 0:
			nextState = 1;
			break;
		case 1:
			nextState = 2;
			break;
		case 2:
			nextState = 3;
			break;
		case 3:
			nextState = 4;
			break;
		default:
			Sim.fatalError("unexpected currentstate: " + currentState);
			break;

		}
		return nextState;
	}
	
	/**
	 * check whether the time stats for each states are collected correctly by
	 * compare three stats: tempIdle time, expected Idle time, accumulated idle
	 * time in idleDistributions
	 * 
	 * @return
	 */
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

	/**
	 * @param theSleepState
	 *            set the current sleepstate
	 */
	public void setCurrentSleepState(int theSleepState) {
		// Update count, time, wakeup stats
		accumulateSSTimes(theSleepState, mExp.getCurrentTime());
		setLastEnterSleepStateTime(mExp.getCurrentTime());
		
		Pair<Double, Integer> pair = new Pair<Double,Integer>();
		pair.setFirst(mExp.getCurrentTime());
		pair.setSecond(theSleepState);
		LCSwitch swtch = this.getSwitch();
		if(swtch != null) {
			swtch.SSTrace.add(pair);
		}

		this.currentSleepState = theSleepState;
		
		// Set time to transition to active state
		if(currentSleepState == 0) {
			transitionToActiveTime = 0.0;
		}
		else {
			this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];
		}
	}

 

	/**
	 * Initial sleep state of when simulation starts
	 */
	public int getInitialSleepState() {
		if(mExp.getExpConfig().getLineCardPowerPolicy() == LineCardPowerPolicy.LINECARD_SLEEP) {
			// Shallowest sleep state
			return 1;
		}
		else if(mExp.getExpConfig().getLineCardPowerPolicy() == LineCardPowerPolicy.NO_MANAGEMENT) {
			// Active state
			return 0;
		}
		else {
			Sim.fatalError("\"" + mExp.getExpConfig().getLineCardPowerPolicy().toString() + "\" is an unrecognized line card power policy");
		}
		
		return -1;
	}

	/**
	 * Uupda
	 */
	public void accumulateSSTimes(int newSleepState, double time) {
		// Update time spent in old sleep state
		// TODO: Take transition time into account

		int oldSleepState = getCurrentSleepState();
		switch(oldSleepState) {
			case 0:
				L0Time += time - getLastEnterActiveTime();
				break;
			case 1:
				L1Time += time - getLastEnterSleepStateTime();
				
				// Account for wakeup delay if transitioning to active state
				if(newSleepState == 0) {
					L1Time -= transitionToActiveTime;
					L1Wakeup += transitionToActiveTime;
				}
				break;
			case 2:
				L3Time += time - getLastEnterSleepStateTime();
				
				// Account for wakeup delay if transitioning to active state
				if(newSleepState == 0) {
					L3Time -= transitionToActiveTime;
					L3Wakeup += transitionToActiveTime;
				}
				break;
			case 3:
				L5Time += time - getLastEnterSleepStateTime();
				
				// Account for wakeup delay if transitioning to active state
				if(newSleepState == 0) {
					L5Time -= transitionToActiveTime;
					L5Wakeup += transitionToActiveTime;
				}
				break;
			case 4:
				L6Time += time - getLastEnterSleepStateTime();
				
				// Account for wakeup delay if transitioning to active state
				if(newSleepState == 0) {
					L6Time -= transitionToActiveTime;
					L6Wakeup += transitionToActiveTime;
				}
				break;
			default:
				Sim.fatalError("Unexpected sleep state: " + newSleepState);
		}
		
		// Update count of transitions into new sleep state
		switch(newSleepState) {
			case 0:
				L0Count++;
				break;
			case 1:
				L1Count++;
				break;
			case 2:
				L3Count++;
				break;
			case 3:
				L5Count++;
				break;
			case 4:
				L6Count++;
				break;
			default:
				Sim.fatalError("Unexpected sleep state: " + newSleepState);
		}
	}
	
	/**
	 * Calculate time spent in final state before experiment ended
	 */
	public void updateTimeOfFinalState() {
		int currentState = getCurrentSleepState();
		
		switch(currentState) {
			case 0:
				L0Time += mExp.getCurrentTime() - getLastEnterActiveTime();
				break;
			case 1:
				L1Time += mExp.getCurrentTime() - getLastEnterSleepStateTime();
				break;
			case 2:
				L3Time += mExp.getCurrentTime() - getLastEnterSleepStateTime();
				break;
			case 3:
				L5Time += mExp.getCurrentTime() - getLastEnterSleepStateTime();
				break;
			case 4:
				L6Time += mExp.getCurrentTime() - getLastEnterSleepStateTime();
				break;
			default:
				Sim.fatalError("Unexpected sleep state: " + currentState);
		}
	}
	
	public double totalTime() {
		return L0Time + L1Time + L3Time + L5Time + L6Time;
	}
	
	public LineCardTransitiontoSleepEvent getTransitionEvent() {
		return transitionEvent;
	}
	
	public void setTransitionEvent(LineCardTransitiontoSleepEvent transitionEvent) {
		this.transitionEvent = transitionEvent;
	}
}
