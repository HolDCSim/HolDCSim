package infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import utility.Pair;
import utility.ServerPowerMeter;
import constants.Constants;

import debug.Sim;

import event.StartSleepEvent;
import experiment.Experiment;
import experiment.OnSleepExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * This controller is used for OnOffExperiment and OnSleepExperient. It toggles states
 * based between on/off or idle/ deep sleep
 * @author fanyao
 *
 */
public class OnOffController extends AbstractSleepController {
	
	/**************************************************************/
	// statictics for random setup and random delay off
	/**
	 * random transition to active time, for OnOffExperiment and delay off
	 * experiment
	 */
	protected double rSetupTime;

	protected double timeInOffState = 0.0;
	
	protected double offStateWakeup = 0.0;
	
	//number of times sleep controller goes to off state
	protected int offStateCount;

	/**************************************************************/

	public OnOffController(Experiment experiment) {
		super(experiment);
		
//		idleDistribution = new Vector<IdleRecord>();
//		IdleRecord firstRecord = new IdleRecord();
//		firstRecord.startIdleTime = 0.0;
//		firstRecord.sleepstate = 1;
//
//		idleDistribution.add(firstRecord);
		
		// TODO Auto-generated constructor stub
	}
	
	public void generaterSetupTime() {
		// TODO Auto-generated method stub
		rSetupTime = ((OnSleepExperiment) this.mExp).getNextSetupTime();

	}
	
	@Override
	public void startSleep(final double time, int sleepState) {

		/**
		 * if it is OnOff experiment, we need to generate random setup times, we
		 * need to update this before super.startsleep transition time is set. 
		 * OnSleepExperiment has fixed setup time
		 */
		if (sleepState == 5 || sleepState == 6) {
			generaterSetupTime();
			Sim.debug(3, "ZZZ next setup time is " + rSetupTime);
		}

		super.startSleep(time, sleepState);

	}
 
//	@Override
//	public void adjustSSTimes(int currentSleepState, double time) {
//
//		IdleRecord lastRecord = idleDistribution.lastElement();
//		double duration = lastRecord.duration;
//
//		// query idle distribution to get last time the server is tried to
//		// wakeup
//		double delta = time - (lastRecord.startIdleTime + lastRecord.duration);
//		this.setWakeupTime(delta + wakeupTime);
//
//		switch (lastRecord.sleepstate) {
//		case 1:
//			C0S0Time += duration;
//			C0S0Count++;
//			break;
//		case 5:
//			C6S3Time += duration;
//			C6S3Count++;
//			break;
//		case 6:
//			timeInOffState += lastRecord.duration;
//			offStateCount++;
//			break;
//		default:
//			break;
//
//		}
//
//	}
	@Override
	public void setNextSleepEvent(StartSleepEvent nextSleepEvent, int currentSS) {
		// TODO Auto-generated method stub
		return;

	}

	@Override
	public StartSleepEvent getNextSleepEvent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancelAllFutureSleepEvents(Experiment mExperiment) {

	}

	@Override
	public void prepareStats() {
		this.activeTime = this.serviceTime + this.getWakeupTime();
		this.idleTime = mExp.getSimulationTime() - this.activeTime;
		this.collectedWakeupTime = C1S0Wakeup + C6S3Wakeup + offStateWakeup;
		//this.collectedIdleTime = C0S0Time + timeInOffState;
		this.collectedIdleTime = C0S0Time + C6S3Time + timeInOffState;
		this.jobProcessingTime = activeTime - getWakeupTime();
	}
	

