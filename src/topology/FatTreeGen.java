package topology;
import java.util.Arrays;
import java.util.Vector;

import debug.Sim;
import experiment.ExperimentConfig;
import network.Djikstra;
import topology.Topology.NetworkRoutingAlgorithm;

/**
 * this class could generate fat tree architecture
 * @author jingxin
 *
 */
public class FatTreeGen extends AbstractTopologyGen{
	/**
	 * @param k is the parameter for fat tree topology
	 */
	private int k;		
	private Vector<Vector<Integer>> switchConnectivity;
	private Vector<Vector<Integer>> serverSwitchMapping;
	private Vector<Vector<Integer>> switchPortMapping;
	private Vector<Integer> serverList;
	private Vector<Integer> switchList;
	private Vector<Vector<Double>> weightMatrix;
	int oo = 10000;
	
	private Vector<Integer> coreSwitches, aggregateSwitches, edgeSwitches;
	private double coreSwitchBW, aggregateSwitchBW, edgeSwitchBW;
	
	private NetworkRoutingAlgorithm networkRoutingAlgorithm;
	
	public FatTreeGen(int npod, ExperimentConfig expConfig){
		this.k = npod;
		switchConnectivity = new Vector<Vector<Integer>>();
		serverSwitchMapping = new Vector<Vector<Integer>>();
		switchPortMapping = new Vector<Vector<Integer>>();
		
		coreSwitches = new Vector<Integer>();
		aggregateSwitches = new Vector<Integer>();
		edgeSwitches = new Vector<Integer>();
		
		coreSwitchBW = expConfig.getCoreSwitchBW();
		aggregateSwitchBW = expConfig.getAggregateSwitchBW();
		edgeSwitchBW = expConfig.getEdgeSwitchBW();
		
		networkRoutingAlgorithm = expConfig.getNetworkRoutingAlgorithm();
		
		numOfSwitches = (5*k*k)/4;
		numOfServers = (k*k*k)/4;
		totalNodeNo = numOfSwitches+numOfServers;
		nodeConnectivity=new Vector<Vector<Integer>>(totalNodeNo);
		nodePortsMapping=new Vector<Vector<Integer>>(totalNodeNo);
		serverList = new Vector<Integer>();
		//switch id: pod switches: 1~k^2; core switches: k^2+1~k^2+(k/2)^2 
		switchList = new Vector<Integer>();	
		weightMatrix = new Vector<Vector<Double>>();
		
		djikstra = new Djikstra(totalNodeNo, expConfig);
		
		for(int p=0;p<k;p++){		
			//lower level switches
			for (int i=0; i<k/2; i++){				
				switchList.add(i+1+p*k);
				edgeSwitches.add(i+1+p*k);
				Vector<Integer> lst0 = new Vector<Integer>(numOfSwitches);
				Vector<Integer> lst1 = new Vector<Integer>(numOfSwitches);
				for(int j=0; j<numOfSwitches; j++){
					lst0.add(oo); //first initiate all elements of switchConnectivity to oo
					lst1.add(-1);//first initiate all elements of switchPortMapping to -1
				}				
				lst0.set(i+p*k, 0); //the path length from the switch to itself is set to be 0	
				int u=k/2+1;
				//the port id begins from k/2+1 since there are already k/2 servers connected to the switch
				for(int m=p*k+k/2; m<p*k+k; m++){						
					//the lower level switch is connected to all upper level switches in that pod
					lst0.set(m, 10);  
					lst1.set(m, u);
					u++;
				}
				switchConnectivity.add(lst0);	
				switchPortMapping.add(lst1);
			}			
			//upper level switches
			for (int i=k/2; i<k; i++){
				switchList.add(i+1+p*k);
				aggregateSwitches.add(i+1+p*k);
				Vector<Integer> lst0 = new Vector<Integer>(numOfSwitches);
				Vector<Integer> lst1 = new Vector<Integer>(numOfSwitches);
				for(int j=0; j<numOfSwitches; j++){
					lst0.add(oo); //first initiate all elements of switchConnectivity to oo
					lst1.add(-1);//first initiate all elements of switchPortMapping to -1					
				}
				for(int m=p*k+k/2; m<p*k+k; m++){
					if(m==i+p*k){
						lst0.set(m, 0); //the path length from the switch to itself is set to be 0
						break;
						}
				}
				int u=1;
				for(int m=p*k; m<p*k+k/2; m++){						
					//the upper level switch is connected to all lower level switches in that pod
					lst0.set(m, 10);  
					lst1.set(m, u);
					u++;
				}
				//each upper level switch is connected to k/2 core switches		
				for(int s=0; s<k/2; s++){
					lst0.set(k*k+s+(i-(k/2))*(k/2),10);
					lst1.set(k*k+s+(i-(k/2))*(k/2), u);
					u++;
				}
				switchConnectivity.add(lst0);
				switchPortMapping.add(lst1);
			}
			
		}
		//core switches
		for(int i=(int) Math.pow(k,2); i<((k*k)+Math.pow(k/2,2)); i++){
			switchList.add(i+1);
			coreSwitches.add(i+1);
			//to add switch connectivty for core switches
			Vector<Integer> lst0 = new Vector<Integer>(numOfSwitches);
			Vector<Integer> lst1 = new Vector<Integer>(numOfSwitches);
			for(int j=0; j<numOfSwitches; j++){
				lst0.add(oo); //first initiate all elements of switchConnectivity to oo
				lst1.add(-1);//first initiate all elements of switchPortMapping to -1	
			}
			lst0.set(i, 0);
			int u=1;
			for(int p=0; p<k; p++){
				for(int m=k/2; m<k; m++){					
					//TODO //to change lst1
					if((switchConnectivity.get(m+p*k).get(i)!=oo)&(switchConnectivity.get(m+p*k).get(i)!=0)){
						lst0.set(m+p*k, switchConnectivity.get(m+p*k).get(i));
						lst1.set(m+p*k, u);
						u++;
					}					
				}
			}			
			switchConnectivity.add(lst0);
			switchPortMapping.add(lst1);
		}
		for(int p=0;p<k;p++){	
			//k/2 lower level switches which connected to servers in each pod
			for (int i=0; i<k/2; i++){
				//each switch has k/2 ports connected to servers
				for(int j=0; j<k/2; j++){
					serverList.add((int) (j+1+i*(k/2)+p*(Math.pow(k/2,2))));
					Vector<Integer> lst = new Vector<Integer>(Arrays.asList(i+p*k+1,j+1));
					serverSwitchMapping.add(lst);
				}
			}
		}
		
		//NodeConnectivity contains switch part and server part
		for(int i=0; i<numOfSwitches;i++){
			Vector<Integer> lst0 = new Vector<Integer>(totalNodeNo);
			Vector<Integer> lst1 = new Vector<Integer>(totalNodeNo);
			
			Vector<Double> lst2 = new Vector<Double>(totalNodeNo);
			
			for(int j=0;j<totalNodeNo;j++){
				lst0.add(oo);
				lst1.add(-1);
				
				lst2.add((double) oo);
			}
			for(int j=0;j<numOfSwitches;j++){
				if(switchConnectivity.get(i).get(j)!=oo){
					int currentSwitchConnectivity = switchConnectivity.get(i).get(j);
					
					lst0.set(j, currentSwitchConnectivity);
					if(currentSwitchConnectivity == 10) {
						if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.DJIKSTRA ||
								networkRoutingAlgorithm == NetworkRoutingAlgorithm.POPCORNS ||
								networkRoutingAlgorithm == NetworkRoutingAlgorithm.WASP) {
							// All weights are the same in Djikstra. And PopCorns initially
							djikstra.setAdjacency(i + 1, j + 1, 1);
						}
						else if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.ELASTIC_TREE) {
							// Weights are based on bandwidth in Elastic Tree
							double weight = -1;
							if(edgeSwitches.contains(i + 1) || edgeSwitches.contains(j + 1)) {
								weight = edgeSwitchBW;
							}
							else if(aggregateSwitches.contains(i + 1) || aggregateSwitches.contains(j + 1)) {
								weight = aggregateSwitchBW;
							}
							else if(coreSwitches.contains(i + 1) && coreSwitches.contains(j + 1)) {
								weight = coreSwitchBW;
							}
							else {
								Sim.fatalError("Unexpected switch type");
							}
							
							djikstra.setAdjacency(i + 1, j + 1, weight);
						}
					}
					
					lst1.set(j, switchPortMapping.get(i).get(j));
					
					lst2.set(j, (double)currentSwitchConnectivity);
				}
			}
			for(int s=0; s<numOfServers;s++){
				if(serverSwitchMapping.get(s).get(0)==(i+1)){
					lst0.set(s+numOfSwitches,10);
					if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.DJIKSTRA || networkRoutingAlgorithm == NetworkRoutingAlgorithm.POPCORNS || networkRoutingAlgorithm == NetworkRoutingAlgorithm.WASP) {
						djikstra.setAdjacency(i + 1, s + numOfSwitches + 1, 1);
					}
					else if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.ELASTIC_TREE) {
						djikstra.setAdjacency(i + 1, s + numOfSwitches + 1, edgeSwitchBW);
					}
					
