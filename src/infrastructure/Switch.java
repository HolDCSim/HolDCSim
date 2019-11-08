package infrastructure;

import infrastructure.LineCard.LineCardState;
import infrastructure.Port.PortPowerState;

import java.util.*;
import java.util.Map.Entry;

import communication.Flow;
import communication.Packet;

import constants.Constants;
//import stat.RawStatLogger;
import stat.TimeWeightedStatistic;
import utility.Pair;
import experiment.Experiment;

public class Switch extends DCNode implements Routable {
	private static int PORT_PER_LINECARD = 8;
	private Vector<LineCard> lineCards;
	private int numOfLineCards;
	private int numOfActiveLineCards;
	private int numOfActivePorts;
	private double switchpower;
	private Vector<Integer> linecardIds;
	
	public Vector<Pair<Double, Integer>> SSTrace;
	public Double AverageSleepState;
	
	public static int NO_PORT_RATES = Port.availableRates.size();
	
	public void addLinecardMap (Map<Integer, LineCard> _linecardMap) {
		super.addLinecardMap(_linecardMap);
		int index = 0;
		//for (int i = 0; i < numOfLineCards; i++) {
			//this.addLinecard(++index);
		//}
	}

	public Switch(DataCenter dataCenter) {
		super(dataCenter);
		/**this.portUHistory = new HashMap<Integer, Vector<Vector<Double>>>();**/

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

	public double getSwitchpower() {

		// calculate total number of ports that are turned on
		int totalPorts = 0;
		double lcPower = 0.0;
		for (LineCard lc : lineCards) {
			totalPorts += lc.getActivePortsNum();
			lcPower += lc.getLcPower();
		}

		// an array to store active port number for different Rates in all
		// linecards
		Vector<Integer> actportNos = new Vector<Integer>(NO_PORT_RATES);
		for (int i = 0; i < NO_PORT_RATES; i++) {
			int num = 0;
			for (LineCard lc : lineCards) {
				if (lc.getDifPortRates().get(i) != 0) {
					num += lc.getDifPortRates().get(i);
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

		switchpower = Constants.POWER_CHASIS + lcPower + portspower;
		return switchpower;
	}

	public void setSwitchpower(double switchpower) {
		this.switchpower = switchpower;
	}

	public int getnumOfActiveLineCards() {
		int num = 0;
		for (LineCard lc : lineCards) {
			if (lc.getLinecardState() == LineCardState.ACTIVE) {
				num++;
			}
		}
		numOfActiveLineCards = num;
		return numOfActiveLineCards;
	}

	public void setnumOfActiveLineCards(int numOfActiveLineCards) {
		this.numOfActiveLineCards = numOfActiveLineCards;
	}

	public int getNumOfActivePorts() {
		int nap = 0;
		for (int i = 0; i < numOfLineCards; i++) {
			nap += lineCards.get(i).getActivePortsNum();
		}
		numOfActivePorts = nap;
		return numOfActivePorts;
	}

	public void addLinecard(int linecardId) {
		linecardIds.add(linecardId);
	}
	
	public int getLinecardNo() {
		numOfLineCards = linecardIds.size();
		return numOfLineCards;
	}

	public void setLinecardNo(int linecardNo) {
		this.numOfLineCards = linecardNo;
	}
	public void setNumOfActivePorts(int numOfActivePorts) {
		this.numOfActivePorts = numOfActivePorts;
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
	
	public Vector<Pair<Double, Integer>> getSSTrace() {
		return this.SSTrace;
	}

	public double getAverageSleepState() {
		int count = 0;
		double total = 0;
		for (Pair<Double,Integer> pair : SSTrace) {
			total = total + pair.getSecond();
			count = count + 1;
			
		}
		double average = total / (double)count;
		return average;
	}
}
