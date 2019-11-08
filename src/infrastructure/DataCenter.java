package infrastructure;

import infrastructure.Core.CorePowerPolicy;
import infrastructure.DelayOffController.OffMode;
import infrastructure.LineCard.LineCardPowerPolicy;
import infrastructure.LineCard.LineCardState;
import infrastructure.Port.PortPowerPolicy;
import infrastructure.Port.PortPowerState;
import infrastructure.Socket.SocketPowerPolicy;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

//import com.sun.xml.internal.ws.wsdl.parser.MexEntityResolver;

import constants.Constants;
//import constants.Constants.SleepUnit;
import debug.Sim;
import processor.CorePState;
import processor.IntelPentiumPState;
import stat.TimeWeightedStatistic;
import topology.AbstractTopologyGen;
import topology.BcubeGen;
import topology.CamcubeGen;
import topology.FBFLYGen;
import topology.FatTreeGen;
import topology.SmallTopologyGen;
import topology.TestFlowTopologyGen;
import topology.Topology;
import event.*;
import event.Event.EVENT_TYPE;
import experiment.Experiment;
import experiment.ExperimentConfig;
import experiment.LinecardNetworkExperiment;
import experiment.SingletonJobExperiment;
import job.Task;

/**
 * This class will hold all the physical objects in the datacenter for now.
 * 
 * 
 */
public final class DataCenter implements Serializable {

	/**
	 * The serialization id.
	 */
	private static final long serialVersionUID = 1L;

	public Topology mTopo;
	private Constants.TopoClass mTopoClass;
	private ExperimentConfig expConfig;
	// private AbstractTaskScheduler taskScheduler;
	/**
	 * The servers in the datacenter.
	 */
	private Vector<Server> servers = new Vector<Server>();
	private Vector<LPISwitch> switches = new Vector<LPISwitch>();
	// Mar13, Bing adds LCSwitch, linecard switches
	private Vector<LCSwitch> LCSwitches = new Vector<LCSwitch>();
	// Mar13, Bing
	private Vector<DCNode> nodes = new Vector<DCNode>();

	private Map<EVENT_TYPE, Long> eventsMap;

	// number of servers to be scheduled for current experiment
	// private int serversToSch = 0;

	public Experiment experiment;

	public int getNumOfServers() {
		return this.mTopo.getNumOfServers();
	}

	public int getNumOfSwitches() {
		return this.mTopo.getNumOfSwitches();
	}

	public ExperimentConfig getExpConfig() {
		return this.expConfig;
	}

	private Vector<Vector<Integer>> getNodeConnectivity() {
		return mTopo.getNodeConnectivity();
	}

	private Vector<Vector<Integer>> getNodePortsMapping() {
		return mTopo.getNodePortsMapping();
	}

	public DataCenter() {
		System.out.println("switchonly");
		// FIXME: current just set the default topology to camcube
		this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;

		// AbstractTopologyGen abGen = new SmallTopologyGen();

		AbstractTopologyGen abGen = new FatTreeGen(4, expConfig);

		mTopo = new Topology(abGen);

	}

