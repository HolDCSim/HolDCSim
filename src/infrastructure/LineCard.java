package infrastructure;

import event.LineCardEnterSleepEvent;
import infrastructure.Port.PortPowerPolicy;
import infrastructure.Port.PortPowerState;

import java.util.*;

import constants.Constants;
import constants.Constants.PortRate;
import debug.Sim;
import event.LineCardTransitiontoSleepEvent;
import event.LineCardWakeupEvent;

public class LineCard implements Powerable {
	//active: transmit packets, sleep: low power idle
	public static enum LineCardState {
		TRANSITION_TO_ACTIVE, SLEEP, TRANSITION_TO_SLEEP, OFF, ACTIVE
	}
	
	/**
	 * Available power management policies for the linecard.
	 */
	public static enum LineCardPowerPolicy {
		/**
		 * No power management, linecard is always active.
		 */
		NO_MANAGEMENT,

		/**
		 * Linecard will transition through power states
		 */
		LINECARD_SLEEP
	};
	
	/**
	 * the SleepController
	 */
	protected SwitchSleepController mSleepController = null;
	
	/**
	 * The policy used by the linecard for power management.
	 */
	private LineCardPowerPolicy powerPolicy;
	
	private LineCardTransitiontoSleepEvent nextLinecardSleepEvent;
	
	private double linecardSleepTime;
	private double sleepStartTime;
	private double sleepEndTime;
	
	private double offStartTime;
	private double offEndTime;
	private double linecardOffTime;

	private Vector<Integer> portIds;
	private int portNo;
	private int activeportNo;
	/**
	 * an array stored numbers of active ports for each port rate initialized by
	 * setting the first element the number of active ports (because all the
	 * ports are initialized to be in the first port state)
	 */
	private Vector<Integer> portRatesCollection;
	private LCSwitch node = null;
	private LineCardState linecardState;
	
	/* Linecard minimum wakeup and sleep times for different link speeds, 100Mbps, 1000Mbps, 10Gbps */
	public static double[] sleepStateWakeups = { 30.5e-6, 16.5e-6, 4.48e-6};
	public static double[] sleepStateWaitings = { 200e-6, 182e-6, 2.88e-6};
	
	private double L0StartTime;
	private double L0EndTime;
	private double L0Time;
	private double L1StartTime;
	private double L1EndTime;
	private double L1Time;
	private double L3StartTime;
	private double L3EndTime;
	private double L3Time;
	private double L6StartTime;
	private double L6EndTime;
	private double L6Time;
	private double OffTime;
	private double lcPower;

	public LineCard(LCSwitch lcSwitch) {
		portIds = new Vector<Integer>();
		this.node = lcSwitch;
		linecardState = LineCardState.SLEEP;
		portRatesCollection = new Vector<Integer>(LCSwitch.NO_PORT_RATES);
		linecardSleepTime = 0.0;
		for (int i = 0; i < LCSwitch.NO_PORT_RATES; i++) {
			portRatesCollection.add(0);
		}
		
		// initialize time
		this.L0StartTime = 0.0;
		this.L0EndTime = 0.0;
		this.L0Time = 0.0;
		this.L1StartTime = 0.0;
		this.L1EndTime = 0.0;
		this.L1Time = 0.0;
		this.L3StartTime = 0.0;
		this.L3EndTime = 0.0;
		this.L3Time = 0.0;
		this.L6StartTime = 0.0;
		this.L6EndTime = 0.0;
		this.L6Time = 0.0;
		this.OffTime = 0.0;
		this.lcPower = 0.0;
	}
	
	public double getLcPower() {
		return this.lcPower;
	}
	
	// calculate line card power
	public void calLcPower() {
		this.lcPower = Constants.POWER_LINECARD_L0*this.L0Time + Constants.POWER_LINECARD_L1*this.L1Time
				+ Constants.POWER_LINECARD_L3*this.L3Time + Constants.POWER_LINECARD_L6*this.L6Time;
	}

	public void addPort(int portId) {
		portIds.add(portId);
	}

	public int getportNo() {
		portNo = portIds.size();
		return portNo;
	}

	public void setportNo(int portNo) {
		this.portNo = portNo;
	}

	public int getActivePortsNum() {
		activeportNo = portIds.size();
		if (linecardState == LineCardState.OFF) {
			activeportNo = 0;
		}

		return activeportNo;
	}

	public void setActiveportNo(int activeportNo) {
		this.activeportNo = activeportNo;
	}

