package infrastructure;

import infrastructure.LineCard.LineCardPowerPolicy;
import infrastructure.LineCard.LineCardState;
import infrastructure.Port.PortPowerPolicy;

import java.util.*;
import java.util.Map.Entry;

import communication.Flow;
import communication.Packet;

import constants.Constants;
import debug.Sim;
import event.LineCardTransitiontoSleepEvent;
import stat.TimeWeightedStatistic;
import utility.Pair;
import experiment.Experiment;

public class LCSwitch extends DCNode implements Routable {
	
	public static enum Type {
		CORE,
		AGGREGATE,
		EDGE
	}
	
	private static int PORT_PER_LINECARD = 8;
	private int numOfLineCards;
	private int numOfActiveLineCards;
	private int numOfSleepLineCards;
	private int numOfOffLineCards;
	
	private int numOfActivePorts; // number of ports transmitting data
	private int numOfLPIPorts; // number of ports in LPI state
	
	private double switchpower;
	private LCSwitch node = null;
	
	private Vector<Integer> linecardIds;
	
	public Vector<Pair<Double, Integer>> SSTrace;
	public Vector<Pair<Double, Integer>> SSGraph;


	//public static int NO_PORT_RATES = Port.availableRates.size();
	public static int NO_PORT_RATES = 3;

	// TODO: Set activityUnit
	protected IActivityUnit activityUnit = null;
	
	protected SwitchSleepController switchSleepable = null;
	protected boolean isScheduledActive;
	
	public int flowQueueSize = 0;
	
	private Type type;

	public LCSwitch(DataCenter dataCenter, SwitchSleepController switchSleepController) {
		super(dataCenter);
		this.switchSleepable = switchSleepController;
		this.isScheduledActive = false;
		this.SSTrace = new Vector<Pair<Double,Integer>>();
		this.SSGraph = new Vector<Pair<Double,Integer>>();
	}
	
	public void addPortMap(Map<Integer, Port> _portMap) {
		super.addPortMap(_portMap);
		
		this.numOfLPIPorts = _portMap.size();
	}
	
	public void addLinecardMap (Map<Integer, LineCard> _linecardMap) {
		super.addLinecardMap(_linecardMap);
		
		this.numOfLineCards = _linecardMap.size();
	}
	
	public LineCardTransitiontoSleepEvent generateLineCardSleepEvent(double time,
			Experiment experiment, DCNode node, int linecardId, SwitchSleepController switchSleepController) {
		return switchSleepable.generateLineCardSleepEvent(time, experiment, node, linecardId, switchSleepController);
	}

	public void printPortU() {
		// Iterator it = portUHistory.entrySet().iterator();
		// while (it.hasNext()) {
		// Map.Entry pairs = (Map.Entry)it.next();
		// Vector<Vector<Double>> onePortHistory=
		// (Vector<Vector<Double>>)pairs.getValue();
		// Integer portNum = (Integer)pairs.getKey();
		//
		// for(int j=0;j<onePortHistory.size();j++){
		// System.p
		// }
		// }
	}

