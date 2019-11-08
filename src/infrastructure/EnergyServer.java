package infrastructure;

import infrastructure.Core.PowerState;
import utility.Pair;

import java.util.Map;
import java.util.Vector;

import constants.Constants;
import debug.Sim;

import experiment.Experiment;
import experiment.SingletonJobExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * abstract server that uses our energy model with sleep state (based on activity durations)
 * @author fanyao
 *
 */
public abstract class EnergyServer extends Server {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public double energy;

	public double wakeupEnergy;

	public double idleEnergy;

	public double productiveEnergy;
	
	/**
	 * the sleepable unit, could be socket or a single core
	 * needs initialization from subclass
	 */
	protected AbstractSleepController sleepable = null;
	
	protected IActivityUnit activityUnit = null;
	
	public Vector<Pair<Double, Integer>> SSTrace;
	public Vector<Pair<Double, Integer>> SSGraph;
	
	public EnergyServer(Experiment anExperiment, DataCenter dataCenter,
			Socket[] theSockets, final AbstractSleepController abController, Constants.SleepUnit sleepUnit) {
		super(anExperiment, dataCenter, theSockets);
		abController.setServer(this);
		this.sleepable = abController;
		this.SSTrace = new Vector<Pair<Double,Integer>>();
		this.SSGraph = new Vector<Pair<Double,Integer>>();
		
		if(sleepUnit == Constants.SleepUnit.CORE_LEVEL){
			activityUnit = theSockets[0].getCores().get(0);
		}
		else if(sleepUnit == Constants.SleepUnit.SOCKET_LEVEL||
				sleepUnit == Constants.SleepUnit.NO_SLEEP){
			activityUnit = theSockets[0];
		}
		else{
			Sim.fatalError("current simulator does not support SOCKET_CORE_COMBINED SLEEP mode");
			System.exit(0);
		}
		// TODO Auto-generated constructor stub
	}
	
	public EnergyServer(final int theNumberOfSockets, final int theCoresPerSocket,
			final Experiment anExperiment, final DataCenter dataCenter) {
		super(theNumberOfSockets, theCoresPerSocket, anExperiment, dataCenter);

		this.SSTrace = new Vector<Pair<Double,Integer>>();
		this.SSGraph = new Vector<Pair<Double,Integer>>();
	}

	public void setUseMultipleSleepStates(boolean useMS){
		sleepable.setUseMultipleSleepState(useMS);
	}
	
	public void setCurrentSleepState(int theSleepState){
		sleepable.setCurrentSleepState(theSleepState);
	}
	
	public int getCurrentSleepState() {
		return sleepable.getCurrentSleepState();
	}
	
	public void updateWakupTime() {
		// TODO Auto-generated method stub
		
		sleepable.setWakeupTime(sleepable.getWakeupTime()
				+ sleepable.getTransitionToActiveTime());
	}

	public void updateWakupTime(double delta) {
		sleepable.setWakeupTime(sleepable.getWakeupTime() + delta);
	}

	public double getTansitionToActiveTime() {
		return sleepable.getTransitionToActiveTime();
	}

	public double getWakeupTime() {
		return sleepable.getWakeupTime();
	}

	public Vector<IdleRecord> getIdleDistribution() {
		return sleepable.getIdleDistribution();
	}

	public double getLastIdleTime() {
		// TODO Auto-generated method stub
		return sleepable.getLastIdleTime();
	}
	
	public boolean doUseMultipleSleepStates() {

		return sleepable.doUseMultipleSleepStates();
	}
	
	public void accumulateSSTimes(int sleepState, double time) {
		sleepable.accumulateSSTimes(sleepState, time);
	}
	
	public double getLastEneterSleepStateTime() {
		return sleepable.getLastEnterSleepStateTime();
	}
	
	public void setLastSleepStateTime(double time) {
		// TODO Auto-generated method stub
		sleepable.setLastEnterSleepStateTime(time);
	}
	

	public int getNextSleepState() {
		return sleepable.getFixedSleepState();
	}
	
	public void setNextFixedSleepState(int theSleepState) {
		// set the fixed sleepstate for the single core processor
		/*******************************************************/
		sleepable.setNextFixedSleepState(theSleepState);
		/*******************************************************/
	}
	