	@Override
	public double getPower() {
		return 0;
	}
	
	/**
	 * Set the power management policy of the ports in the linecard.
	 */
	public void setPortPolicy(final PortPowerPolicy portPowerPolicy) {
		Vector<Port> ports = new Vector<Port>();
		
		for(int i = 1; i <= this.node.portMap.size(); i++) {
			ports.add(this.node.portMap.get(i));
		}
		Iterator<Port> iter = ports.iterator();
		while(iter.hasNext()) {
			Port port = iter.next();
			port.setPowerPolicy(portPowerPolicy);
		}
	}

	// TODO: implementation is needed
	public void setPortRate(int portId, PortRate _portRate) {
		this.node.getPortById(portId).setPortRate(_portRate);
	}

	// TODO: implementation is needed
	// public void setPortStatus(int portId, PortState ps) {
	// this.node.getPortById(portId).setPortState(ps);
	// }

	public LineCardState getLinecardState() {
		return linecardState;
	}

	/**
	 * @author jingxinwu
	 * @param linecardstate
	 *            this could be used in the controller to turn on/off the
	 *            linecard
	 */
	public void setLinecardstate(LineCardState linecardState) {
		this.linecardState = linecardState;
	}

	public Vector<Integer> getDifPortRates() {
		// initialization

		for (int i = 0; i < LCSwitch.NO_PORT_RATES; i++) {
			int nm = 0;
			for (int p : portIds) {
				// fanyao modified
				// FIXME: now just assume when either direction is busy, the
				// port is busy
				if ((this.node.getPortById(p).getInPortState() == PortPowerState.busy || this.node
						.getPortById(p).getOutPortState() == PortPowerState.busy)
						&& (this.node.getPortById(p).getPortRate() == Port.availableRates
								.get(i))) {
					nm++;
				}
			}
			portRatesCollection.set(i, nm);

		}
		return portRatesCollection;
	}

	/**
	 * @author jingxin
	 * @param activeportNosfordifRates
	 *            this could be used in the controller to do rate adaptation
	 */
	public void setPortRatesCollection(
			Vector<Integer> portRates) {
		this.portRatesCollection = portRates;
	}
	
	/**
	 * Set the power management policy of the linecard.
	 */
	public void setPowerPolicy(final LineCardPowerPolicy policy) {
		this.powerPolicy = policy;
		
		if(powerPolicy == LineCardPowerPolicy.NO_MANAGEMENT) {
			this.linecardState = LineCardState.ACTIVE;
		}
		else if(powerPolicy == LineCardPowerPolicy.LINECARD_SLEEP) {
			this.linecardState = LineCardState.SLEEP;
			
			if (mSleepController != null)
				mSleepController.initialController();
			else{
				Sim.fatalError("null sleepcontroller for linecard with LINECARD_SLEEP mode");
			}
		}
	}
	
	/**
	 * Get the power management policy of the linecard.
	 */
	public LineCardPowerPolicy getPowerPolicy() {
		return this.powerPolicy;
	}
	
	public void setSleepController(SwitchSleepController switchController){
		this.mSleepController = switchController;
	}
	
	public SwitchSleepController getSleepController(){
		return this.mSleepController;
	}
	
	public void TransitiontoSleep(final double time){
		this.linecardState = LineCardState.TRANSITION_TO_SLEEP;
	}
	
	public void enterSleep(final double time){
		this.linecardState = LineCardState.SLEEP;
		setSleepStartTime(time);
	}
	
	public void TransitiontoActive(){
		this.linecardState = LineCardState.TRANSITION_TO_ACTIVE;
	}
	
	// Line card exits SLEEP or OFF state
	public void exitSleep(final double time) {
		SwitchSleepController switchSleepController = node.getSwitchSleepController();
		
		if(switchSleepController.getCurrentSleepState() == switchSleepController.getDeepestSleepState()) {
			// Line card exits OFF state
			this.setOffEndTime(time);
		}
		else {
			// LineCard exits SLEEP state
			this.setSleepEndTime(time);
		}
	}
	