	public DataCenter(ExperimentConfig expConfig) {

		this.expConfig = expConfig;

		// taskScheduler = expConfig.getTaskScheduler();
		// taskScheduler.setDataCenter(this);

		// serversToSch = expConfig.getServerToSchedule();
		AbstractTopologyGen abGen = null;
		eventsMap = new HashMap<EVENT_TYPE, Long>();
		HashMap<String, String> topoParams = expConfig.getTopoParams();
		//System.out.println(expConfig.getmTopo()+"getmTopo");  //original config: first executed, FLOWTEST
		switch (expConfig.getmTopo()) {
		case FAT_TREE:
			this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;

			if (topoParams.size() == Constants.FAT_TREE_PARAMS) {
				abGen = new FatTreeGen(Integer.parseInt(topoParams.get("k")), expConfig);
				System.out.println("create fat tree topology with k = "
						+ Integer.parseInt(topoParams.get("k")));
			} else {
				System.err.println("numbers of params incorrect for FAT_TREE");
			}
			break;
		case FBFLY:
			this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;
			if (expConfig.getTopoParams().size() == Constants.FBFLY_PARAMS) {
				abGen = new FBFLYGen(Integer.parseInt(topoParams.get("k")),
						Integer.parseInt(topoParams.get("d")));
				System.out.println("create FBFLY topology with k = "
						+ Integer.parseInt(topoParams.get("k")) + " d = "
						+ Integer.parseInt(topoParams.get("d")));
			} else {
				System.err.println("numbers of params incorrect for FBFLY");
			}
			break;

		case SMALL_TOPO:
			this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;
			abGen = new SmallTopologyGen();
			System.out.println("create simple small topology");
			break;

		case FLOWTEST:
			this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;
			abGen = new TestFlowTopologyGen();
			System.out.print("hahahahahaha");
			System.out.println("create test flow topology");
			break;
		case BCUBE:
			this.mTopoClass = Constants.TopoClass.HYBRID;
			if (topoParams.size() == Constants.BCUBE_PARAMS) {
				abGen = new BcubeGen(Integer.parseInt(topoParams.get("k")),
						Integer.parseInt(topoParams.get("n")));
				System.out.println("create bcube topology with k = "
						+ Integer.parseInt(topoParams.get("k")));
			} else {
				System.err.println("numbers of params incorrect for BCUBE");
			}
			break;

		case CAMCUBE:
			this.mTopoClass = Constants.TopoClass.SERVER_ONLY;
			if (topoParams.size() == Constants.CAMCUBE_PARAMS) {
				abGen = new CamcubeGen(Integer.parseInt(topoParams.get("k")));

				System.out.println("create camcube topology with k = "
						+ Integer.parseInt(topoParams.get("k")));
			}

			else {
				System.err.println("numbers of params incorrect for CAMCUBE");
			}
			break;

		case UNKNOWN:
			System.err.println("Topology is unknown");
			System.exit(0);
			break;

		}

		mTopo = new Topology(abGen);
	}

	// FIXME: topology should be able to be set from outside
	/**
	 * create data center object with topology initialized
	 * 
	 * @param topo
	 * @param numOfServers
	 */
	public DataCenter(Constants.TopoName topo, int numOfServers) {
		//System.out.println("topo"+topo);	//not executed
		switch (topo) {
		case FAT_TREE:
		case FBFLY:
		case SMALL_TOPO:
			this.mTopoClass = Constants.TopoClass.SWITCH_ONLY;
			break;
		case BCUBE:
			this.mTopoClass = Constants.TopoClass.HYBRID;
			break;
		case CAMCUBE:
			this.mTopoClass = Constants.TopoClass.SERVER_ONLY;
		default:
			break;
		}
	}