	public void udpateFinalStats(double time) {

		int currentSleepState = sleepable.getCurrentSleepState();
		
		if (!(this.experiment instanceof SingletonJobExperiment) || 
				(((SingletonJobExperiment) this.experiment).sleepUnit != Constants.SleepUnit.NO_SLEEP)) {
			if(this.isActive()){
				Sim.fatalError("fatal error: server : " + this.getNodeId() + "active when all job finished");
			}
		}

		/*FIXME: server could be in transition to active state, but sleep state not set to 0*/
		if (!this.isActive() && !this.isTransitionToActive()) {
			if (this.getIdleDistribution().size() == 0) {
				IdleRecord onlyRecord = new IdleRecord();
				onlyRecord.startIdleTime = 0.0;
				onlyRecord.duration = time;
				onlyRecord.sleepstate = currentSleepState;
				this.getIdleDistribution().add(onlyRecord);
			}
			IdleRecord record = this.getIdleDistribution().lastElement();
			if(record.duration == -1) {
				if(this.getNodeId() == 1) {
					System.out.println("Setting record duration for server 1");
				}
				record.duration = time - record.startIdleTime;
			}
			else
				System.out.println("ERROR duration ");
				//Sim.fatalError("try to set duration twice");

			/*
			 * toggle this so that it always shows the last sleep state if
			 * multiple sleeps states are used
			 */
			record.sleepstate = currentSleepState;
			
		}
		
		if (!(this.experiment instanceof SingletonJobExperiment) || 
		    (((SingletonJobExperiment) this.experiment).sleepUnit != Constants.SleepUnit.NO_SLEEP)) {
			sleepable.adjustSSTimes(currentSleepState, time);
		}


		// need to update tempIdleTime as well
		// Double lastSleepTime = getLastEneterSleepStateTime();
		// if(time != lastSleepTime){
		// this.updateTempIdleTime(time - lastSleepTime);
		// }

	}
	
	public boolean timingCheck() {
		// TODO Auto-generated method stub
		return sleepable.timingCheck();
	}
	
	public Map<Integer, Double> getStateDurations() {
		return sleepable.getStateDurations();
	}
	
	public void setActive(){
		activityUnit.setActive();
	}
	
	public  boolean isActive(){
		return activityUnit.isActive();
	}
	
	public  boolean isTransitionToActive(){
		return activityUnit.isTransitionToActive();
	}
	
	public  void setTransitionToActive(){
		activityUnit.setTransitionToActive();
	}
	
	public AbstractSleepController getSleepController(){
		return sleepable;
	}
	
	public IActivityUnit getActivityUnit(){
		return activityUnit;
	}

	public Vector<Pair<Double, Integer>> getSSTrace(){
		return SSTrace;
	}
	
	/**
	 * E-Rover's energy model
	 * @return
	 */
	public abstract  double generateEnergyAndPerfStats();

	public double getAverageSleepState(double totalTime) {
		double total = 0;
		
		total = total + this.sleepable.C0S0Time*0;
		total = total + this.sleepable.C1S0Time*1;
		total = total + this.sleepable.C3S0Time*2;
		total = total + this.sleepable.C6S0Time*3;
		total = total + this.sleepable.C6S3Time*4;
	/*	

		for (int i =0; i<SSTrace.size(); i++) {

			if(i<SSTrace.size()-1) {
//				total = total + SSTrace.get(i).getSecond()*SSTrace.get(i+1).getFirst();
				total = total + SSTrace.get(i).getSecond()*(SSTrace.get(i+1).getFirst()-SSTrace.get(i).getFirst());
			}
			else if(i==SSTrace.size()-1) {
				total = total + SSTrace.get(i).getSecond()*(totalTime - SSTrace.get(i).getFirst());
			}
			

		}
		*/
		double average = total / totalTime;
		
		return average;
	}

	public int getSleepStateByTime(double time) {
		
		int j =1;
		while(j<SSTrace.size()) {

			if(SSTrace.get(SSTrace.size()-j).getFirst() <= time) {
				break;
			}
			//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
			j = j+1;
		}
		
		return SSTrace.get(SSTrace.size()-j).getSecond();
	}
	
	public double getAverageSleepStateOverTime(double timeStart, double timeStop) {
		
		double average;
		double total = 0;
		Vector<Pair<Double, Integer>> vect = new Vector<Pair<Double,Integer>>();
		vect = SSTrace;

	
		int j = 1;
		while(j< vect.size()) {

			if(vect.get(vect.size()-j).getFirst() <= timeStart) {
				break;
			}
			//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
			j = j+1;
		}
		int j_start = j;
		j =1;
		while(j< vect.size()) {

			if(vect.get(vect.size()-j).getFirst() <= timeStop) {
				break;
			}
			//System.out.print(vect.get(j).getFirst() + " " + i + "\n");
			j = j+1;
		}
		int j_stop = j;
		
		// The start and stop time are in the same transition period, the server is in the same sleep state for the whole duration
		if (j_start == j_stop) {
		//	System.out.println("j's equal, avg ss:", vect.get)
			average = vect.get(vect.size()-j_start).getSecond();
		}
		else {
			// Start time to first transition
			total = total + (vect.get(vect.size()-j_start+1).getFirst() - timeStart)* vect.get(vect.size()-j_start).getSecond();
			j = j_start + 1;
			// Transition to transition
			while(j<j_stop) {
				// Add the whole transition time to the total
				total = total + (vect.get(vect.size()-j+1).getFirst() - vect.get(vect.size()-j).getFirst())*vect.get(vect.size()-j).getSecond();
				j = j+1;
			}
			
			
			// Last transition to stop time
			total = total + (timeStop - vect.get(vect.size()-j_stop).getFirst())*vect.get(vect.size()-j_stop).getSecond(); 
			average = total/(timeStop-timeStart);
		}
		
		
		return average;
		
		
	}
	
	
}
