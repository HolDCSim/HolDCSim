package topology;

import experiment.LinecardNetworkExperiment;
import infrastructure.*;
import network.Djikstra;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import constants.Constants;
import debug.Sim;
import utility.ServerPowerMeter;

//import utility.AbstractTopologyGenertor;
//import utility.FBFLYGenerator;

public class Topology {
	
	public static enum NetworkRoutingAlgorithm {
		ELASTIC_TREE,
		DJIKSTRA,
		POPCORNS,
		WASP
	}

	// private NodesInterconnection topology_tree;
	private DataCenter mDataCenter;
	private int numOfServers;
	private int numOfSwitches;
	private int totalNodeNo;

	// private Routing mRouting;

	// ////////////////////////////////////////////////////////

	private Vector<Integer> Predecessor;
	private Vector<Vector<Integer>> nodeConnectivity;
	private Vector<Vector<Integer>> nodePortsMapping;
	private Vector<Vector<Vector<Integer>>> cachedPaths;
	private AbstractTopologyGen abGen;
	private String[][] pathRecord;
	
	private BufferedWriter weightMatrixWriter;
	private boolean initializeBufferedWriter = false;

	// ///////////////////////////////////////////////////////
	public int getTotalNodeNo() {
		return totalNodeNo;
	}

	public void setTopologyGenertor(AbstractTopologyGen _abGen) {
		this.abGen = _abGen;
	}

	public Vector<Vector<Integer>> getNodePortsMapping() {
		return abGen.getNodePortsMapping();
	}

	public Vector<Vector<Integer>> getNodeConnectivity() {
		return abGen.getNodeConnectivity();
	}
	
	public Vector<Vector<Double>> getInitialWeightMatrix() {
		return abGen.getInitialWeightMatrix();
	}
	
	public class Node  
	{  
	    public String name = null; 
	    public int index = 0;
	    public ArrayList<Node> relationNodes = new ArrayList<Node>();  
	  
	    public String getName() {  
	        return name;  
	    }  
	  
	    public void setName(String name) {  
	        this.name = name;  
	    }  
	    
	    public void setIndex(int index) {
	        this.index = index;  
	    }
	    
	    public int getIndex() {  
	        return index;  
	    }
	  
	    public ArrayList<Node> getRelationNodes() {  
	        return relationNodes;  
	    }  
	  
	    public void setRelationNodes(ArrayList<Node> relationNodes) {  
	        this.relationNodes = relationNodes;  
	    }  
	}

	/**
	 * in current design the rows for server connectivity are 
	 * located below the rows of switches
	 * @param i
	 * @return
	 */
	public int getServerPortNum(int i) {
		int serverRowIndex = numOfSwitches + i;
		int num = 0;
		if (nodeConnectivity == null) {
			System.err.println("nodePortsMapping not initialized");
			// FIXME: should throw an exception here
			System.exit(0);
		}
		Vector<Integer> serverOut = nodePortsMapping.get(serverRowIndex);
		for (int port : serverOut) {
			if (port != -1) {
				num++;
			}
		}

		return num;
	}

	public int getSwitchPortNum(int i) {
		// switch id begins with 0
		int switchRowIndex = i;
		int num = 0;
		if (nodeConnectivity == null) {
			System.err.println("nodePortsMapping not initialized");
			// FIXME: should throw an exception here
			System.exit(0);
		}
		Vector<Integer> switchOut = nodePortsMapping.get(switchRowIndex);
		for (int port : switchOut) {
			if (port != -1) {
				num++;
			}
		}

		return num;
	}

	public Topology(AbstractTopologyGen _abGen) {
		this.abGen = _abGen;
		cachedPaths = null;
	}

	public void initialFromXML() {
		// TO read from XML with the definition of a topology

	}

	public int getNumOfServers() {
		return numOfServers;
	}

	public void setNumOfServers(int numOfServers) {
		this.numOfServers = numOfServers;
	}

	public int getNumOfSwitches() {
		return numOfSwitches;
	}

	public void setNumOfSwitches(int numOfSwitches) {
		this.numOfSwitches = numOfSwitches;
	}