	/* (non-Javadoc)
	 * should not be called in OnOffController since this controller uses fixed
	 * sleep state
	 * @see infrastructure.AbstractSleepController#generateNextSleepState(int)
	 */
	@Override
	public int generateNextSleepState(int currentState) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean timingCheck() {

		if(this.C0S0Time < 0 || this.C1S0Time < 0 || this.C3S0Time < 0 || this.C6S0Time < 0 || this.C6S3Time < 0){
			Sim.fatalError("fatal error, sleep duration less than 0!");
			return false;
		}
		if (Math.abs(collectedIdleTime - idleTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, expected idle != collected Idle, server : " + this.getNodeId());
			return false;
		}
		
		if (Math.abs(collectedWakeupTime - wakeupTime) > Constants.TIMING_RRECISION) {
			Sim.warning("timing check failed, expected collectedWakeupTime != wakeupTime");
			return false;
		}

		if (useMultipleState) {
			Sim.debug(4,
					"no distribution check in for cores with multiple states");
			return true;
		}

		// distribution check
		double collectedC0S0Time = 0.0;
		double collectedC6S3Time = 0.0;
		double collectedOffTime = 0.0;

		for (IdleRecord record : idleDistribution) {
			switch (record.sleepstate) {
			case 1:
				collectedC0S0Time += record.duration;
				break;
			case 2:
			case 3:
			case 4:
			case 5:
				collectedC6S3Time += record.duration;
				//Sim.fatalError("unexpected sleep state record in idle distribution");
				break;
			case 6:
				collectedOffTime += record.duration;
				break;
			default:
				Sim.fatalError("unknown sleep state when performing timing check");
			}
		}

		if (Math.abs(collectedC0S0Time - C0S0Time) > Constants.TIMING_RRECISION
				|| Math.abs(collectedOffTime - timeInOffState) > Constants.TIMING_RRECISION) {
			Sim.warning("collected sleep state distribution incorrect");
			return false;
		} else
			return true;
	}

	@Override
	public void setCurrentSleepState(int theSleepState) {
		// TODO Auto-generated method stub
		// this.fixedSleepState = theSleepState;
		Pair<Double, Integer> pair = new Pair<Double,Integer>();
		pair.setFirst(mExp.getCurrentTime());
		pair.setSecond(theSleepState);
		this.getServer().SSTrace.add(pair);
		this.currentSleepState = theSleepState;
		if (currentSleepState == 0 || currentSleepState == 1) {
			transitionToActiveTime = 0.0;
			return;

		}

//		if (theSleepState != 6)
//			this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];
//
//		else
			this.transitionToActiveTime = rSetupTime;

	}

	@Override
	public void updateWakeupSplits(int currentState){
		switch(currentState){
		case 1:
			C0S0Wakeup += sleepStateWakeups[currentState -1];
			break;
		case 5:
			C6S3Wakeup += rSetupTime;
			break;
		case 6:
			offStateWakeup += rSetupTime;
			break;
		default:
			Sim.fatalError("unsupported currentstate in updateWakeupSplits "
					+ currentState);
		}
	}
	
	public void updateWakeupSplits(int currentState, double delta){
		switch(currentState){
		case 1:
			C0S0Wakeup += delta;
			break;
		case 5:
			C6S3Wakeup += delta;
			break;
		case 6:
			offStateWakeup += delta;
		default:
			Sim.fatalError("unsupported currentstate in updateWakeupSplits " + currentState);
				
		}
	}

	@Override
	public void accumulateSSTimes(int sleepState, double time) {
		// TODO Auto-generated method stub

		//fanyao added: update wakeup splits
		this.updateWakeupSplits(sleepState);
		IdleRecord record = idleDistribution.lastElement();
		if (sleepState == 1) {
			C0S0Count++;
			C0S0Time += time - sleepStateWaitings[sleepState - 1]
					- sleepStateWakeups[sleepState - 1];
		} 
		else if(sleepState == 5){
			C6S3Time += record.duration;
			C6S3Count++;
		}
		else if (sleepState == 6) {
			// use idle record instead, since in onoff mechanism
			// transition to active could be interrupted
			timeInOffState += record.duration;
			offStateCount++;
		}

		else {
			Sim.fatalError("fatal error: OnOffController is in invalid sleepstate :"
					+ sleepState);
		}

	}


	@Override
	public Map<Integer, Double> getStateDurations() {
		// TODO Auto-generated method stub
		Map<Integer, Double> stateDurations = new HashMap<Integer, Double>();

		stateDurations.put(0, this.serviceTime);
		stateDurations.put(1, C0S0Time);
		stateDurations.put(5, C6S3Time);
		stateDurations.put(6, timeInOffState);
		// -1 denotes wakeup
		stateDurations.put(-1, this.wakeupTime);
		return stateDurations;
	}


}
