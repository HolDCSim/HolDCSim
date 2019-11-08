package infrastructure;

import java.util.*;

import debug.Sim;

import utility.FakeRecordVector;

import event.StartSleepEvent;
import event.WakedupEvent;
import experiment.Experiment;
import experiment.ExperimentConfig;
import experiment.SingletonJobExperiment.IdleRecord;

public  abstract class AbstractSleepController implements ISleepController {
	
	public static double[] sleepStateWakeups = new double[5]; // = { 0.0, 1e-5, 1e-4, 1e-3, 1.0 };
	// public static double[] sleepStateWakeups = { 0.0, 1e-5, 1e-4, 1e-3, 10.0
	// };
 //public static double[] sleepStateWaitings = { 0.0, 1e-3, 1e-3, 1e-1,1
	// };

	/* one premium setting based on previous multiple sleep states experiments */
	//public static double[] sleepStateWaitings = { 0.0, 0.0, 0.0, 0.0, 0.5,1 };
	//public static double[] sleepStateWaitings = { 0.0, 5, 50, 200, 2000,1000 };
//	public static double[] sleepStateWaitings; // = { 0.0, 0.0, 0.0, 0.0, 0.5,100 };
	public static double[] sleepStateWaitings  = { 0.0, 100e-6,100e-3, 100e-3, 100e-3,1 };
	//public static double[] sleepStateWaitings = { 0.0, 5e-5, 5e-4, 5e-3, 0.5,100 };
	
	/*************************************************************/
	// events for multiple sleep states
	protected StartSleepEvent enterC0S0 = null;
	protected StartSleepEvent enterC1S0 = null;
	protected StartSleepEvent enterC3S0 = null;
	protected StartSleepEvent enterC6S0 = null;
	protected StartSleepEvent enterC6S3 = null;

	/************************************************************/

	/**************************************************************/
	// statistics for multiple sleepstates experiments single server
	public double C0S0Time = 0.0;
	public double C1S0Time = 0.0;
	public double C3S0Time = 0.0;
	public double C6S0Time = 0.0;
	public double C6S3Time = 0.0;
	
	//time spend in wakeup due to sleep in a specific state
	public double C0S0Wakeup = 0.0;
	public double C1S0Wakeup = 0.0;
	public double C3S0Wakeup = 0.0;
	public double C6S0Wakeup = 0.0;
	public double C6S3Wakeup = 0.0;

	public int C0S0Count = 0;
	public int C1S0Count = 0;
	public int C3S0Count = 0;
	public int C6S0Count = 0;
	public int C6S3Count = 0;

	// use map to manage association with sleep state and its sleep state time
	// && counts


	/**************************************************************/
	
	/**
	 * fixed sleepstate level for this sleep controller
	 */
	protected int nextFixedSleepState;

	public int deepiestState;

	/**
	 * the current sleep state may be different from fixedSleepState when
	 * multiple sleepsates are used
	 */
	protected int currentSleepState;

	/**
	 * the last sleep state before core is waked up
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

	/**
	 * whether this sleepable uses multiple sleep state
	 */
	protected boolean useMultipleState;
	
	public double serviceTime = 0.0;

	public double activeTime = 0.0;

	public double idleTime = 0.0;

	public double jobProcessingTime = 0.0;

	public double wakeupTime = 0.0;
	
	public double collectedIdleTime;
	
	public double collectedWakeupTime = 0.0;
	
	protected double lastIdleTime = Double.MAX_VALUE;
	
	protected Experiment mExp;
	
	protected EnergyServer mServer;
	
	protected Map<Integer, Double> stateToSleepTimes;
	
	protected Map<Integer, Integer> stateToSleepCounts;
	
	/**
	 * The transition time of the core to halt. SHOULD BE PARKED?
	 */
	protected double transitionToActiveTime;
	