	public DataCenter getmDataCenter() {
		return mDataCenter;
	}

	public void setmDataCenter(DataCenter mDataCenter) {
		this.mDataCenter = mDataCenter;
	}

	// public Routing getmRouting() {
	// return mRouting;
	// }
	//
	// public void setmRouting(Routing mRouting) {
	// this.mRouting = mRouting;
	// }
	
	public String[][] getPathRecord() {
		return pathRecord;
	}
	
	public String getPath(int src, int dst) {
		return pathRecord[src][dst];
	}

	public Vector<Vector<Double>> updateWeightMatrix_energy(Vector<Vector<Integer>> nodeConnectivity, Vector<Vector<Double>> weightMatrix) {
		// Data structure used for network routing
		Djikstra djikstra = abGen.djikstra;

		int qos = mDataCenter.getExpConfig().getJobQoS();

		double avgFlowSize = mDataCenter.getExpConfig().getAvgFlowSize();

		double edgeSwitchBW = mDataCenter.getExpConfig().getEdgeSwitchBW(); // Edge switch has max BW for a flow Use for QoS calculation

		double qosTimeConstraint = qos * (avgFlowSize / edgeSwitchBW);

		//TODO: SHould be depedent on the fat tree size.
		double avgHopCount = 10;

		double minBWPerFlow = avgFlowSize / qosTimeConstraint;

		Vector<Vector<Double>> newWeightMatrix = weightMatrix;	//copy nodeConnectivity and modify later
		for (int i = 0; i < totalNodeNo; i++) {
			for (int j = 0; j < totalNodeNo; j++) {
				if (nodeConnectivity.get(i).get(j) == 10) {    // i and j are  connected
					double weight = 0;

					if (j >= numOfSwitches) {// j corresponds to a server
						int portId = 1;
						DCNode server = mDataCenter.getServerById(j - numOfSwitches + 1);
//						Port port = server.getPortById(portId);
//						if (port.getPortState() == Port.PortPowerState.LOW_POWER_IDLE) {
//							weight = Constants.POWER_ACTIVE_PORT - Constants.POWER_LPI_PORT;
//						} else if (port.getPortState() == Port.PortPowerState.OFF) {
//							weight = Constants.POWER_ACTIVE_PORT;
//						} else {
//							weight = 0;
//						}
						//TODO: add wakeup energy
						switch (((ERoverServer) server).getCurrentSleepState()) {
							case 0:
								break;
							case 1:
								weight += (ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1, 0) - ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1,1)) * ((avgFlowSize / edgeSwitchBW)  + mDataCenter.getExpConfig().getuBar());
								break;
							case 4:
								weight += (ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1, 0) - ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1,4)) * ((avgFlowSize / edgeSwitchBW) + mDataCenter.getExpConfig().getuBar()); ;
								break;
							case 5:
                                    weight += (ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1, 0) - ServerPowerMeter.calculateLowPower(mDataCenter.getExpConfig(),1,5)) * ((avgFlowSize / edgeSwitchBW) + mDataCenter.getExpConfig().getuBar()); ;;
								break;
						}
					} else {        // j corresponds to a switch
						//System.out.println("i"+i+" "+"j"+j);
						int switchId = j + 1;
						int portId = nodePortsMapping.get(i).get(j);        // port id;
						int portsPerLineCard = mDataCenter.getExpConfig().getPortsPerLineCard();
						int linecardId = (portId - 1) / portsPerLineCard + 1;

						DCNode lcswitch = mDataCenter.getLCSwitchById(switchId);
						Port port = lcswitch.getPortById(portId);
						LineCard linecard = lcswitch.getLinecardById(linecardId);


						// Get line card power
						double lcPower= 0;
						double lcActivePower=0;


						if (lcswitch instanceof LCSwitch) {
							if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.CORE) {
								lcActivePower = mDataCenter.getExpConfig().getCoreLcActivePower();
								switch (linecard.getSleepController().getCurrentSleepState()) {
									case 0: lcPower = 0;
											break;
									case 1: lcPower = lcActivePower - mDataCenter.getExpConfig().getCoreLcLPI1Power();
											break;
									case 2: lcPower = lcActivePower - mDataCenter.getExpConfig().getCoreLcLPI2Power();
										break;
									case 3: lcPower = lcActivePower - mDataCenter.getExpConfig().getCoreLcLPI3Power();
								}

							} else if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.AGGREGATE) {
								lcActivePower = mDataCenter.getExpConfig().getAggregateLcActivePower();
								switch (linecard.getSleepController().getCurrentSleepState()) {
									case 0: lcPower = 0;
										break;
									case 1: lcPower = lcActivePower - mDataCenter.getExpConfig().getAggregateLcLPI1Power();
										break;
									case 2: lcPower = lcActivePower - mDataCenter.getExpConfig().getAggregateLcLPI2Power();
										break;
									case 3: lcPower = lcActivePower - mDataCenter.getExpConfig().getAggregateLcLPI3Power();
								}
							} else if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.EDGE) {
								lcActivePower = mDataCenter.getExpConfig().getEdgeLcActivePower();
								switch (linecard.getSleepController().getCurrentSleepState()) {
									case 0: lcPower = 0;
										break;
									case 1: lcPower = lcActivePower - mDataCenter.getExpConfig().getEdgeLcLPI1Power();
										break;
									case 2: lcPower = lcActivePower - mDataCenter.getExpConfig().getEdgeLcLPI2Power();
										break;
									case 3: lcPower = lcActivePower - mDataCenter.getExpConfig().getEdgeLcLPI3Power();
								}
							} else {
								Sim.fatalError("Unrecognized switch type");
							}

						} else {
							Sim.fatalError("LCSwitch expected");
						}


						if (i < numOfSwitches) {    //If Link is from Switch to Switch
//							int qos = mDataCenter.getExpConfig().getJobQoS();
//							double avgFlowSize = mDataCenter.getExpConfig().getAvgFlowSize();
//
//							double edgeSwitchBW = mDataCenter.getExpConfig().getEdgeSwitchBW(); // Edge switch has max BW for a flow Use for QoS calculation
//
//							double qosTimeConstraint = qos * (avgFlowSize / edgeSwitchBW);
//
//							double minBWPerFlow = avgFlowSize / qosTimeConstraint;
//
							DCNode linkstart = getmDataCenter().getDCNodeById(i + 1);
							DCNode linkend = getmDataCenter().getDCNodeById(j + 1);

							//weight+= lcPower * (avgFlowSize / edgeSwitchBW);

							if (mDataCenter.getExpConfig().getExperiment() instanceof LinecardNetworkExperiment) {
								Link alink = ((LinecardNetworkExperiment) mDataCenter.getExpConfig().getExperiment()).getFlowController().checkLinkBW(linkstart, linkend);

								if (alink != null) {

									if (alink.getNumOfFlows() > 0) {
										weight+=((LinecardNetworkExperiment) mDataCenter.getExpConfig().getExperiment()).getFlowController().additionalAwakeTimeforSwitch(alink) * lcActivePower;
									}
									else {
										weight+= avgFlowSize/edgeSwitchBW * lcActivePower;
									}

								} else {
									weight+= avgFlowSize/edgeSwitchBW * lcActivePower;
								}
							}


						}
					}

					// newWeightMatrix is returned to find optimal servers to schedule tasks on according to PopCorns algorithm
					newWeightMatrix.get(i).set(j, weight);

					// Djikstra weights are updated for network routing
					djikstra.updateWeightPopCorns(i + 1, j + 1, weight);
				}
			}
		}

		return newWeightMatrix;
	}
	public Vector<Vector<Double>> updateWeightMatrix(Vector<Vector<Integer>> nodeConnectivity, Vector<Vector<Double>> weightMatrix) {
		// Data structure used for network routing
		Djikstra djikstra = abGen.djikstra;
		
		Vector<Vector<Double>> newWeightMatrix = weightMatrix;	//copy nodeConnectivity and modify later
		for (int i = 0; i < totalNodeNo; i++) {
			for (int j = 0; j < totalNodeNo; j++) {
				if (nodeConnectivity.get(i).get(j) == 10) {    // i and j are  connected
					double weight = -1;
					
					if (j >= numOfSwitches) {// j corresponds to a server
						int portId = 1;
						DCNode server = mDataCenter.getServerById(j - numOfSwitches + 1);
						Port port = server.getPortById(portId);
						if (port.getPortState() == Port.PortPowerState.LOW_POWER_IDLE) {
							weight = Constants.POWER_ACTIVE_PORT - Constants.POWER_LPI_PORT;
						} else if (port.getPortState() == Port.PortPowerState.OFF) {
							weight = Constants.POWER_ACTIVE_PORT;
						} else {
							weight = 0;
						}
						switch (((ERoverServer) server).getCurrentSleepState()) {
							case 0:
								break;
							case 1:
								weight += 80;
								break;
							case 4:
								weight += 140;
								break;
							case 5:
								weight += 150;
								break;
						}
					} else {        // j corresponds to a switch
						//System.out.println("i"+i+" "+"j"+j);
						int switchId = j + 1;
						int portId = nodePortsMapping.get(i).get(j);        // port id;
						int portsPerLineCard = mDataCenter.getExpConfig().getPortsPerLineCard();
						int linecardId = (portId - 1) / portsPerLineCard + 1;
						weight = 10;
						DCNode lcswitch = mDataCenter.getLCSwitchById(switchId);
						Port port = lcswitch.getPortById(portId);
						LineCard linecard = lcswitch.getLinecardById(linecardId);


						// Get line card power
						double lcActivePower = 0;
						double lcLPI1Power = 0;

						if (lcswitch instanceof LCSwitch) {
							if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.CORE) {
								lcActivePower = mDataCenter.getExpConfig().getCoreLcActivePower();
								lcLPI1Power = mDataCenter.getExpConfig().getCoreLcLPI1Power();
							} else if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.AGGREGATE) {
								lcActivePower = mDataCenter.getExpConfig().getAggregateLcActivePower();
								lcLPI1Power = mDataCenter.getExpConfig().getAggregateLcLPI1Power();
							} else if (((LCSwitch) lcswitch).getType() == LCSwitch.Type.EDGE) {
								lcActivePower = mDataCenter.getExpConfig().getEdgeLcActivePower();
								lcLPI1Power = mDataCenter.getExpConfig().getEdgeLcLPI1Power();
							} else {
								Sim.fatalError("Unrecognized switch type");
							}
						} else {
							Sim.fatalError("LCSwitch expected");
						}
						
						if (i < numOfSwitches) {    //If Link is from Switch to Switch
//							int qos = mDataCenter.getExpConfig().getJobQoS();
//							double avgFlowSize = mDataCenter.getExpConfig().getAvgFlowSize();
//
//							double edgeSwitchBW = mDataCenter.getExpConfig().getEdgeSwitchBW(); // Edge switch has max BW for a flow Use for QoS calculation
//
//							double qosTimeConstraint = qos * (avgFlowSize / edgeSwitchBW);
//
//							double minBWPerFlow = avgFlowSize / qosTimeConstraint;
//
//							DCNode linkstart = getmDataCenter().getDCNodeById(i + 1);
//							DCNode linkend = getmDataCenter().getDCNodeById(j + 1);
//
//							if (mDataCenter.getExpConfig().getExperiment() instanceof LinecardNetworkExperiment) {
//								Link alink = ((LinecardNetworkExperiment) mDataCenter.getExpConfig().getExperiment()).getFlowController().checkLinkBW(linkstart, linkend);
//
//								if (alink != null) {
//
//									double BW_allocated = alink.getNumOfFlows() * minBWPerFlow;
//									if ((alink.linkRate - BW_allocated) < (minBWPerFlow * 1.2)) {
//										weight += 10000;
//									}
//								}
//							}

							if (linecard.getLinecardState() == LineCard.LineCardState.SLEEP) {
								weight += lcActivePower - lcLPI1Power + Constants.POWER_ACTIVE_PORT - Constants.POWER_LPI_PORT;
								switch (linecard.getSleepController().getCurrentSleepState()) {
									case 2:
										weight += 100;
										break;
									case 3:
										weight += 240;
										break;
								}


							} else if (linecard.getLinecardState() == LineCard.LineCardState.ACTIVE && port.getPortState() == Port.PortPowerState.LOW_POWER_IDLE) {
								weight += Constants.POWER_ACTIVE_PORT - Constants.POWER_LPI_PORT;
							} else if (linecard.getLinecardState() == LineCard.LineCardState.OFF) {
								weight += lcActivePower + Constants.POWER_ACTIVE_PORT;
							}
						}
					}
					
					// newWeightMatrix is returned to find optimal servers to schedule tasks on according to PopCorns algorithm
					newWeightMatrix.get(i).set(j, weight);
					
					// Djikstra weights are updated for network routing
					djikstra.updateWeightPopCorns(i + 1, j + 1, weight);
				}
			}
		}
		
		return newWeightMatrix;
	}

	public static double[] ShellSort(double[] array) {
        // TODO Auto-generated method stub
        double temp = 0.0;
        int j;

        for(int gap = array.length/2;gap>0;gap/=2){
            for(int i = gap;i<array.length;i++){
                temp = array[i];
                for(j = i-gap;j>=0;j-=gap){
                    if(temp<array[j]){
                        array[j+gap] = array[j];
                    }
                    else {
                        break;
                    }
                }
                array[j+gap] = temp;
            }
        }
        return array;
    }

	public int findMinWeightIndex(double[] shortPath, int start, Vector<Integer> availServers) {	// exclude the start

		int index = numOfSwitches;
		double min = Double.MAX_VALUE;
		for (int i = numOfSwitches; i != start && i < shortPath.length && availServers.contains(i); i++) {
			if (i != start && shortPath[i] < min) {
				min = shortPath[i];
				index = i;
			}
		}
		return index;
	}

	public double[] Dijsktra(Vector<Vector<Double>> weightMatrix,int start, Vector<Integer> availServers){
           int n = totalNodeNo;
           double[] shortPath = new double[n];
           String[] path=new String[n];
           HashMap<Integer, Vector<Integer>> paths_list = new HashMap<Integer, Vector<Integer>>();
           //Vector<Vector<Integer>> path = new Vector<Vector<Integer>>();
            for(int i=0;i<n;i++)  {
                path[i]=new String(start+"-->"+i);
             //   path2.addElement((new Vector<Integer>()));
                Vector<Integer> vec = new Vector<Integer>();
                vec.add(start+1);
       //        path2.add(vec);
                paths_list.put(i, vec);
                //path2.set(i,vec);
            }
           int[] visited = new int[n];
           shortPath[start] = 0;
           visited[start] = 1;

           double[][] weight = new double[weightMatrix.size()][weightMatrix.size()];
           for (int i = 0; i < weightMatrix.size(); i++) {
        	   for (int j = 0; j < weightMatrix.size(); j++) {
        		   weight[i][j] = weightMatrix.get(i).get(j);
        	   }
           }

           for(int count = 1;count <= n - 1;count++)
           {

               int k = -1;
               double dmin = Double.MAX_VALUE;
               for(int i = 0;i < n;i++)
               {
                   if(visited[i] == 0 && weight[start][i] < dmin)
                   {
                       dmin = weight[start][i];

                       k = i;
                   }

               }
              // System.out.println("k="+k);

               shortPath[k] = dmin;

               visited[k] = 1;
               // System.out.println("k="+k);
               if (count == 1) {
     //          	System.out.println("Adding " + k + " to " + k + " --before Paths :" +paths_list);
               	paths_list.get(k).add(k+1);
     //          	System.out.println("Adding " + k + " to " + k + " --before Paths :" +paths_list);
               }

               for(int i = 0;i < n;i++)
               {
                   if(visited[i] == 0 && weight[start][k] + weight[k][i] < weight[start][i]){
                	   //weightMatrix.elementAt(start).set(i, weightMatrix.get(start).get(k) + weightMatrix.get(k).get(i));
                	   weight[start][i] = weight[start][k] + weight[k][i];
                        path[i]=path[k]+"-->"+i;
                	   //Vector<Integer> tmp = path.get(k);
                	   //tmp.add(i);
                        //path.setElementAt(tmp, i);
                   //     System.out.println("Adding " + k + " to " + i + " Before Paths :" +paths_list);
                        Vector<Integer> vec = new Vector<Integer>(paths_list.get(k));
                        vec.add(i+1);
                    //    path2.set( i, new Vector<Integer>(vec));
                      paths_list.put(i, vec);
                //      System.out.println("After Paths :" +paths_list);
                	   //Vector<Integer> tmp = path.get(k);
                	   //tmp.add(i);
                        //path.setElementAt(tmp, i);
                   }

               }
           }

           int end = numOfSwitches;
           HashMap<Double, ArrayList<Integer>> m = new HashMap<Double, ArrayList<Integer>>();
           for (int j = 0; j < shortPath.length; j++) {
        	   // Initialize new list if 1st time key
        	   if(!m.containsKey(shortPath[j])) {
        		   m.put(shortPath[j], new ArrayList<Integer>());
        	   }

        	   // Append server matrix node to entry and store in map
        	   ArrayList<Integer> newEntry = m.get(shortPath[j]);
        	   newEntry.add(j);
               m.put(shortPath[j], newEntry);
           }

           // List is sorted in ascending order of server path length
           double[] list = ShellSort(shortPath);

           for(int z = 0; z < list.length; z++) {
        	   ArrayList<Integer> serversList = m.get(list[z]);
        	   for(int i = 0; i < serversList.size(); i++) {
        		   if(serversList.get(i) == start || !availServers.contains(serversList.get(i))) {
        			   continue;
        		   }
        		   else {
        			   end = serversList.get(i);
					   double[] result = new double[] {shortPath[end], start, end};
        			   return result;
        		   }
        	   }

        	   z += serversList.size() - 1;
           }

           double[] result = new double[] {shortPath[end], start, end};
           pathRecord[(int)end][(int)start] = path[(int)end];
           System.out.println("path record"+ pathRecord[(int)start][(int)end]);
           return result;
       }

	/**
	 * this method could calculate the Dijkstra Length between two nodes
	 * Predecessor(contains the order of nodes from src to dst)
	 *
	 * @return: Dist (denotes the Dijkstra Length from src to dst)
	 * @author: jingxin
	 */
	private int DijkstraLength(int src, int dst) {
		int oo = 10000;
		Vector<Boolean> Tight = new Vector<Boolean>(totalNodeNo);
		Vector<Integer> Dist = new Vector<Integer>(totalNodeNo);

		Predecessor = new Vector<Integer>(totalNodeNo);
		for (int j = 0; j < totalNodeNo; j++) {
			Tight.add(false); // Tight[j]=false;
			Dist.add(oo);
			Predecessor.add(-1);
		}
		Dist.set(src, 0);
		int i, k;
		for (k = 0; k < totalNodeNo; k++) {
			// Pick the next closest node
			int p;
			for (p = 0; p < totalNodeNo; p++) {
				if (!Tight.get(p))
					break;
			}
			int u = p;
			p = p + 1;
			while (p < totalNodeNo) {
				if (!Tight.get(p) && (Dist.get(p) < Dist.get(u))) {
					u = p;
				}
				p++;
			}
			// p--;
			Tight.set(u, true);

			if (u == dst) {
				break;
			} else {
				for (i = 0; i < totalNodeNo; i++) {
					if (!Tight.get(i)
							&& ((Dist.get(u) + nodeConnectivity.get(u).get(i)) < Dist
									.get(i))) {
						Dist.set(i, Dist.get(u)
								+ nodeConnectivity.get(u).get(i));
						Predecessor.set(i, u);
					}
				}
			}
		}
		return Dist.get(dst);
	}

	/**
	 * find the shortest paths between any pair of nodes in the network. Path:
	 * stores all the routes, Path[i][j] is a vector which means the routes from
	 * node i to node j
	 * 
	 * @author: jingxin
	 */
	private void findSingleShortestPath() {

		cachedPaths = new Vector<Vector<Vector<Integer>>>(totalNodeNo);
		for (int x = 0; x < totalNodeNo; x++) {
			Vector<Vector<Integer>> lst = new Vector<Vector<Integer>>(
					totalNodeNo);
			for (int y = 0; y < totalNodeNo; y++) {
				Vector<Integer> lst2 = new Vector<Integer>(totalNodeNo);
				for (int z = 0; z < totalNodeNo; z++) {
					lst2.add(0);
				}
				lst.add(lst2);
			}
			cachedPaths.add(lst);
		}

		int i, j, p, u;
		for (i = 0; i < totalNodeNo; i++) {
			Vector<Vector<Integer>> lst = new Vector<Vector<Integer>>(
					totalNodeNo);
			// ArrayList<ArrayList<Integer>> lst1=new
			// ArrayList<ArrayList<Integer>>(TotalNodeNo+1);
			for (j = 0; j < totalNodeNo; j++) {
				Vector<Integer> lst2 = new Vector<Integer>(totalNodeNo);
				Vector<Integer> lst3 = new Vector<Integer>(totalNodeNo);
				// int length=DijkstraLength(i,j);
				//DijkstraLength(i, j);
				// System.out.println("Dist:"+DijkstraLength(i,j));
				p = 0;
				for (u = j; u != i; u = Predecessor.get(u)) {
					if (u == -1) {
						// can't find primary path
						break;
					} else {
						lst2.add(u + 1);
						p++;
					}
				}// end u
				lst2.add(i + 1);
				// lst1.set(i,lst2);
				int ss;
				// int m=0;
				for (ss = p; ss != 0; ss--) {
					lst3.add(lst2.get(ss));// Path[i][j][m]=Path[j][i][ss];
				}
				lst3.add(j + 1); // Path[i][j][m]=j;
				lst.add(lst3);
			}// end for j
			cachedPaths.set(i, lst);
		}// end i
		System.out.println("cccccccccccccccccccccc"+cachedPaths);
		//System.out.println("cccccccccccccccccccccc"+cachedPaths.get(1));
		//System.out.println("cccccccccccccccccccccc"+cachedPaths.get(33));
		//System.out.println("cccccccccccccccccccccc"+cachedPaths.get(1).get(33));
	}

	/**
	 * this method calculates the routes between two servers src and des
	 * 
	 * @return routeBetweenServers
	 * @author: jingxin
	 */
	public Vector<Vector<Integer>> routeBetweenServers(int src, int des) {
		// Each Vector<Integer> has 3 elements. 1st element: Server/switch matrix id. 2nd element: Input port. 3rd element: Output port.
		// If there is not input port (e.g.: src server), 2nd element is -1
		// If there is no output port (e.g.: dst server), 3rd element is -1
		Vector<Vector<Integer>> routeBetweenServers = new Vector<Vector<Integer>>();
		Vector<Integer> rr = null;
		
		NetworkRoutingAlgorithm networkRoutingAlgorithm = this.mDataCenter.getExpConfig().getNetworkRoutingAlgorithm();
		
		// Calculate shortest path using Djikstra's algorithm
		Djikstra djikstra = abGen.djikstra;
			
		// Calculate weight matrix according to current power state of servers/switches
		if(networkRoutingAlgorithm == networkRoutingAlgorithm.POPCORNS) {
			updateWeightMatrix_energy(getNodeConnectivity(), getInitialWeightMatrix());
		}
			
		djikstra.computePaths(src, des, false);
		rr = djikstra.getShortestPathTo(des, src);
		
		int n = rr.size(); // number of nodes that the path from server src to
		// des contains
		// source server(first element is server id, second is -1, third is
		// output port of this server to the next switch)
		Vector<Integer> lst0 = new Vector<Integer>(3);
		lst0.add(src);
		lst0.add(-1);
		lst0.add(nodePortsMapping.get(src - 1).get(rr.get(1) - 1));
		routeBetweenServers.add(lst0);
		
		for (int i = 1; i < n - 1; i++) {
		// the first element is the switch id
		// the second element is the input port, the third element is the
		// output port
		Vector<Integer> lst = new Vector<Integer>(3);
		int nodec = rr.get(i);// current node
		lst.add(nodec);
		int nodea = rr.get(i - 1);// node
															// ahead
		int nodeb = rr.get(i + 1);// node
															// behind
		lst.add(nodePortsMapping.get(nodec - 1).get(nodea - 1));
		lst.add(nodePortsMapping.get(nodec - 1).get(nodeb - 1));
		routeBetweenServers.add(lst);
		}
		// des server(first element is server id, second is input port of this
		// server from the last switch, third is -1)
		Vector<Integer> lstlast = new Vector<Integer>(3);
		lstlast.add(des);
		lstlast.add(nodePortsMapping.get(des - 1).get(rr.get(n - 2) - 1));
		lstlast.add(-1);
		routeBetweenServers.add(lstlast);
		
		return routeBetweenServers;
	}

	public Vector<Vector<Integer>> getSinglePathRoute(int src, int dst) {
		if (src == dst) {
			//System.out.println("src task" + src + " ---> dst task" + dst
			//		+ " are scheduled on the same node");
			System.out.println("src task ---> dst task"
					+ " are scheduled on the same node");
			Vector<Vector<Integer>> directPath = new Vector<Vector<Integer>>();
			Vector<Integer> oneHop = new Vector<Integer>();
			oneHop.add(src + numOfSwitches);
			oneHop.add(0);
			oneHop.add(0);
			directPath.add(oneHop);
			System.out.println(directPath.toString());
			return directPath;
		}
		// since src and dst are the server node id begining from 1
		// we need to adjust this by an offset
		// System.out.println("routes between " + (src + numOfSwitches) + " : "
		// + (dst + numOfSwitches));
		// System.out.println(routeBetweenServers(src + numOfSwitches, dst
		// + numOfSwitches));
		return routeBetweenServers(src + numOfSwitches, dst + numOfSwitches);
	}

	public void initialize(boolean preloadPath) {
		// TODO Auto-generated method stub
		if (abGen == null) {
			System.out.println("Topology generator not set, use SmallTopology");
			abGen = new SmallTopologyGen();
		}
		setNumOfServers(abGen.getNumOfServers()); // Set number of servers and
													// switches
		setNumOfSwitches(abGen.getNumOfSwitches());
		totalNodeNo = abGen.getTotalNodeNo();
		pathRecord = new String[totalNodeNo][totalNodeNo];

		this.nodeConnectivity = abGen.getNodeConnectivity();
		this.nodePortsMapping = abGen.getNodePortsMapping();
		//System.out.println("Nodeconnectivity" + nodeConnectivity);
		// System.out.println("NodePortsMatrix: " + nodePortsMapping);
		if (preloadPath) {
			findSingleShortestPath();
		}
		else{
			Sim.debug(3, "QAQ: paths between pairs are not pre-loaded");
		}
		// Vector<Vector<Vector<Integer>>> routes = generateSinglePathRoutes();
		// System.out.println("routes between nodes:" + routes);
		// Vector<Vector<Integer>> routeBetweenServers = getSinglePathRoute(1,
		// 3);
		// System.out.println("routeBetweenServers 1 and 4:"+routeBetweenServers);

	}
	
	public Vector<Integer> getCoreSwitches() {
		if(abGen instanceof FatTreeGen) {
			return ((FatTreeGen) abGen).getCoreSwitches();
		}
		
		Sim.fatalError("Error getting core switches.");
		
		return null;
	}
	
	public Vector<Integer> getAggregateSwitches() {
		if(abGen instanceof FatTreeGen) {
			return ((FatTreeGen) abGen).getAggregateSwitches();
		}
		
		Sim.fatalError("Error getting aggregate switches.");
		
		return null;
	}

	public Vector<Integer> getEdgeSwitches() {
		if(abGen instanceof FatTreeGen) {
			return ((FatTreeGen) abGen).getEdgeSwitches();
		}
		
		Sim.fatalError("Error getting edge switches.");
		
		return null;
	}
	
	public AbstractTopologyGen getAbGen() {
		return abGen;
	}
}