					lst1.set(s+numOfSwitches,serverSwitchMapping.get(s).get(1));
					
					lst2.set(s+numOfSwitches,(double)10);
				}
			}
			nodeConnectivity.add(lst0);
			nodePortsMapping.add(lst1);
			weightMatrix.add(lst2);
		}	
		for(int i=0; i<numOfServers;i++){
			Vector<Integer> lst0 = new Vector<Integer>(totalNodeNo);
			Vector<Integer> lst1 = new Vector<Integer>(totalNodeNo);
			
			Vector<Double> lst2 = new Vector<Double>(totalNodeNo);
			
			for(int j=0;j<totalNodeNo;j++){
				lst0.add(oo);
				lst1.add(-1);
				
				lst2.add((double)oo);
			}
			lst0.set(i+numOfSwitches, 0);
			
			lst2.set(i+numOfSwitches, (double)0);
			for(int j=0;j<numOfSwitches;j++){
				if(serverSwitchMapping.get(i).get(0)==(j+1)){
					lst0.set(j, 10);
					if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.DJIKSTRA || networkRoutingAlgorithm == NetworkRoutingAlgorithm.POPCORNS || networkRoutingAlgorithm == NetworkRoutingAlgorithm.WASP) {
						djikstra.setAdjacency(i + numOfSwitches + 1, j + 1, 1);
					}
					else if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.ELASTIC_TREE) {
						djikstra.setAdjacency(i + numOfSwitches + 1, j + 1, edgeSwitchBW);
					}
					
					lst1.set(j, 1);
					
					lst2.set(j, (double)10);
				}
			}
			nodeConnectivity.add(lst0);
			nodePortsMapping.add(lst1);
			
			weightMatrix.add(lst2);
		}
	}

	public int getNumOfSwitches() {
		return numOfSwitches;
	}
	public void setNumOfSwitches(int numOfSwitches) {
		this.numOfSwitches = numOfSwitches;
	}
	public int getNumOfServers() {
		return numOfServers;
	}
	public void setNumOfServers(int numOfServers) {
		this.numOfServers = numOfServers;
	}
	public int getTotalNodeNo() {
		return totalNodeNo;
	}
	
	public Vector<Integer> getServerList() {
		/*for(int n = 0; n < numOfServers; n++ ){
			serverList.add(n + 1);
		}*/
		return serverList;
	}
	public Vector<Integer> getSwitchList() {
		return switchList;
	}
	
	public Vector<Vector<Integer>> getNodeConnectivity() {			
		return nodeConnectivity;
	}
	
	public Vector<Vector<Integer>> getNodePortsMapping() {				
		return nodePortsMapping;
	}
	
	public Vector<Vector<Double>> getInitialWeightMatrix() {			
		return weightMatrix;
	}
	
	public Vector<Integer> getCoreSwitches() {
		return coreSwitches;
	}
	
	public Vector<Integer> getAggregateSwitches() {
		return aggregateSwitches;
	}
	
	public Vector<Integer> getEdgeSwitches() {
		return edgeSwitches;
	}
}