	// events for single sleep state
	protected StartSleepEvent singleStateSleep = null;
	
	
	public AbstractSleepController(Experiment experiment){
		this.mExp = experiment;
		ExperimentConfig mExpConfig = mExp.getExperimentConfig();
		stateToEventsMap = new HashMap<Integer, StartSleepEvent>();
		//subclass needs to define its own deepiest sleep state
		deepiestState = -1;
		//this.mServer = aServer;
		
		sleepStateWakeups[0] = mExpConfig.getWakeupTime(1);
		sleepStateWakeups[1] = mExpConfig.getWakeupTime(2);
		sleepStateWakeups[2] = mExpConfig.getWakeupTime(3);
		sleepStateWakeups[3] = mExpConfig.getWakeupTime(4);
		sleepStateWakeups[4] = mExpConfig.getWakeupTime(5);
		
		sleepStateWaitings[0] = mExpConfig.getWaitingTime(0);
		sleepStateWaitings[1] = mExpConfig.getWaitingTime(1);
		sleepStateWaitings[2] = mExpConfig.getWaitingTime(2);
		sleepStateWaitings[3] = mExpConfig.getWaitingTime(3);
		sleepStateWaitings[4] = mExpConfig.getWaitingTime(4);
		sleepStateWaitings[5] = mExpConfig.getWaitingTime(5);
		
	}
	
	/**
	 * needs to set the server since later the reference to the container server is useful
	 * @param aServer
	 */
	public void setServer(EnergyServer aServer){
		this.mServer = aServer;
	}
	public static void setSleepStateWatings(double[] taos) {
		if (sleepStateWaitings.length == taos.length) {
			sleepStateWaitings = taos;
		} else {
			System.err.println("number of sleep state waiting incorrect");
			System.exit(0);
		}
	}
	
	public static double getTimeBetweenStates(int initialState) {
		return sleepStateWaitings[initialState]
				- sleepStateWaitings[initialState - 1];
	}
	
	public void initialController(){
		this.useMultipleState = false;
		this.nextFixedSleepState = -1;
		this.setCurrentSleepState(1);
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
		// TODO Auto-generated method stub
		return deepiestState;
	}

	public double getLastEnterSleepStateTime() {
		return this.lastEnterSleepState;
	}

