package topology;

import java.math.BigInteger;
import java.util.Vector;


/**
 * generate Camcube topology with ary k and dimension 3
 * @return NodeConnectivity
 * @author jingxin
 *
 */
public class CamcubeGen extends AbstractTopologyGen {
	/**
	 * @param k is the parameter for Camcube topology
	 */
	private int k;
	private int d=3;
	private Vector<Integer> serverList;
	
	// TODO: Set weightMatrix
	private Vector<Vector<Double>> weightMatrix;
	
	public CamcubeGen(int k){
		this.k=k;
		numOfServers = (int) Math.pow(k,3);
		totalNodeNo = numOfServers;	
		nodeConnectivity = new Vector<Vector<Integer>>(numOfServers);
		nodePortsMapping = new Vector<Vector<Integer>>(numOfServers);
		
	}
	
	public Vector<Integer> getServerList() {
		for(int i = 0; i < numOfServers; i++ ){
			serverList.add(i + 1);
		}
		return serverList;
	}	
	
	//FIXME: new interfaces to be implemented
	public  Vector<Vector<Integer>> getNodeConnectivity(){	
		NumberSystemCalculate numSys = new NumberSystemCalculate();

		for (int i = 0; i <= numOfServers - 1; i++) {
			numSys.toAnyConversion(BigInteger.valueOf(i), BigInteger.valueOf(k));
			String dScaleFormat = numSys.getToAnyConversion();
			StringBuilder extendedBuilder = new StringBuilder();

			for (int toPrepend = d - dScaleFormat.length(); toPrepend > 0; toPrepend--) {
				extendedBuilder.append('0');
			}

			extendedBuilder.append(dScaleFormat);
			String extendedString = extendedBuilder.toString();

			Vector<Integer> adjacentList = null;
			int oo = 10000;

			// set and initialize the adjacent list
			adjacentList = new Vector<Integer>();
			// // default, fill the collection with infinite value
			// Collections.fill(adjacentList, oo);

			for (int temp = 0; temp < numOfServers; temp++) {
				adjacentList.add(oo);
			}

			for (int j = d - 1; j >= 0; j--) {

				int numAtPosition = NumberSystemCalculate
						.changeDec(extendedString.charAt(j));

				for (int t = k - 1; t >= 0; t--) {
					if (t != numAtPosition) {
						if((Math.abs(t-numAtPosition)==1)||(Math.abs(t-numAtPosition)==k-1)){
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
					}

					else
						adjacentList.set(i, 0);
				}

			}

			nodeConnectivity.add(adjacentList);
		}
		return nodeConnectivity;
	}
	
	public  Vector<Vector<Integer>> getNodePortsMapping(){	
		if (nodeConnectivity == null) {
			System.err.println("error, swithes are not initialized \n");
			return null;
		} else {
			for (int i = 0; i < numOfServers; i++) {				
				//Vector<Integer> neighborServers = NodeConnectivity.get(i);
				Vector<Integer> rowPorts = new Vector<Integer>();

				// Collections.fill(rowSwitchPorts, -1);
				for (int temp = 0; temp < numOfServers; temp++) {
					rowPorts.add(-1);
				}
				// port number for inter-server connection begins with 1
				int base = 1;

				Vector<Integer> rowVector = nodeConnectivity.get(i);

				for (int t = 0; t < rowVector.size(); t++) {
					if (rowVector.get(t) < 10000 && rowVector.get(t) > 0) {
						rowPorts.set(t, base);
						base++;
					}
				}
				nodePortsMapping.add(rowPorts);
			}
		}		
		return nodePortsMapping;
	}
	
	public Vector<Vector<Double>> getInitialWeightMatrix() {			
		return weightMatrix;
	}
}
