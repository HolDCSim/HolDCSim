package infrastructure;

import java.util.Map;
import java.util.Vector;

import debug.Sim;

import event.OnOffSleepEvent;
import event.StartSleepEvent;
import experiment.DelayDozeExperiment;
import experiment.DelayOffExperiment;
import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * This controller is used for DelayOff and DelayDoze experiment.
 * It waits on time tau before goes to off state or deep sleep state
 * when idle 
 * @author fanyao
 *
 */
public class DelayOffController extends OnOffController {

	protected StartSleepEvent delayOffEvent = null;
	protected double rDelayOffTime;
	
	//FIXME: temp fix for waking up controller which has entered C6S3 directly, bypassing 
	//C0S0
	protected boolean bypass = false;
	
	public static enum OffMode{
		SLEEP,
		TURNED_OFF
	}
	
	/**
	 * register off state, could be deep sleep state or off state
	 */
	protected OffMode offMode;
	
	

	public DelayOffController(Experiment experiment, OffMode offMode) {
		super(experiment);
		this.offMode = offMode;	
		if(offMode == OffMode.SLEEP)
			deepiestState = 5;
		else
			deepiestState = 6;
		// TODO Auto-generated constructor stub
	}

	@Override
	public StartSleepEvent generateSleepEvent(double time,
			Experiment experiment, int theSleepState) {
		return new OnOffSleepEvent(time, experiment, this.getServer(),
				theSleepState);
	}

	public void setByPass(boolean bypass){
		this.bypass = bypass;
	}
	@Override
	public void setNextSleepEvent(StartSleepEvent nextSleepEvent, int currentSS) {
		if (currentSS == 1) {
			delayOffEvent = nextSleepEvent;
		} else {
			Sim.fatalError("DelayOffCore is in invalid sleepstate");
		}
	}

	@Override
	public StartSleepEvent getNextSleepEvent() {
		if (currentSleepState == 1) {
			return delayOffEvent;
		}

		else {
			Sim.fatalError("get next sleep event invoked incorrectly");
			return null;
		}

	}

	@Override
	public void wakedupFromSleep(final double time) {
		super.wakedupFromSleep(time);
		delayOffEvent = null;
	}

	// @Override
	// public int generateNextSleepState(int currentState) {
	// // TODO Auto-generated method stub
	// return -1;
	// }

	@Override
	public void accumulateSSTimes(int sleepState, double time) {
		C0S0Count++;
        updateWakeupSplits(sleepState);
		if (sleepState == 1) {
			C0S0Time += time - sleepStateWaitings[sleepState - 1]
					- sleepStateWakeups[sleepState - 1];
		}

		else {
			
			//C0S0Time += rDelayOffTime;

			IdleRecord record = idleDistribution.lastElement();
			double duration = record.duration;
			if(sleepState == 5){
				if (!bypass) {
					C0S0Time += rDelayOffTime;
					C6S3Time += duration - rDelayOffTime;
				}
				else{
					C6S3Time += duration;
				}
				C6S3Count++;
			}
			if (sleepState == 6) {
				if (!bypass) {
					timeInOffState += duration - rDelayOffTime;
				} 
				else {
					timeInOffState += duration;
				}
				offStateCount++;
			}
			
			//reset bypass
			if(bypass)
				bypass = false;
		}
		
		
		return;

	}

	public void resetDelayOffTime() {
		this.rDelayOffTime = 0.0;
	}

//	@Override
//	public void adjustSSTimes(int sleepState, double time) {
//
//		IdleRecord lastRecord = idleDistribution.lastElement();
//		double duration = lastRecord.duration;
//
//		// query idle distribution to get last time the server is tried to
//		// wakeup
//		double delta = time - (lastRecord.startIdleTime + lastRecord.duration);
//		this.setWakeupTime(delta + wakeupTime);
//		
//		
//		switch (lastRecord.sleepstate) {
//		case 1:
//			C0S0Time += duration;	
//			C0S0Count++;
//			break;
//		case 5:
//			C0S0Time += rDelayOffTime;
//			C6S3Time += duration - rDelayOffTime;
//			C6S3Count++;
//			break;
//		case 6:
//			C0S0Time += rDelayOffTime;
//			timeInOffState += duration - rDelayOffTime;
//			offStateCount++;
//			break;
//		default:
//			break;
//
//		}
//	}

	@Override
	public void prepareStats() {
		this.activeTime = this.serviceTime + this.getWakeupTime();
		this.idleTime = mExp.getSimulationTime() - this.activeTime;
		// this.collectedIdleTime = C0S0Time + timeInOffState;
		this.collectedWakeupTime = C0S0Wakeup + C6S3Wakeup + offStateWakeup;
		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
				+ C6S3Time + timeInOffState;
		this.jobProcessingTime = activeTime - getWakeupTime();
	}

	@Override
	public double getNextWaitingTime(int currentSS) {
		// TODO Auto-generated method stub
		if (currentSS == 1) {
			if (mExp instanceof DelayDozeExperiment) {
				DelayDozeExperiment exp = (DelayDozeExperiment) mExp;
				rDelayOffTime = exp.getNextDelayTime();
			}

			else {
				DelayOffExperiment exp = (DelayOffExperiment) mExp;
				rDelayOffTime = exp.getNextDelayTime();
			}

			return rDelayOffTime;
		} else {
			Sim.fatalError("DelayoffCore is in invalid sleepstate");
			return 0.0;
		}

	}
	
	@Override
	public int generateNextSleepState(int currentState) {
		// TODO Auto-generated method stub
		int nextState = -1;
		switch (currentState) {
		case 1:
			if(offMode == OffMode.SLEEP)
				nextState = 5;
			else
				nextState = 6;
			break;
		default:
			Sim.fatalError("fatal error: should not request next sleep state with current state " + currentState);

		}
		return nextState;
	}

}