	public void setLastEnterSleepStateTime(double time) {
		this.lastEnterSleepState = time;
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
	
	public StartSleepEvent getNextSleepEventToCancel(){
		StartSleepEvent event = null;
		
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
	
	
	/**
	 * controller waked up because of inserttask
	 * @param time, the time when sleepcontroller begins to be waked up
	 */
	public void prepareControllerWakeup(double time){
//		double exitTime = 0.0;
//		// consider On/off experiment
//		// if (currentSleepState != 6)
//		exitTime = time + transitionToActiveTime;
//		// else
//		// exitTime = time + this.rSetupTime;
//		WakedupEvent coreWakedupEvent = new WakedupEvent(exitTime,
//				this.mExp, this, mServer.getActivityUnit(), getCurrentSleepState());
//		this.mExp.addEvent(coreWakedupEvent);

		/************************************************************************/
		// fanyao added: accumulate idle distrubtion statistics, windowed
		// SleepScaleExperiment ssExp = (SleepScaleExperiment) experiment;
		if (idleDistribution.size() == 0) {
			IdleRecord firstRecord = new IdleRecord();
			firstRecord.startIdleTime = 0.0;
			idleDistribution.add(firstRecord);
		}
		IdleRecord lastRecord = idleDistribution.lastElement();
		lastRecord.duration = time - lastRecord.startIdleTime;
		// if we have multiple sleep states, each record will remember the
		// deepest sleepstate
		lastRecord.sleepstate = currentSleepState;
		/************************************************************************/

		// fanyao added: cancel next sleepState event
		// if (useMultipleState) {
		// if (currentSleepState != getLastSleepState()
		// && lastEnterSleepState != 0.0) {
		// StartSleepEvent event = getNextSleepEvent();
		// if (event == null) {
		// Sim.fatalError("fatal error, using multiple states but no valid next state found");
		// }
		// this.experiment.cancelEvent(event);
		// }
		// }
		StartSleepEvent event = getNextSleepEventToCancel();
		this.mExp.cancelEvent(event);
		// when core is in transitioning, treate the status as active
		// this.powerState = PowerState.TRANSITIONINGG_TO_ACTIVE;
		// this.currentSleepState = 0;

		/* before set the new state, record last sleep state before wakeup */
		this.lastStateBeforeWakeup = currentSleepState;
		// fanyao added:
		collectedIdleTime += time - this.getLastEnterSleepStateTime();
		
		/////////////////////////////////////////////////////////////////////////////////////
		//bug fix for real trace: if there is no wakeup penality, do wakeup process directly
		// set wakeup time statistics for single server sleepscale experiment
		/***
		 * // fanyao added: calcuate wakeup times
		 */
		// this.core.setWakeupTime(core.getWakeupTime() +
		// core.getTransitionToActiveTime());
		// ssExp.setWakeupTime(ssExp.getWakeupTime() +
		// core.getTransitionToActiveTime());
		
		if (transitionToActiveTime != 0.0) {
			double exitTime = 0.0;
			// consider On/off experiment
			// if (currentSleepState != 6)
			exitTime = time + transitionToActiveTime;
			// else
			// exitTime = time + this.rSetupTime;
			WakedupEvent coreWakedupEvent = new WakedupEvent(exitTime,
					this.mExp, this, mServer.getActivityUnit(),
					getCurrentSleepState());
			this.mExp.addEvent(coreWakedupEvent);
		}

		else {
			this.setWakeupTime(this.getWakeupTime()
					+ this.getTransitionToActiveTime());

			// fanyao added: for multiple sleepstates
			/************************************************************************/
			// int sleepState = core.getCurrentSleepState();
			this.accumulateSSTimes(currentSleepState,
					time - this.getLastEnterSleepStateTime());

			this.setLastEnterSleepStateTime(0.0);
			/************************************************************************/

			mServer.getActivityUnit().wakedupFromSleep(time);
		}
	}
	
	public StartSleepEvent generateSleepEvent(double time,
			Experiment experiment, int theSleepState) {
		return new StartSleepEvent(time, experiment, this.getServer(), theSleepState);
	}
	
	/**
	 * prepare for sleep when job is removed as finished and there is no further job waiting
	 */
	public void prepareForSleep(double time){
		//FIXME: this function could be removed if there is we do not assume
		//time to enter sleep state
		lastIdleTime = time;
		
		// fanyao commented: let CoreStartSleepEvent set its power state
		// at this time we still considered it is active
		// this is used to distinguish servers that stay in low power
		// idle
		// for a period of time, otherwise in UpdateThreshold() there
		// would
		// be an inconsistency sleepstate = 0 but power state is low
		// power idle
		/* this.powerState = PowerState.LOW_POWER_IDLE; */

		StartSleepEvent coreStartSleepEvent = null;
		/*****************************************************************/
		// fanyao commented: use multiple sleepstates
		if (useMultipleState) {
			coreStartSleepEvent = generateSleepEvent(time,
					this.mExp, getInitialSleepState());
			// enterC0S0 = coreStartSleepEvent;
			stateToEventsMap.put(getInitialSleepState(),
					coreStartSleepEvent);
		}

		// otherwise, use one predefined fixed low power state
		else {
			coreStartSleepEvent = generateSleepEvent(time,
					this.mExp, nextFixedSleepState);
			singleStateSleep = coreStartSleepEvent;

			// // if is is OnOff experiment, we need to generate random
			// // setup times
			// if (nextSleepState == 6) {
			// rSetupTime = ((OnOffExperiment) this.experiment)
			// .getNextSetupTime();
			// }
		}
		/*****************************************************************/
		this.setNextSleepEvent(coreStartSleepEvent, nextFixedSleepState);
		this.mExp.addEvent(coreStartSleepEvent);
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
		return mServer.getNodeId();
	}
	
	public void updateServiceTime(double time){
		this.serviceTime += time;
	}
	
	public EnergyServer getServer(){
		return mServer;
	}
	
	public void adjustSSTimes(int currentSleepState, double time) {
		// FIXME: this is a fragile design, time spent in each sleep state
		// depends on the correct record of IdleRecord
		IdleRecord lastRecord = idleDistribution.lastElement();

		double delta = (time - lastRecord.startIdleTime - lastRecord.duration);

		this.setWakeupTime(delta + wakeupTime);
		
		/* here we need to wrap up and finalize wakeup splits
		 * to keep consistency with accumulateSSTimes, we deduct the transitionToActiveTime 
		 * first and then apply for the real wakeup delta*/
		updateWakeupSplits(currentSleepState, delta - transitionToActiveTime);

		/* need to craft time in order to call accumulateSSTimes
		 * modify the second parameter with transitionToActive, so we do not have
		 * to assume what sleep state it is since subclassed controller may use different
		 * number of sleep state
		 */
//		accumulateSSTimes(lastRecord.sleepstate, lastRecord.duration
//				+ sleepStateWakeups[lastRecord.sleepstate - 1]);
		
		accumulateSSTimes(lastRecord.sleepstate, lastRecord.duration
				+ transitionToActiveTime);
		/*
		 * switch (lastRecord.sleepstate) { // when all job finished, the
		 * current sleep state of this server is // 0 // means it is in
		 * transition state
		 * 
		 * case 1: C0S0Time += duration; C0S0Count++; break; case 2: C1S0Time +=
		 * duration; C1S0Count++; break; case 3: C3S0Time += duration;
		 * C3S0Count++; break; case 4: C6S0Time += duration; C6S0Count++; break;
		 * case 5: C6S3Time += duration; C6S3Count++; break;
		 * 
		 * default: break;
		 * 
		 * }
		 */
	}
	
	public void setIdleDistribution(Vector<IdleRecord> idleDistribution) {
		this.idleDistribution = idleDistribution;
	}
	
	protected void initializeSleepStateMaps() {
		this.stateToSleepCounts = new HashMap<Integer, Integer>();
		this.stateToSleepTimes = new HashMap<Integer, Double>();

		for (int i = 0; i < sleepStateWakeups.length + 1; i++) {
			stateToSleepCounts.put(i + 1, 0);
			stateToSleepTimes.put(i + 1, 0.0);
		}

	}
	
	protected void wakedupFromSleep(double time){
		// fanyao added: set all the sleepstate events to null
		// this should also reset the reference in stateEventMap
		/***********************************************************/
		enterC0S0 = null;
		enterC1S0 = null;
		enterC3S0 = null;
		enterC6S0 = null;
		enterC6S3 = null;

		singleStateSleep = null;

		/***********************************************************/
		
		this.setCurrentSleepState(0);
	}
	
	/**
	 * get waiting time to enter next sleeps tate
	 * 
	 * @param currentSS
	 * @return
	 */
	public double getNextWaitingTime(int currentSS) {
		// TODO Auto-generated method stub
		return sleepStateWaitings[currentSS]
				- sleepStateWaitings[currentSS - 1];
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
	
	public void updateWakeupSplits(int currentState){
		double wakeupTime = sleepStateWakeups[currentState -1];
		switch(currentState){
		case 1:
			C0S0Wakeup += wakeupTime;
			break;
		case 2:
			C1S0Wakeup += wakeupTime;
			break;
		case 3:
			C3S0Wakeup += wakeupTime;
			break;
		case 4:
			C6S0Wakeup += wakeupTime;
			break;
		case 5:
			C6S3Wakeup += wakeupTime;
			break;
		default:
			Sim.fatalError("unsupported currentstate in updateWakeupSplits " + currentState);
				
		}
	}
	
	public void updateWakeupSplits(int currentState, double delta){
//		if(delta < 0){
//			Sim.fatalError("delta time less than 0.0");
//		}
		switch(currentState){
		case 1:
			C0S0Wakeup += delta;
			break;
		case 2:
			C1S0Wakeup += delta;
			break;
		case 3:
			C3S0Wakeup += delta;
			break;
		case 4:
			C6S0Wakeup += delta;
			break;
		case 5:
			C6S3Wakeup += delta;
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
	public abstract StartSleepEvent getNextSleepEvent();
	public abstract void cancelAllFutureSleepEvents(Experiment mExperiment);
	
	public abstract void prepareStats();
	
	/**
	 * ideally this function should model a state transition graph
	 * 
	 * @param currentState
	 *            , the current sleep state
	 * @return next sleep state
	 */
	public abstract int generateNextSleepState(int currentState);
	
	/**
	 * check whether the time stats for each states are collected correctly by
	 * compare three stats: tempIdle time, expected Idle time, accumulated idle
	 * time in idleDistributions
	 * 
	 * @return
	 */
	public abstract boolean timingCheck();

	/**
	 * @param theSleepState
	 *            set the current sleepstate
	 */
	public abstract void setCurrentSleepState(int theSleepState); 

 

	/**
	 * initial sleep state when simulation starts or when current sleep controller 
	 * starts a new mode
	 * @return
	 */
	public int getInitialSleepState() {
		// TODO Auto-generated method stub
		return 1;
	}

	/**
	 * default behavior collects sleep state durations for all the states
	 * predefined
	 * 
	 * @param sleepState
	 * @param time duration of time between the unit last enter sleep state
	 * to the time when it is fully waked up
	 */
	public abstract void accumulateSSTimes(int sleepState, double time);
	
	/**
	 * should be called after genPerf function
	 * 
	 * @return
	 */
	public abstract Map<Integer, Double> getStateDurations();
	
}
