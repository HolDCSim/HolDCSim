package topology;

import java.math.BigInteger;
import java.util.Vector;

public class FBFLYGen extends AbstractTopologyGen{

	/**
	 * k is number of base switches
	 */
	private int k = 0;
	
	// TODO: Set weightMatrix
	private Vector<Vector<Double>> weightMatrix;

	/**
	 * d is the dimension of flattened butterfly
	 */
	private int d = 0;// the minimum d is 1, only one switch in the topology
	int oo = 10000;

	private Vector<Vector<Integer>> switchConnectivity;
	private Vector<Vector<Integer>> serverSwitchMapping;
	private Vector<Vector<Integer>> switchPortMapping;
	////////////////////////////////////////////////////////added by jingxin
	private Vector<Integer> serverList;
	private Vector<Integer> switchList;
	////////////////////////////////////////////////////////
	public FBFLYGen(int ary, int dim) {
		this.k = ary;
		this.d = dim;

		switchConnectivity = new Vector<Vector<Integer>>();
		serverSwitchMapping = new Vector<Vector<Integer>>();
		switchPortMapping = new Vector<Vector<Integer>>();
////////////////////////////////////////////////////////added by jingxin
		numOfSwitches = (int) Math.pow((double) k, d - 1);
		numOfServers = numOfSwitches * k;
		totalNodeNo = numOfSwitches+numOfServers;
		nodeConnectivity=new Vector<Vector<Integer>>(totalNodeNo);
		nodePortsMapping=new Vector<Vector<Integer>>(totalNodeNo);
		serverList = new Vector<Integer>();
		switchList = new Vector<Integer>();		
				
////////////////////////////////////////////////////////added by jingxin
		NumberSystemCalculate numSys = new NumberSystemCalculate();
		for (int i = 0; i <= numOfSwitches - 1; i++) {
			numSys.toAnyConversion(BigInteger.valueOf(i), BigInteger.valueOf(k));
			String dScaleFormat = numSys.getToAnyConversion();
			StringBuilder extendedBuilder = new StringBuilder();

			for (int toPrepend = d - 1 - dScaleFormat.length(); toPrepend > 0; toPrepend--) {
				extendedBuilder.append('0');
			}

			extendedBuilder.append(dScaleFormat);
			String extendedString = extendedBuilder.toString();

			Vector<Integer> adjacentList = null;
			
			// set and initialize the adjacent list
			adjacentList = new Vector<Integer>();
			// // default, fill the collection with infinite value
			// Collections.fill(adjacentList, oo);

			for (int temp = 0; temp < numOfSwitches; temp++) {
				adjacentList.add(oo);
			}

			for (int j = d - 2; j >= 0; j--) {

				int numAtPosition = NumberSystemCalculate
						.changeDec(extendedString.charAt(j));

				for (int t = k - 1; t >= 0; t--) {
					if (t != numAtPosition) {
						char charOfNum = NumberSystemCalculate
								.changToNum(BigInteger.valueOf(t));
						StringBuilder stringB = new StringBuilder(
								extendedString);
						stringB.setCharAt(j, charOfNum);
						numSys.toDecimal(stringB.toString(), k);

						// get the decimal result of its neighbor
						int neighborNum = numSys.getToDecimalResult()
								.intValue();
						// adjacentList.add(Integer.valueOf(neighborNum));
						adjacentList.set(neighborNum, 10);
					}

					else
						adjacentList.set(i, 0);
				}
			}
			switchConnectivity.add(adjacentList);
		}
		Vector<Integer> connectedSwitch = null;
		for (int i = 0; i < numOfServers; i++) {
			int connectSwitchNum = (i) / k + 1;
			int switchPortNo = i % k;
			connectedSwitch = new Vector<Integer>();
			connectedSwitch.add(connectSwitchNum);
			connectedSwitch.add(switchPortNo + 1);
			// add switch port number to the vector
			serverSwitchMapping.add(connectedSwitch);
		}	
		
		for (int i = 0; i < numOfSwitches; i++) {				
			Vector<Integer> rowSwitchPorts = new Vector<Integer>();
			// Collections.fill(rowSwitchPorts, -1);
			for (int temp = 0; temp < numOfSwitches; temp++) {
				rowSwitchPorts.add(-1);
			}
			// port number for inter-switch connection begins with k + 1
			int base = k + 1;

			Vector<Integer> rowVector = switchConnectivity.get(i);
			for (int t = 0; t < rowVector.size(); t++) {
				if (rowVector.get(t) < 10000 && rowVector.get(t) > 0) {
					rowSwitchPorts.set(t, base);
					base++;
				}
			}
			switchPortMapping.add(rowSwitchPorts);
		}			
		
		//NodeConnectivity contains switch part and server part
		for(int i=0; i<numOfSwitches;i++){
			Vector<Integer> lst0 = new Vector<Integer>(totalNodeNo);
			Vector<Integer> lst1 = new Vector<Integer>(totalNodeNo);
			for(int j=0;j<totalNodeNo;j++){
				lst0.add(oo);
				lst1.add(-1);
			}
			for(int j=0;j<numOfSwitches;j++){
				if(switchConnectivity.get(i).get(j)!=oo){
					lst0.set(j, switchConnectivity.get(i).get(j));
					lst1.set(j, switchPortMapping.get(i).get(j));
				}
			}
			for(int s=0; s<numOfServers;s++){
				if(serverSwitchMapping.get(s).get(0)==(i+1)){
					lst0.set(s+numOfSwitches,10);
					lst1.set(s+numOfSwitches,serverSwitchMapping.get(s).get(1));
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
			lst0.set(i+numOfSwitches, 0);
			for(int j=0;j<numOfSwitches;j++){
				if(serverSwitchMapping.get(i).get(0)==(j+1)){
					lst0.set(j, 10);
					lst1.set(j, 1);
					break;
				}
			}
			nodeConnectivity.add(lst0);
			nodePortsMapping.add(lst1);
		}
	}

	public Vector<Integer> getServerList() {
		for(int n = 0; n < numOfServers; n++ ){
			serverList.add(n + 1);
		}
		return serverList;
	}
	public Vector<Integer> getSwitchList() {
		for(int m =0; m < numOfSwitches; m++){
			switchList.add(m + 1);
		}
		return switchList;
	}
	public Vector<Vector<Integer>> getNodeConnectivity() {		
		return nodeConnectivity;
	}

	public Vector<Vector<Integer>> getNodePortsMapping() {	
		if (nodeConnectivity == null) {
			System.err.println("error, swithes are not initialized \n");
			return null;
		} else {
			return nodePortsMapping;
		}
	}
	
	public Vector<Vector<Double>> getInitialWeightMatrix() {			
		return weightMatrix;
	}
}