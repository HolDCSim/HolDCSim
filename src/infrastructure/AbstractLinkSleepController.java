package infrastructure;

import java.util.*;

import debug.Sim;
import utility.FakeRecordVector;
import event.LinkWakedupEvent;
import event.StartLinkSleepEvent;
import event.StartSleepEvent;
import experiment.Experiment;
import experiment.NetworkExperiment.IdleRecord;;

public  abstract class AbstractLinkSleepController{

	/* LPI minimum wakeup and sleep times for different link speeds, 100Mbps, 1000Mbps, 10Gbps */
	public static double[] sleepStateWakeups = { 30.5e-6, 16.5e-6, 4.48e-6};

	public static double[] sleepStateWaitings = { 200e-6, 182e-6, 2.88e-6};
		
	/*************************************************************/
	// events for enter link sleep state
	protected StartLinkSleepEvent enterLPI = null;

	/**************************************************************/
	// statistics for LPI sleepstates experiments single link
	public double LPITime = 0.0;
	
	//time spend in wakeup due to sleep in LPI
	public double LPIWakeup = 0.0;

	public int LPICount = 0;

	/**************************************************************/
	
	/**
	 * fixed sleepstate level for this sleep controller
	 */
	protected int FixedSleepState;

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
	 * idle time distribution each period record represent the link from
	 * beginning of entering LPI to wakeup
	 */
	protected Vector<IdleRecord> idleDistribution;

	/**
	 * last time when the link first enter LPI state
	 */
	protected double lastEnterSleepState;
	
	public double serviceTime = 0.0;

	public double activeTime = 0.0;

	public double idleTime = 0.0;

	public double jobProcessingTime = 0.0;

	public double wakeupTime = 0.0;
	public double sleepWaitingTime = 0.0;
	
	public double collectedIdleTime;
	
	public double collectedWakeupTime = 0.0;
	
	protected double lastIdleTime = Double.MAX_VALUE;
	
	protected Experiment mExp;
	
	protected Link aLink;
	
	protected EnergyServer mServer;
	
	protected int stateToSleepTimes;
	
	protected int stateToSleepCounts;
	
	/**
	 * The transition time of the link to active.
	 */
	protected double transitionToActiveTime;
	
	// events for single sleep state
	protected StartLinkSleepEvent singleStateSleep = null;
	
	
	public AbstractLinkSleepController(Experiment experiment){
		this.mExp = experiment;
		//subclass needs to define its own deepiest sleep state
		//this.mServer = aServer;
	}
	
	/**
	 * needs to set the link since later the reference to the link is useful
	 * @param aLink
	 */
	public void setLink(Link aLink){
		this.aLink = aLink;
	}
	
	public void initialController(){
		this.FixedSleepState = 1;
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
	
	/**
	 * @return returns the transition to active time based on current sleep state
	 */
	public double getTransitionToActiveTime() {
		return this.transitionToActiveTime;
	}
	
	public int getFixedSleepState() {
		return FixedSleepState;
	}

	public double getLastEnterSleepStateTime() {
		return this.lastEnterSleepState;
	}

	public void setLastEnterSleepStateTime(double time) {
		this.lastEnterSleepState = time;
	}
	
	public double getWakeupTime() {
		return wakeupTime;
	} 
	
	public double getsleepWaitingTime() {
		return sleepWaitingTime;
	} 

	/**
	 * 
	 * @param theSleepState
	 *            , the sleepstate next time when the core goes sleep
	 */
	public void setFixedSleepState(int theSleepState) {
		this.FixedSleepState = theSleepState;
	}

	/**
	 * return the idle distributions of current sleepable unit
	 * 
	 * @return
	 */
	public Vector<IdleRecord> getIdleDistribution() {
		return idleDistribution;
	}
	
	public StartLinkSleepEvent getSingleSleepEvent() {
		return singleStateSleep;
	}
	
	public void setWakeupTime(double d) {
		wakeupTime = d;
	}
	public void setsleepWaitingTime(double d) {
		sleepWaitingTime = d;
	}
	
	public double getLastIdleTime() {
		return this.lastIdleTime;
	}
	
	public int getStateBeforeWakeup() {
		return lastStateBeforeWakeup;
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
		
		if (transitionToActiveTime != 0.0) {
			double exitTime = 0.0;
			// consider On/off experiment
			// if (currentSleepState != 6)
			exitTime = time + transitionToActiveTime;
			
			//add linkWakeupEvent after the link wakeup time
			LinkWakedupEvent linkWakedupEvent = new LinkWakedupEvent(exitTime,
					this.mExp, this,getCurrentSleepState());
			this.mExp.addEvent(linkWakedupEvent);
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
	
	public StartLinkSleepEvent generateSleepEvent(double time,
			Experiment experiment, int theSleepState) {
		return new StartLinkSleepEvent(time, experiment, this.getLink(), theSleepState);
	}
	
	/**
	 * prepare for sleep when job is removed as finished and there is no further job waiting
	 */
	public void prepareForSleep(double time){
		//FIXME: this function could be removed if there is we do not assume
		//time to enter sleep state
		lastIdleTime = time+sleepWaitingTime;
		
		// fanyao commented: let CoreStartSleepEvent set its power state
		// at this time we still considered it is active
		// this is used to distinguish servers that stay in low power
		// idle
		// for a period of time, otherwise in UpdateThreshold() there
		// would
		// be an inconsistency sleepstate = 0 but power state is low
		// power idle
		/* this.powerState = PowerState.LOW_POWER_IDLE; */

		/*****************************************************************/

		// use one predefined fixed low power state
		
		singleStateSleep = generateSleepEvent(time,this.mExp, FixedSleepState);
		
		/*****************************************************************/
		this.mExp.addEvent(singleStateSleep);
	}
	
	/**
	 * Puts the link into LPI mode. 
	 * 
	 * @param time
	 *            - the time this sleepable enters LPI
	 */
	protected void startSleep(final double time, int sleepState){
		setCurrentSleepState(sleepState);
	}
	
	public int getLinkId(){
		return aLink.hashCode();
	}
	
	public void updateServiceTime(double time){
		this.serviceTime += time;
	}
	
	public Link getLink(){
		return aLink;
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
		this.stateToSleepTimes=0;
		this.stateToSleepCounts=0;
	}
	
	protected void wakedupFromSleep(double time){
		// fanyao added: set all the sleepstate events to null
		// this should also reset the reference in stateEventMap
		/***********************************************************/
		enterLPI = null;

		singleStateSleep = null;

		/***********************************************************/
		
		this.setCurrentSleepState(0);
	}
	
	/**
	 * get waiting time to enter next sleeps state
	 * 
	 * @param currentSS
	 * @return
	 */
	public double getNextWaitingTime(int currentSS) {
		return sleepStateWaitings[currentSS]
				- sleepStateWaitings[currentSS - 1];
	}
	
	/**
	 * subclass should implement this method for custom sleep state transitions
	 * 
	 * @param nextSleepEvent
	 * @param currentSS
	 */
	public void setNextSleepEvent(StartLinkSleepEvent nextSleepEvent, int currentSS) {

		// TODO Auto-generated method stub
		switch (currentSS) {
		case 1:
			enterLPI = nextSleepEvent;
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
			LPIWakeup += wakeupTime;
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
			LPIWakeup += delta;
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

