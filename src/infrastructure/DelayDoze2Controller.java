package infrastructure;

import java.util.HashMap;
import java.util.Map;

import infrastructure.DelayOffController.OffMode;
import utility.Pair;
import debug.Sim;
import event.StartSleepEvent;
import experiment.DelayDoze2Experiment;
import experiment.DelayDozeExperiment;
import experiment.DelayOffExperiment;
import experiment.Experiment;
import experiment.SingletonJobExperiment.IdleRecord;

public class DelayDoze2Controller extends DelayOffController {

	protected StartSleepEvent delayOffEvent2 = null;
	
	/**
	 * delay off time for idle -> s4 
	 */
	protected double rDelayOffTime2;
	
	public DelayDoze2Controller(Experiment experiment) {
		super(experiment, OffMode.SLEEP);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void setNextSleepEvent(StartSleepEvent nextSleepEvent, int currentSS) {
		if (currentSS == 1) {
			delayOffEvent = nextSleepEvent;
		} 
		//FIXME: hardcoded sleep state
		else if (currentSS == 4){
			delayOffEvent2 = nextSleepEvent;
		}
		else {
			Sim.fatalError("DelayOffCore is in invalid sleepstate");
		}
	}
	
	@Override
	public StartSleepEvent getNextSleepEvent() {
		if (currentSleepState == 1) {
			return delayOffEvent;
		}
		else if(currentSleepState == 4){
			return delayOffEvent2;
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
		delayOffEvent2 = null;
	}
	
	@Override
	public void updateWakeupSplits(int currentState){
		// TODO Auto-generated method stub
		// this.fixedSleepState = theSleepState;
		if(currentState == 4){
	    	C6S0Wakeup += sleepStateWakeups[currentSleepState - 1];
	    }
	    else{
	    	super.updateWakeupSplits(currentState);
	    }

//		if (theSleepState != 6)
//			this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];
//
//		else
		
	}
	
	@Override
	public void updateWakeupSplits(int currentState, double delta){
	    if(currentState == 4){
	    	C6S0Wakeup += delta;
	    }
	    else{
	    	super.updateWakeupSplits(currentState, delta);
	    }

	}
	
	@Override
	public void setCurrentSleepState(int theSleepState) {
		// TODO Auto-generated method stub
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
		if (currentSleepState == 4) {
			this.transitionToActiveTime = sleepStateWakeups[currentSleepState - 1];
		} else {
			this.transitionToActiveTime = rSetupTime;
		}
	}
	
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
			if(sleepState == 4){
				C0S0Time += rDelayOffTime2;
				C6S0Time += duration - rDelayOffTime2;
			}
			else if(sleepState == 5){
				if (!bypass) {
					C0S0Time += rDelayOffTime2;
					C6S0Time += rDelayOffTime;
					C6S3Time += duration - rDelayOffTime - rDelayOffTime2;
					if(duration - rDelayOffTime - rDelayOffTime2 < 0){
						Sim.fatalError("fatal error: incremented C6S3Time less than 0");
					}
				}
				else{
					C6S3Time += duration;
				}
				C6S0Count++;
				C6S3Count++;
			}
			
			//reset bypass
			if(bypass)
				bypass = false;
		}
		
		
		return;

	}
	
	@Override
	public void prepareStats() {
		this.activeTime = this.serviceTime + this.getWakeupTime();
		this.idleTime = mExp.getSimulationTime() - this.activeTime;
		// this.collectedIdleTime = C0S0Time + timeInOffState;
		this.collectedWakeupTime = C0S0Wakeup + C6S0Wakeup + C6S3Wakeup + offStateWakeup;
		this.collectedIdleTime = C0S0Time + C1S0Time + C3S0Time + C6S0Time
				+ C6S3Time + timeInOffState;
		this.jobProcessingTime = activeTime - getWakeupTime();
	}
	
	@Override
	public double getNextWaitingTime(int currentSS) {
		// TODO Auto-generated method stub
		DelayDoze2Experiment exp = (DelayDoze2Experiment) mExp;
		
		if (currentSS == 1) {
			rDelayOffTime2 = exp.getNextDelayTime2();
			return rDelayOffTime2;
		}
		
		else if(currentSS == 4){
			rDelayOffTime = exp.getNextDelayTime();
			return rDelayOffTime;
		}
//		else if(currentSS == 4){
//			DelayDoze2Experiment exp = (DelayDoze2Experiment) mExp;
//			rDelayOffTime = exp.getNextDelayTime();
//		}
		else {
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
				nextState = 4;
			break;
		case 4:
				nextState = 5;
				if(offMode != OffMode.SLEEP){
					Sim.fatalError("fatal error: unexpected sleep state 4 in OffMode.off mode");
				}
			break;
		default:
			Sim.fatalError("fatal error: should not request next sleep state with current state " + currentState);

		}
		return nextState;
	}
	
	@Override
	public Map<Integer, Double> getStateDurations() {
		// TODO Auto-generated method stub
		Map<Integer, Double> stateDurations = super.getStateDurations();

		// -1 denotes wakeup
		stateDurations.put(4, this.C6S0Time);
		return stateDurations;
	}


}
