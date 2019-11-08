package topology;

import java.util.Vector;

/**
 * generate Bcube topology with level k and switch port number n
 * @return NodeConnectivity
 * @author jingxin
 *
 */
public class BcubeGen extends AbstractTopologyGen {
	/**
	 * @param k,n is the parameter for Bcube topology
	 */
	private int k,n;
	private Vector<Vector<Double>> weightMatrix;
	private int oo = 10000;

	
	public BcubeGen(int lvlno, int ptno){
		this.k = lvlno;
		this.n = ptno;
		
		numOfSwitches = (k+1)*(int) Math.pow(n,k);
		numOfServers = (int) Math.pow(n,k+1);
		totalNodeNo = numOfSwitches+numOfServers;
		nodeConnectivity = new Vector<Vector<Integer>>(totalNodeNo);
		nodePortsMapping = new Vector<Vector<Integer>>(totalNodeNo);
		
		Vector<Vector<Integer>> switchconnectedserver = new Vector<Vector<Integer>>(numOfSwitches);		
		//for each level, there are n^k switches, each connecting to n servers	
		for(int l=0;l<=k;l++){			
			for(int m=0;m<(int) Math.pow(n,k-l);m++){
				for(int i=0; i<(int) Math.pow(n,l);i++){
					Vector<Integer> lst = new Vector<Integer>(numOfServers);
					for(int j=0;j<numOfServers;j++){
						lst.add(oo);
					}
					for(int nn=0;nn<n;nn++){
						lst.set(i+ m*(int) Math.pow(n,l+1) +nn*(int) Math.pow(n,l), 10);						
					}			
					switchconnectedserver.add(lst);
				}				
			}			
		}
		//NodeConnectivity contains switch part and server part
		for(int i=0; i<numOfSwitches;i++){
			Vector<Integer> lst0 = new Vector<Integer>(totalNodeNo);
			Vector<Integer> lst1 = new Vector<Integer>(totalNodeNo);
			for(int j=0;j<totalNodeNo;j++){
				lst0.add(oo);
				lst1.add(-1);
			}
			int pp=0;
			for(int j=0;j<numOfServers;j++){				
				if(switchconnectedserver.get(i).get(j)!=oo){
					lst0.set(j+numOfSwitches,switchconnectedserver.get(i).get(j));
					lst1.set(j+numOfSwitches,pp+1);
					pp++;
				}				
			}
			nodeConnectivity.add(lst0);
			nodePortsMapping.add(lst1);
		}
		for(int i=0; i<numOfServers;i++){
			Vector<Integer> lst0 = new Vector<Integer>(totalNodeNo);
			Vector<Integer> lst1 = new Vector<Integer>(totalNodeNo);
			for(int j=0;j<totalNodeNo;j++){
				lst0.add(oo);
				lst1.add(-1);
			}
			int pp=0;
			for(int j=0;j<numOfSwitches;j++){
				if(switchconnectedserver.get(j).get(i)!=oo){
					lst0.set(j,switchconnectedserver.get(j).get(i));
					lst1.set(j,pp+1);
					pp++;
				}				
			}
			nodeConnectivity.add(lst0);
			nodePortsMapping.add(lst1);
		}
		
	}

	//FIXME: new interfaces to be implemented
	public  Vector<Vector<Integer>> getNodeConnectivity(){		
		return nodeConnectivity;
	}
	
	public  Vector<Vector<Integer>> getNodePortsMapping(){		
		return nodePortsMapping;
	}

	public Vector<Vector<Double>> getInitialWeightMatrix() {			
		return weightMatrix;
	}
}
