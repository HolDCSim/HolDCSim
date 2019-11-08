package infrastructure;

import java.util.*;
import java.util.Map.Entry;

import communication.Flow;
import communication.Packet;

import constants.Constants;
//import stat.RawStatLogger;
import stat.TimeWeightedStatistic;
import experiment.Experiment;

public class LPISwitch extends DCNode implements Routable {

	private int numOfActivePorts;//number of ports transmitting data
	private int numOfLPIPorts;//number of ports in LPI state
	private double switchpower;
	

	public static int NO_PORT_RATES = Port.availableRates.size();

	// temp code for port utilization
	/**private Map<Integer, Vector<Vector<Double>>> portUHistory;**/

	// private int nodeId;


	public LPISwitch(DataCenter dataCenter) {
		super(dataCenter);
		/**this.portUHistory = new HashMap<Integer, Vector<Vector<Double>>>();**/
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
		getNumOfActivePorts();
		getnumOfLPIPorts();
		double portspower = numOfActivePorts * Constants.POWER_ACTIVE_PORT+numOfLPIPorts*Constants.POWER_LPI_PORT;
		switchpower = Constants.POWER_SWITCH_BASE+portspower;
		return switchpower;
	}

	public void setSwitchpower(double switchpower) {
		this.switchpower = switchpower;
	}

	public int getNumOfActivePorts() {
		int nap = 0;
		Iterator<Entry<Integer, Port>> it = portMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Port> pairs =  it.next();
			Port port = (Port) pairs.getValue();

			if ((port.getInPortState() != Port.PortPowerState.LOW_POWER_IDLE)
					&& (port.getOutPortState() != Port.PortPowerState.LOW_POWER_IDLE)) {
				nap=nap+1;
			}
		}
		numOfActivePorts=nap;
		return numOfActivePorts;
	}
	public int getnumOfLPIPorts(){
		numOfLPIPorts=this.portMap.size()-numOfActivePorts;
		return numOfLPIPorts;
	}

	public void setNumOfActivePorts(int numOfActivePorts) {
		this.numOfActivePorts = numOfActivePorts;
	}
	public void setNumOfLPIPorts(int numOfLPIPorts) {
		this.numOfLPIPorts = numOfLPIPorts;
	}

	public AbstractLinkSleepController getSleepController() {
		// TODO Auto-generated method stub
		return null;
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

}