	public void updatePortsInOutState(LineCardState linecardState){
//		switch (linecardState) {
//			case TRANSITION_TO_SLEEP:
//				for (int p : portIds) {
//					this.node.getPortById(p).setPortState(PortPowerState.TRANSITION_TO_LOW_POWER_IDLE);
//				}
//				break;
//			case SLEEP:
//				for (int p : portIds) {
//					this.node.getPortById(p).setPortState(PortPowerState.LOW_POWER_IDLE);
//				}
//				break;
//		    case TRANSITION_TO_ACTIVE:
//		    	for (int p : portIds) {
//					this.node.getPortById(p).setPortState(PortPowerState.TRANSITIONG_TO_ACTIVE);
//				}
//				break;
//			case ACTIVE:
//				for (int p : portIds) {
//					this.node.getPortById(p).setPortState(PortPowerState.busy);
//				}
//				break;
//			case OFF:
//				activeportNo = 0;
//				break;
//		}
	}
	
	public LineCardTransitiontoSleepEvent getNextLinecardSleepEvent() {
		return nextLinecardSleepEvent;
	}

	public void setNextLinecardSleepEvent(LineCardTransitiontoSleepEvent nextLinecardSleepEvent) {
		this.nextLinecardSleepEvent = nextLinecardSleepEvent;
	}
	
	private LineCardWakeupEvent nextWakeUpevent;
	
	public LineCardWakeupEvent getNextWakeUpevent() {
		return nextWakeUpevent;
	}
	
	public void setNextWakeUpevent(LineCardWakeupEvent nextWakeUpevent){
		this.nextWakeUpevent = nextWakeUpevent;
	}
	
	public double getLinecardSleepTime() {
		// Line card was in SLEEP state when experiment ended
		if(sleepEndTime < sleepStartTime) {
			sleepEndTime = this.node.getDataCenter().experiment.getCurrentTime();
			updateLinecardSleepTime();
		}
		
		return linecardSleepTime;
	}

	public void updateLinecardSleepTime() {
		this.linecardSleepTime += sleepEndTime - sleepStartTime;
	}

	public double getSleepStartTime() {
		return sleepStartTime;
	}

	public void setSleepStartTime(double sleepStartTime) {
		this.sleepStartTime = sleepStartTime;
	}

	public double getSleepEndTime() {
		return sleepEndTime;
	}

	public void setSleepEndTime(double sleepEndTime) {
		this.sleepEndTime = sleepEndTime;
		updateLinecardSleepTime();
	}
	
	public void enterOff(final double time){
		this.linecardState = LineCardState.OFF;
		setOffStartTime(time);
	}
	
	public double getLinecardOffTime() {
		// Line card was in OFF state when experiment ended
		if(offEndTime < offStartTime) {
			offEndTime = this.node.getDataCenter().experiment.getCurrentTime();
			updateLinecardOffTime();
		}
		
		return linecardOffTime;
	}

	public void updateLinecardOffTime() {
		this.linecardOffTime += offEndTime - offStartTime;
	}

	public double getOffStartTime() {
		return offStartTime;
	}

	public void setOffStartTime(double offStartTime) {
		this.offStartTime = offStartTime;
	}

	public double getOffEndTime() {
		return offEndTime;
	}

	public void setOffEndTime(double offEndTime) {
		this.offEndTime = offEndTime;
		
		this.updateLinecardOffTime();
	}

//	public boolean isLineCardActive() {
//		boolean isActive = false;
//
//		int i;
//		for (i = this.linecardId*experiment.getExpConfig().getPortsPerLineCard()+1; i < (linecardId+1)*experiment.getExpConfig().getPortsPerLineCard(); i++) {
//			Port portwithin = mNode.getPortById(i);
//			if (portwithin.getPortState()==Port.PortPowerState.LOW_POWER_IDLE || portwithin.getPortState()==Port.PortPowerState.TRANSITION_TO_LOW_POWER_IDLE) {
//				continue;
//			} else {
//				break;
//			}
//		}
//		if (i >= (linecardId+1)* experiment.getExpConfig().getPortsPerLineCard()) {
//			double tslc = 0.0;
//			tslc = LineCard.sleepStateWakeups[1];
//			linecard.TransitiontoSleep(time);
//			linecard.setNextLinecardSleepEvent(null);
//			LineCardEnterSleepEvent enterSleepEvent = new LineCardEnterSleepEvent(time, mNode.getDataCenter().experiment, mNode, linecardId);
//			mNode.getDataCenter().experiment.addEvent(enterSleepEvent);
//		}
//
//		for (int portid : portIds) {
//
//
//
//
//			Port port = node.getPortById(portid+1);
//			if (port.getInPortState() == PortPowerState.idle && port.getOutPortState() == PortPowerState.idle) {
//				continue;
//			} else {
//				isActive = true;
//				break;
//			}
//
//		}
//
//		return isActive;
//	}
}

