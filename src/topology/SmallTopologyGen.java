package topology;
import java.util.Arrays;
import java.util.Vector;


/**
 * to generate small topology for east testing
 * this topology contains 3 switches and 4 servers
 * D           D
 *  \         /
 *   O---O---O
 *  /         \
 * D           D
 * @author jingxin
 */
public class SmallTopologyGen extends AbstractTopologyGen {
	
	private int numOfSwitches;
	private int numOfServers;
	
	// TODO: Set weightMatrix
	private Vector<Vector<Double>> weightMatrix;
	
	public SmallTopologyGen(){
		numOfSwitches=3;
		numOfServers=4;
		totalNodeNo = numOfSwitches+numOfServers;
	}
	
	public int getNumOfSwitches() {
		return numOfSwitches;
	}
	public void setNumOfSwitches(int numOfSwitches) {
		this.numOfSwitches = numOfSwitches;
	}
	public int getTotalNodeNo() {
		return totalNodeNo;
	}
	public void setNumOfServers(int numOfServers) {
		this.numOfServers = numOfServers;
	}
	public int getNumOfServers() {
		return numOfServers;
	}

	public Vector<Vector<Integer>> getNodeConnectivity() {
	
		int oo=10000;
		nodeConnectivity =new Vector<Vector<Integer>>();	
		
		Vector<Integer> lst0 = new Vector<Integer>(Arrays.asList(0,oo,10,10,10,oo,oo));
		Vector<Integer> lst1 = new Vector<Integer>(Arrays.asList(oo,0,10,oo,oo,10,10));
		Vector<Integer> lst2 = new Vector<Integer>(Arrays.asList(10,10,0,oo,oo,oo,oo));
		Vector<Integer> lst3 = new Vector<Integer>(Arrays.asList(10,oo,oo,0,oo,oo,oo));
		Vector<Integer> lst4 = new Vector<Integer>(Arrays.asList(10,oo,oo,oo,0,oo,oo));
		Vector<Integer> lst5 = new Vector<Integer>(Arrays.asList(oo,10,oo,oo,oo,0,oo));
		Vector<Integer> lst6 = new Vector<Integer>(Arrays.asList(oo,10,oo,oo,oo,oo,0));
		nodeConnectivity.add(lst0);
		nodeConnectivity.add(lst1);
		nodeConnectivity.add(lst2);
		nodeConnectivity.add(lst3);
		nodeConnectivity.add(lst4);
		nodeConnectivity.add(lst5);
		nodeConnectivity.add(lst6);
		
		return nodeConnectivity;
	}
	
//	public Vector<Vector<Integer>> getServerSwitchMapping() {
//		// TODO Auto-generated method stub		
//		// port number begins with 1	// switch number begins with 1
//		Vector<Vector<Integer>> B =new Vector<Vector<Integer>>(numOfServers);//the input could be read from xml file
//		Vector<Integer> lst7 = new Vector<Integer>(Arrays.asList(1,1));				
//		Vector<Integer> lst8 = new Vector<Integer>(Arrays.asList(2,1));//server2 connect to switch2 and port1 of switch2
//		Vector<Integer> lst9 = new Vector<Integer>(Arrays.asList(6,1));//server3 connect to switch6 and port1 of switch6
//		Vector<Integer> lst10 = new Vector<Integer>(Arrays.asList(7,1));//server4 connect to switch7 and port1 of switch7
//		B.add(lst7); B.add(lst8);B.add(lst9); B.add(lst10);
//		serverSwitchMapping=B;
//		//System.out.println("serverSwitchMapping: "+B);
//		return serverSwitchMapping;
//	}
	public Vector<Vector<Integer>> getNodePortsMapping() {
		// TODO Auto-generated method stub

		nodePortsMapping =new Vector<Vector<Integer>>();
		
		Vector<Integer> lst0 = new Vector<Integer>(Arrays.asList(-1,-1,1,2,3,-1,-1));
		Vector<Integer> lst1 = new Vector<Integer>(Arrays.asList(-1,-1,1,-1,-1,2,3));
		Vector<Integer> lst2 = new Vector<Integer>(Arrays.asList(1,2,-1,-1,-1,-1,-1));
		Vector<Integer> lst3 = new Vector<Integer>(Arrays.asList(1,-1,-1,-1,-1,-1,-1));
		Vector<Integer> lst4 = new Vector<Integer>(Arrays.asList(1,-1,-1,-1,-1,-1,-1));
		Vector<Integer> lst5 = new Vector<Integer>(Arrays.asList(-1,1,-1,-1,-1,-1,-1));
		Vector<Integer> lst6 = new Vector<Integer>(Arrays.asList(-1,1,-1,-1,-1,-1,-1));
		

		nodePortsMapping.add(lst0);
		nodePortsMapping.add(lst1);
		nodePortsMapping.add(lst2);
		nodePortsMapping.add(lst3);
		nodePortsMapping.add(lst4);
		nodePortsMapping.add(lst5);
		nodePortsMapping.add(lst6);

		return nodePortsMapping;
	}
	
	public Vector<Vector<Double>> getInitialWeightMatrix() {			
		return weightMatrix;
	}
}