	public void initialize(Experiment aExperiment) {
		this.experiment = aExperiment;
		mTopo.initialize(expConfig.preloadPath());

		// fanyao added: set the core sleepstate waitings
		if (expConfig.getTaos() != null) {
			AbstractSleepController.setSleepStateWatings(expConfig.getTaos());
		}
		// fan yao added, Datacenter operations should be kept in DataCenter
		// class
		int nServers = getNumOfServers();//number of servers in topology
		System.out.println("Number of servers = " + nServers);
		//System.out.println("hhhhhhhhhhhhhhhhhhh"+nServers);
		//int sServers; // number of servers to be scheduled in experiment
		int num_of_sockets;
		int num_of_cores;
        
		//sServers = expConfig.getServersToSchedule();
		num_of_sockets = expConfig.getNumOfSockets();
		num_of_cores = expConfig.getCoresPerSocket();

		// setup wakeup latency
		// Constants.CORE_TRANSITION_TO_PAKR =
		// Core.sleepStateWakeups[expConfig.getSleepState() - 1];
		//prepare socket and cores 

		//add code to get the number of server really needed to be newed
		//int serversNeeded = nServers < sServers ? nServers : sServers; 
		for (int i = 0; i < nServers; i++) {

			CorePState pState = new IntelPentiumPState(null, null);
			pState.setFrenAndVol((float) (1.6 * expConfig.getSpeed()),
					(float) (1.6 * expConfig.getSpeed()));

			// pState.setFrenAndVol((float) (1.6 ), (float) (1.6) );
			Server server = null;
			if (mTopoClass == Constants.TopoClass.SWITCH_ONLY) {

				if (experiment instanceof SingletonJobExperiment) {
					SingletonJobExperiment sjExp = (SingletonJobExperiment) experiment;
					Socket[] msEESockets = new Socket[1];
					
					Vector<Core> cores = new Vector<Core>();
					for (int j = 0; j < (sjExp.numOfCores); j++) {
						Core core = new Core(experiment);
						cores.add(core);
					}
					msEESockets[0] = new Socket(aExperiment, cores);
					AbstractSleepController gsController = null;
				
					switch (experiment.getServerType()) {
					// case BASE_SERVER:
					// server = new Server(num_of_sockets, num_of_cores,
					// experiment, this);
					// break;
					case UNIPROCESSOR_SERVER:
						
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new GeneralSleepController(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new GeneralSleepController(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new UniprocessorServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					case SHALLOW_DEEP_SERVER:
	
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new GeneralSleepController(aExperiment);
							gsController.deepiestState = ShallowDeepStates.DEEP;
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new GeneralSleepController(experiment);
			                    	gsController.deepiestState = ShallowDeepStates.DEEP;
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new ShallowDeepServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					case EROVER_SERVER: {
	//					Socket[] msEESockets = new Socket[1];
	//					Core msEECore = new MultiStateCore(experiment, null);
	//					Vector<Core> msEECores = new Vector<Core>();
	//					msEECores.add(msEECore);
	//
	//					msEESockets[0] = new Socket(_experiment, null, msEECores);
	//					msEECore.setSocket(msEESockets[0]);
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL||
								sjExp.sleepUnit == Constants.SleepUnit.NO_SLEEP){
							gsController = new ERoverSleepController(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL ){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new ERoverSleepController(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new ERoverServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					}
					
					case DUAL_EROVER_SERVER: {
	//					Socket[] msEESockets = new Socket[1];
	//					Core msEECore = new MultiStateCore(experiment, null);
	//					Vector<Core> msEECores = new Vector<Core>();
	//					msEECores.add(msEECore);
	//
	//					msEESockets[0] = new Socket(_experiment, null, msEECores);
	//					msEECore.setSocket(msEESockets[0]);
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new DualERoverController(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new DualERoverController(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new ERoverServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					}
					case ON_OFF_SERVER: {
	//					Socket[] onOffSockets = new Socket[1];
	//					Core onOffCore = new OnOffCore(experiment, null);
	//					Vector<Core> onOffCores = new Vector<Core>();
	//					onOffCores.add(onOffCore);
	//
	//					onOffSockets[0] = new Socket(_experiment, null, onOffCores);
	//					onOffCore.setSocket(onOffSockets[0]);
	//
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new OnOffController(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new OnOffController(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new OnOffServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					}
					case DELAY_OFF_SERVER: {
	//					Socket[] delayOffSockets = new Socket[1];
	//					Core delayOffCore = new DelayOffCore(experiment, null);
	//					Vector<Core> delayOffCores = new Vector<Core>();
	//					delayOffCores.add(delayOffCore);
	//
	//					delayOffSockets[0] = new Socket(_experiment, null,
	//							delayOffCores);
	//					delayOffCore.setSocket(delayOffSockets[0]);
	
						// OnOffServer is used to reproduce the original delayoff
						// algorithm
	
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new DelayOffController(aExperiment, OffMode.TURNED_OFF);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new DelayOffController(experiment, OffMode.TURNED_OFF);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						server = new OnOffServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					}

					case ON_SLEEP_SERVER: {
	//					Socket[] onOffSockets = new Socket[1];
	//					Core onOffCore = new OnOffCore(experiment, null);
	//					Vector<Core> onOffCores = new Vector<Core>();
	//					onOffCores.add(onOffCore);
	//
	//					onOffSockets[0] = new Socket(_experiment, null, onOffCores);
	//					onOffCore.setSocket(onOffSockets[0]);
	
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new GeneralSleepController(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new OnOffController(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						
						server = new UniprocessorServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
						break;
					}
					case DELAY_DOZE_SERVER: {
	//					Socket[] delayOffSockets = new Socket[1];
	//					Core delayOffCore = new DelayOffCore(experiment, null);
	//					Vector<Core> delayOffCores = new Vector<Core>();
	//					delayOffCores.add(delayOffCore);
	//
	//					delayOffSockets[0] = new Socket(_experiment, null,
	//							delayOffCores);
	//					delayOffCore.setSocket(delayOffSockets[0]);
	
						// use SingleCoreServer instead
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new DelayOffController(aExperiment,OffMode.SLEEP);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new DelayOffController(experiment, OffMode.SLEEP);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						
						server = new UniprocessorServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
	
						break;
					}
				
					case DELAY_DOZE_SERVER2: {
	//					Socket[] delayOffSockets = new Socket[1];
	//					Core delayOffCore = new DelayOffCore(experiment, null);
	//					Vector<Core> delayOffCores = new Vector<Core>();
	//					delayOffCores.add(delayOffCore);
	//
	//					delayOffSockets[0] = new Socket(_experiment, null,
	//							delayOffCores);
	//					delayOffCore.setSocket(delayOffSockets[0]);
	
						// use SingleCoreServer instead
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new DelayDoze2Controller(aExperiment);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new DelayDoze2Controller(experiment);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						
						server = new UniprocessorServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
	
						break;
					}
				
					case DUAL_DELAY_SERVER: {
	//					Socket[] delayOffSockets = new Socket[1];
	//					Core delayOffCore = new DelayOffCore(experiment, null);
	//					Vector<Core> delayOffCores = new Vector<Core>();
	//					delayOffCores.add(delayOffCore);
	//
	//					delayOffSockets[0] = new Socket(_experiment, null,
	//							delayOffCores);
	//					delayOffCore.setSocket(delayOffSockets[0]);
	
						// use SingleCoreServer instead
						if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
							gsController = new DualDelayOffController(aExperiment,OffMode.SLEEP);
							msEESockets[0].setSleepController(gsController);
						}
						else if(sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL){
							 for(int k =0; k < sjExp.numOfCores; k++){
			                    	gsController = new DualDelayOffController(experiment, OffMode.SLEEP);
			                    	cores.get(k).setSleepController(gsController);
			
			                    }
						}
						
						server = new UniprocessorServer(experiment, this, msEESockets, gsController, sjExp.sleepUnit);
						//msEESockets[0].setServer(server);
	
						break;
					}
					default:
						Sim.fatalError("fatal error: unknown type of server to create");
	
					}
					
					if (sjExp.sleepUnit == Constants.SleepUnit.CORE_LEVEL) {
						server.setSocketPolicy(SocketPowerPolicy.NO_MANAGEMENT);
						server.setCorePolicy(CorePowerPolicy.CORE_SLEEP);
					}
					
					else if(sjExp.sleepUnit == Constants.SleepUnit.SOCKET_LEVEL){
						server.setSocketPolicy(SocketPowerPolicy.SOCKET_SLEEP);
						server.setCorePolicy(CorePowerPolicy.SYNC_TO_SOCKET);
					} else if(sjExp.sleepUnit == Constants.SleepUnit.NO_SLEEP) {
						
						server.setSocketPolicy(SocketPowerPolicy.NO_MANAGEMENT);
						server.setCorePolicy(CorePowerPolicy.SYNC_TO_SOCKET);
					}
				}
			} else {
				server = new HybridServer(num_of_sockets, num_of_cores,
						experiment, this);
			}
			/* set server level dvfs */
			server.setDvfsSpeed(0.0, expConfig.getSpeed());
			// Server server = new PowerNapServer(sockets, cores, experiment,
			// arrivalGenerator, serviceGenerator, 0.001, 5);


			/***************************************************/
			// fanyao commnented: power model from bighouse is not commented
			// double coreActivePower = 40 * (4.0 / 5) / num_of_cores;
			// double coreHaltPower = coreActivePower * .2;
			// double coreParkPower = 0;
			//
			// double socketActivePower = 40 * (1.0 / 5) / num_of_sockets;
			// double socketParkPower = 0;
			//
			// server.setCoreActivePower(coreActivePower);
			//
			// // frequency and voltage is set here
			// server.setCorePState(pState);
			// server.setCoreParkPower(coreParkPower);
			// server.setCoreIdlePower(coreHaltPower);
			//
			// server.setSocketActivePower(socketActivePower);
			// server.setSocketParkPower(socketParkPower);
			/***************************************************/

			int serverPortNum = mTopo.getServerPortNum(i);
			//System.out.println("ServerPortNum"+serverPortNum);
			Map<Integer, Port> portMap = new HashMap<Integer, Port>();
			for (int j = 0; j < serverPortNum; j++) {
				Port serverPort = new Port(server, expConfig.getPortRate(),
						Constants.PORT_BUFFER_SIZE_SERVER);
				if(expConfig.getPortPowerPolicy() == PortPowerPolicy.NO_MANAGEMENT) {
					serverPort.setPortState(PortPowerState.idle);
				}
				else if(expConfig.getPortPowerPolicy() == PortPowerPolicy.Port_LPI) {
					serverPort.setInPortState(PortPowerState.LOW_POWER_IDLE);
				}
				else {
					System.out.println("Unrecognized Port Power Policy: " + expConfig.getPortPowerPolicy());
				}
				portMap.put(j + 1, serverPort);

			}

			server.addPortMap(portMap);
			// enforcer.addServer(server);
			server.setIds(i + 1, i + 1 + getNumOfSwitches());
			addServer(server);
			// TODO fanyao add a super class for server and switch

		}// End for i
		
		SwitchSleepController switchSleepController = null;

		Vector<Integer> coreSwitches = mTopo.getCoreSwitches();
		Vector<Integer> aggregateSwitches = mTopo.getAggregateSwitches();
		Vector<Integer> edgeSwitches = mTopo.getEdgeSwitches();
		
		int nSwitches = getNumOfSwitches();
		for (int i = 0; i < nSwitches; i++) {
			// Vector<Vector<Integer>> switchOut = this.getNodePortsMapping();
			// Map<Integer, Port> portMap = new HashMap<Integer, Port>();
			// Vector<Integer> adjacentPorts = switchOut.get(i);

			//Map<Integer, Port> portMap = new HashMap<Integer, Port>();
			//LPISwitch _switch = new LPISwitch(this);
			
			//Mar13, Bing adds LCSwitch
			Map<Integer, Port> portMap = new HashMap<Integer, Port>();
			
			switchSleepController = new SwitchSleepController(aExperiment);
			
			LCSwitch __switch = new LCSwitch(this, switchSleepController);
			switchSleepController.setSwitch(__switch);
			//Mar13, Bing


			Map<Integer, LineCard> linecardMap = new HashMap<Integer, LineCard>();
			int switchPortNum = mTopo.getSwitchPortNum(i);
			//System.out.println("switchPortNum"+switchPortNum);
			for (int j = 0; j < switchPortNum; j++) {

				//Port switchPort = new Port(_switch, expConfig.getPortRate(),
						//Constants.PORT_BUFFER_SIZE_SWITCH);
				
				//Mar13, Bing adds LCSwitch
				Port LCSwitchPort = new Port(__switch, expConfig.getPortRate(), Constants.PORT_BUFFER_SIZE_SWITCH);
				if(expConfig.getPortPowerPolicy() == PortPowerPolicy.NO_MANAGEMENT) {
					LCSwitchPort.setPortState(PortPowerState.idle);
				}
				else if(expConfig.getPortPowerPolicy() == PortPowerPolicy.Port_LPI) {
					LCSwitchPort.setInPortState(PortPowerState.LOW_POWER_IDLE);
				}
				else {
					System.out.println("Unrecognized Port Power Policy: " + expConfig.getPortPowerPolicy());
				}
				portMap.put(j + 1, LCSwitchPort);
				//Mar13, Bing
			}
			
			for (int m = 0; m < switchPortNum / expConfig.getPortsPerLineCard(); m++) {
				LineCard linecard = new LineCard(__switch);

				SwitchSleepController lineCardSwitchSleepController = new SwitchSleepController(aExperiment);
				linecard.setSleepController(lineCardSwitchSleepController);
				lineCardSwitchSleepController.setSwitch(__switch);

				linecardMap.put(m+1, linecard);
			}
			
			//Mar13, Bing adds LCSwitch
			__switch.addPortMap(portMap);
			__switch.setIds(i + 1, i + 1);
			__switch.addLinecardMap(linecardMap);
			__switch.setLineCardPolicy(expConfig.getLineCardPowerPolicy());
			__switch.setPortPolicy(expConfig.getPortPowerPolicy());
			addSwitch(__switch);
			//Mar13, Bing
			
			// Set type of switch according to fat tree topology
			int switchID = __switch.getNodeId();
			if(coreSwitches.contains(switchID)) {
				__switch.setType(LCSwitch.Type.CORE);
			}
			else if(aggregateSwitches.contains(switchID)) {
				__switch.setType(LCSwitch.Type.AGGREGATE);
			}
			else if(edgeSwitches.contains(switchID)) {
				__switch.setType(LCSwitch.Type.EDGE);
			}
			else {
				Sim.fatalError("Switch type not found.");
			}
		}

		nodes.addAll(LCSwitches);
		nodes.addAll(servers);
		//update neighbor port information of all ports
		for (int i = 0; i < nodes.size(); i++) {
			for(int p=1;p<=nodes.get(i).portMap.size();p++){
				//System.out.println(nodes.get(i).portMap.get(p));
				nodes.get(i).portMap.get(p).updateConnectedPort();
			}
		}
		if(this.experiment instanceof LinecardNetworkExperiment || this.experiment instanceof LinecardNetworkExperiment){
			//this.expConfig.setServersToSchedule(7*nServers/8);
			this.expConfig.setServersToSchedule(nServers);
			this.expConfig.setInitialJobsToSkip(nServers*this.expConfig.initialJobsToSkip());
		}
		

	}

	/**
	 * Adds a server to the datacenter.
	 * 
	 * @param server
	 *            - the server to add
	 */
	public void addServer(final Server _server) {
		this.servers.add(_server);
	}

	//public void addSwitch(final LPISwitch _switch) {
	//	this.switches.add(_switch);
	//}
	
	//Mar13, Bing adds for LCSwitch
	public void addSwitch(final LCSwitch _switch) {
		this.LCSwitches.add(_switch);
	}
	//Mar13, Bing

	/**
	 * Gets the servers in the datacenter.
	 * 
	 * @return the server in the datacenter
	 */
	public Vector<Server> getServers() {
		return this.servers;
	}

	public Vector<LPISwitch> getSwitches() {
		return this.switches;
	}
	
	//Mar13, Bing adds for LCSwitch
	public Vector<LCSwitch> getLCSwitches() {
		return this.LCSwitches;
	}

	/**
	 * Updates the statistics of all the objects in the datacenter.
	 * 
	 * @param time
	 *            - the time the statistics are updated
	 */
	public void updateStatistics(final double time) {
		// Iterator<Server> iter = this.servers.iterator();
		double tT = 0.0;
		// while (iter.hasNext()) {
		// Server iServer = iter.next();
		// iServer.updateStatistics(time);
		// // do not use consecutive next();
		// tT += iServer.getThroughput();
		//
		// }
		//
		for (Server aServer : this.servers) {
			aServer.updateStatistics(time);
			tT += aServer.getThroughput();
		}

		TimeWeightedStatistic totalthroughput = this.experiment.getStats()
				.getTimeWeightedStat(
						Constants.TimeWeightedStatName.TOTAL_THROUGHPUT);
		totalthroughput.addSample(tT, time);

		//Iterator<LPISwitch> iter_switch = this.switches.iterator();
		Iterator<LCSwitch> iter_switch = this.LCSwitches.iterator();
		while (iter_switch.hasNext()) {
			iter_switch.next().updateStatistics(time);
		}
	}

	public void updateStatistics(AbstractEvent event) {
		// Iterator<Server> iter = this.servers.iterator();
		double tT = 0.0;

		double time = event.getTime();

		if (event.getEventType() == EVENT_TYPE.SERVER_EVENT) {
			for (Server aServer : this.servers) {
				aServer.updateStatistics(time);
				tT += aServer.getThroughput();
			}
		}

		/**
		 * TimeWeightedStatistic totalthroughput = this.experiment.getStats()
		 * .getTimeWeightedStat(
		 * Constants.TimeWeightedStatName.TOTAL_THROUGHPUT);
		 * totalthroughput.addSample(tT, time);
		 **/
		else {
			//for (LPISwitch aSwitch : this.switches) {
			//	aSwitch.updateStatistics(time);
			//}
			for (LCSwitch aSwitch : this.LCSwitches) {
				aSwitch.updateStatistics(time);
			}
		}

		EVENT_TYPE eventEnum = event.getEventType();
		if (eventsMap.get(EVENT_TYPE.ALL_EVENT) == null) {
			eventsMap.put(EVENT_TYPE.ALL_EVENT, 0L);
		}
		eventsMap.put(EVENT_TYPE.ALL_EVENT,
				eventsMap.get(EVENT_TYPE.ALL_EVENT) + 1);

		if (eventsMap.get(eventEnum) == null) {
			eventsMap.put(eventEnum, 1L);
		} else {
			Long eventNum = eventsMap.get(eventEnum);
			eventNum++;
			eventsMap.put(eventEnum, eventNum);
		}

	}

	public Map<EVENT_TYPE, Long> getEventsMap() {
		return this.eventsMap;
	}

	/**
	 * get the actual node object based on node index
	 * 
	 * @param matrixNodeId the index of the node (two sets, first switches and 
	 * then servers)
	 * @return
	 */
	public DCNode getDCNodeById(int matrixNodeId) {
		// TODO Auto-generated method stub
		if (matrixNodeId < 0 || matrixNodeId > mTopo.getTotalNodeNo())
			return null;
		else
			return nodes.get(matrixNodeId - 1);
	}
	
	public DCNode getServerById(int serverId) {
		// TODO Auto-generated method stub
		if (serverId < 1 || serverId > servers.size()){
			Sim.fatalError("getServerById out of bound");
			return null;
		}
		else
			return servers.get(serverId - 1);
	}
	
	public DCNode getLCSwitchById(int switchId) {
		if (switchId < 1 || switchId > LCSwitches.size()) {
			Sim.fatalError("getLCSwitchById out of bound");
			return null;
		}
		else {
			return LCSwitches.get(switchId - 1);
		}
	}


	public String getNodeTypeName(int matrixNodeId) {
		// TODO Auto-generated method stub
		return matrixNodeId <= getNumOfSwitches() ? "switch" : "server";

	}

	public Server scheduleTask(Task task, double time, boolean isDependingTask) {
		return experiment.dispatchTask(task, time, isDependingTask);
	}
	
	public Vector<Server> scheduleTaskHeuristic(Vector<Task> tasks, double time, boolean isDependingTask) {
		Vector<Server> assignedServers = new Vector<Server>();
		
		// Assign each task to a server
		for(int i = 0; i < tasks.size(); i++) {
			assignedServers.add(experiment.dispatchTask(tasks.get(i), time, isDependingTask));
		}
		
		return assignedServers;
	}
	
	/**
	 * statistics for single server experiment only get the results from the
	 * first server in the first queue
	 * 
	 * @return
	 */
	public int getJobsInQueue() {
		// TODO Auto-generated method stub
		return servers.get(0).queue.size();
	}

	/**
	 * statistics for single server experiment only get the results from the
	 * first server in the first queue
	 * 
	 * @return
	 */
	public double[] getSSTimeArray() {
		double[] sleepStateTimes = new double[AbstractSleepController.sleepStateWakeups.length];
		AbstractSleepController sleepController = ((EnergyServer) (servers
				.get(0))).getSleepController();
		sleepStateTimes[0] = sleepController.C0S0Time;
		sleepStateTimes[1] = sleepController.C1S0Time;
		sleepStateTimes[2] = sleepController.C3S0Time;
		sleepStateTimes[3] = sleepController.C6S0Time;
		sleepStateTimes[4] = sleepController.C6S3Time;

		return sleepStateTimes;
	}

	public int[] getSSCountsArray() {
		int[] sleepStateCounts = new int[AbstractSleepController.sleepStateWakeups.length];
		AbstractSleepController sleepController = ((EnergyServer) (servers
				.get(0))).getSleepController();
		sleepStateCounts[0] = sleepController.C0S0Count;
		sleepStateCounts[1] = sleepController.C1S0Count;
		sleepStateCounts[2] = sleepController.C3S0Count;
		sleepStateCounts[3] = sleepController.C6S0Count;
		sleepStateCounts[4] = sleepController.C6S3Count;

		return sleepStateCounts;
	}

}