	/**
	 * Sets the power management policy for the CPU cores in this server.
	 * 
	 * @param corePowerPolicy
	 *            - the power management policy to use
	 */
	public void setPortPolicy(final PortPowerPolicy portPowerPolicy) {
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			this.linecardMap.get(i).setPortPolicy(portPowerPolicy);
		}
	}
	
	/**
	 * Set the server's sockets' power management policy.
	 * 
	 * @param socketPolicy
	 *            - the power management policy
	 */
	public void setLineCardPolicy(final LineCardPowerPolicy lineCardPolicy) {
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			this.linecardMap.get(i).setPowerPolicy(lineCardPolicy);
		}
		
		if(lineCardPolicy == LineCardPowerPolicy.NO_MANAGEMENT) {
			this.numOfActiveLineCards = this.linecardMap.size();
		}
		else if(lineCardPolicy == LineCardPowerPolicy.LINECARD_SLEEP) {
			this.numOfSleepLineCards = this.linecardMap.size();
		}
	}
	
	public void updateStatistics(final double time) {
		TimeWeightedStatistic portUtilization = this.mDataCenter.experiment
				.getStats().getTimeWeightedStat(
						Constants.TimeWeightedStatName.PORT_UTILIZATION);
		// FIXME
		portUtilization.setRowSize(this.portMap.size());

		Iterator<Entry<Integer, Port>> it = portMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Port> pairs =  it.next();
			Port port = (Port) pairs.getValue();
			/**Integer portNum = (Integer) pairs.getKey();**/

			// FIXME: now we have two states, needs to determine which
			// state to query
			if ((port.getInPortState() == Port.PortPowerState.busy)
					&& (port.getOutPortState() == Port.PortPowerState.busy)) {
				// if (port.getInPortState() == Port.PortState.busy)
				// TODO: fanyao commented for memory profiling

				portUtilization.addSample(port.getPortRate(), time);

				// portUHistory.get(portNum.intValue()).add(oneSample);
			} else {
				portUtilization.addSample(0.0, time);

				Vector<Double> oneSample = new Vector<Double>();
				oneSample.add(0.0);
				oneSample.add(time);
				// TODO: fanyao commented for memory profiling
				// portUHistory.get(portNum.intValue()).add(oneSample);
			}
		}

		TimeWeightedStatistic switchPowerStat = this.mDataCenter.experiment
				.getStats().getTimeWeightedStat(
						Constants.TimeWeightedStatName.SWITCH_POWER);

		switchPowerStat.setRowSize(this.mDataCenter.getNumOfSwitches());
		switchPowerStat.addSample(this.getSwitchpower(), time);
	}

	@Override
	// FIXME: needs to be implemented
	public Constants.NodeState queryPowerState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void routePacket(double time, Experiment experiment, Packet packet,
			int portId) {
		// TODO Auto-generated method stub
		// FIXME: only needs port number, not port object here
		Port port = portMap.get(portId);
		if (port == null) {
			System.err.println("invalid port number for packet routing\n");
		}
		port.insertPacket(packet, time, false);
	}
	
	public void routeFlow(double time, Experiment experiment, Flow flow,
			int portId) {
		// TODO Auto-generated method stub
		// FIXME: only needs port number, not port object here
		Port port = portMap.get(portId);
		if (port == null) {
			System.err.println("invalid port number for packet routing\n");
		}
		port.insertFlow(flow, time, false);
	}

	public double getSwitchpower() {
		// calculate total number of ports that are turned on
		int totalPorts = 0;
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			totalPorts += this.linecardMap.get(i).getActivePortsNum();
		}

		// an array to store active port number for different Rates in all
		// linecards
		Vector<Integer> actportNos = new Vector<Integer>(NO_PORT_RATES);
		for (int i = 0; i < NO_PORT_RATES; i++) {
			int num = 0;
			for(int j = 1; j <= this.linecardMap.size(); j++) {
				if(this.linecardMap.get(j).getDifPortRates().get(i) != 0) {
					num += this.linecardMap.get(j).getDifPortRates().get(i);
				}
			}
			actportNos.add(num);
		}

		double portspower = totalPorts * Constants.POWER_ENABLEDPORT;

		for (int i = 0; i < NO_PORT_RATES; i++) {
			if (actportNos.get(i) != 0) {
				portspower += actportNos.get(i)
						* Port.availableSwitchPortPowers.get(i);
			}
		}

		// Get line card power
		double lcActivePower = 0;
		double lcLPI1Power = 0;
		double lcLPI2Power = 0;
		double lcLPI3Power = 0;
		
		if(getType() == Type.CORE) {
			lcActivePower = mDataCenter.getExpConfig().getCoreLcActivePower();
			lcLPI1Power = mDataCenter.getExpConfig().getCoreLcLPI1Power();
			lcLPI2Power = mDataCenter.getExpConfig().getCoreLcLPI2Power();
			lcLPI3Power = mDataCenter.getExpConfig().getCoreLcLPI3Power();
		}
		else if(getType() == Type.AGGREGATE) {
			lcActivePower = mDataCenter.getExpConfig().getAggregateLcActivePower();
			lcLPI1Power = mDataCenter.getExpConfig().getAggregateLcLPI1Power();
			lcLPI2Power = mDataCenter.getExpConfig().getAggregateLcLPI2Power();
			lcLPI3Power = mDataCenter.getExpConfig().getAggregateLcLPI3Power();
		}
		else if(getType() == Type.EDGE) {
			lcActivePower = mDataCenter.getExpConfig().getEdgeLcActivePower();
			lcLPI1Power = mDataCenter.getExpConfig().getEdgeLcLPI1Power();
			lcLPI2Power = mDataCenter.getExpConfig().getEdgeLcLPI2Power();
			lcLPI3Power = mDataCenter.getExpConfig().getEdgeLcLPI3Power();
		}
		else {
			Sim.fatalError("Unrecognized switch type");
		}
		
		// Get # of line cards in each state
		int numActiveLC = 0;
		int numLPI1LC = 0;
		int numLPI2LC = 0;
		int numLPI3LC = 0;
		
		Vector<LineCard> lineCards = getLineCards();
		
		for(LineCard lineCard : lineCards) {
			int currentState = lineCard.mSleepController.currentSleepState;
			
			switch(currentState) {
				case 0:  numActiveLC++;
						 break;
				case 1:  numLPI1LC++;
						 break;
				case 2:  numLPI2LC++;
						 break;
				case 3:  numLPI3LC++;
						 break;
				default: Sim.fatalError("Unexpected line card state");
						 break;
			}
		}
		
		switchpower = Constants.POWER_CHASIS + numActiveLC * lcActivePower + numLPI1LC * lcLPI1Power + numLPI2LC * lcLPI2Power + numLPI3LC * lcLPI3Power + portspower;
		return switchpower;
	}

	public void setSwitchpower(double switchpower) {
		this.switchpower = switchpower;
	}

	public int getnumOfActiveLineCards() {
		int num = 0;
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			if(this.linecardMap.get(i).getLinecardState() == LineCardState.ACTIVE) {
				num++;
			}
		}
		numOfActiveLineCards = num;
		return numOfActiveLineCards;
	}

	public void setnumOfActiveLineCards(int numOfActiveLineCards) {
		this.numOfActiveLineCards = numOfActiveLineCards;
	}
	
	/*
	 * Calculate the number of linecards in each linecard state and set corresponding
	 * variable
	 */
	public void calculateLineCardStates() {
		this.numOfActiveLineCards = 0;
		this.numOfSleepLineCards = 0;
		this.numOfOffLineCards = 0;
		for(int i = 1; i <= linecardMap.size(); i++) {
			if(linecardMap.get(i).getLinecardState() == LineCard.LineCardState.ACTIVE) {
				this.numOfActiveLineCards++;
			}
			else if(linecardMap.get(i).getLinecardState() == LineCard.LineCardState.SLEEP) {
				this.numOfSleepLineCards++;
			}
			else if(linecardMap.get(i).getLinecardState() == LineCard.LineCardState.OFF) {
				this.numOfOffLineCards++;
			}
		}
	}
	
	/*
	 * Calculate the number of ports in each port state and set corresponding variable
	 */
	public void calculatePortStates() {
		this.numOfActivePorts = 0;
		this.numOfLPIPorts = 0;
		for(int i = 1; i <= portMap.size(); i++) {
			if(portMap.get(i).getPortState() == Port.PortPowerState.idle) {
				this.numOfActivePorts++;
			}
			else if(portMap.get(i).getPortState() == Port.PortPowerState.LOW_POWER_IDLE) {
				this.numOfLPIPorts++;
			}
		}
	}
	
	public void addLinecard(int linecardId) {
		linecardIds.add(linecardId);
	}
	
	public int getNumOfLineCards() {
		return numOfLineCards;
	}

	public void setLinecardNo(int linecardNo) {
		this.numOfLineCards = linecardNo;
	}

	public int getNumOfActivePorts() {
		int num = 0;
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			num += this.linecardMap.get(i).getActivePortsNum();
		}
		numOfActivePorts = num;
		return numOfActivePorts;
	}

	public int getNumOfSleepLinecards(){
		int num = 0;
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			if(this.linecardMap.get(i).getLinecardState() == LineCardState.SLEEP) {
				num++;
			}
		}
		numOfSleepLineCards = num;
		return numOfSleepLineCards;
	}

	public void setNumOfActivePorts(int numOfActivePorts) {
		this.numOfActivePorts = numOfActivePorts;
	}
	public void setNumOfSleepLinecards(int numOfSleepLinecards) {
		this.numOfSleepLineCards = numOfSleepLinecards;
	}
	
	public int getnumOfLPIPorts(){
		numOfLPIPorts = this.portMap.size() - numOfActivePorts;
		return numOfLPIPorts;
	}
	
	public IActivityUnit getActivityUnit(){
		return activityUnit;
	}
	
	public Vector<LineCard> getLineCards() {
		// Create empty vector
		Vector<LineCard> lineCards = new Vector<LineCard>();
		
		// Add all linecards in switch to vector
		for(int i = 1; i <= this.linecardMap.size(); i++) {
			lineCards.add(this.linecardMap.get(i));
		}
		
		// Return vector
		return lineCards;
	}
	
	public SwitchSleepController getSwitchSleepController() {
		return this.switchSleepable;
	}
	
	public void unsetScheduledActive() {
		this.isScheduledActive = false;
		this.setUseMultipleSleepStates(true);
	}
	
	public void setUseMultipleSleepStates(boolean useMS){
		switchSleepable.setUseMultipleSleepState(useMS);
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}

	public Vector<Pair<Double, Integer>> getSSTrace() {
		return this.SSTrace;
	}
	public double getAverageSleepState(double totalTime) {
		double total = 0;
		double average=0;
		Vector<LineCard> lineCards = this.getLineCards();
		for(int p = 0; p < getNumOfLineCards(); p++) {
			SwitchSleepController switchSleepController = lineCards.get(p).getSleepController();
			total =0;
			total = total + switchSleepController.L0Time * 0;
			total = total + switchSleepController.L1Time * 1;
			total = total + switchSleepController.L3Time * 2;
			total = total + switchSleepController.L5Time * 3;
			total = total + switchSleepController.L6Time * 4;

		/*for (int i =0; i<SSTrace.size(); i++) {

			if(i<SSTrace.size()-1) {
				total = total + SSTrace.get(i).getSecond()*(SSTrace.get(i+1).getFirst()-SSTrace.get(i).getFirst());
			}
			else if(i==SSTrace.size()-1) {
				total = total + SSTrace.get(i).getSecond()*(totalTime - SSTrace.get(i).getFirst());
			}
			
		}
		*/
			average += (total / totalTime);
		}
		return average/getNumOfLineCards();
	}
	
	public int getSleepStateByTime(double time) {
		
		int j =1;
		while(j<SSTrace.size()) {

			if(SSTrace.get(SSTrace.size()-j).getFirst() < time) {
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

