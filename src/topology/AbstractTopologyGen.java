package topology;

import java.util.*;

import network.Djikstra;

import java.math.BigInteger;


public abstract class AbstractTopologyGen {

	protected int numOfSwitches = 0;
	protected int numOfServers = 0;
	protected int totalNodeNo = 0;

	protected Vector<Vector<Integer>> nodeConnectivity;
	protected Vector<Vector<Integer>> nodePortsMapping;
	// public abstract Vector<Vector<Integer>> getSwitchConnectivity();
	// public abstract Vector<Vector<Integer>> getServerSwitchMapping();
	// //////////////////////////////////////////////////////added by jingxin

	// //////////////////////////////////////////////////////
	
	// Used when ElasticTree network routing algorithm is used
	protected Djikstra djikstra;
	

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
	
	public Djikstra getDjikstra() {
		return djikstra;
	}

	public abstract Vector<Vector<Integer>> getNodeConnectivity();
	
	public abstract Vector<Vector<Double>> getInitialWeightMatrix();

	public abstract Vector<Vector<Integer>> getNodePortsMapping();
}

class NumberSystemCalculate {
	private static BigInteger toDecimalResult = BigInteger.ZERO;
	private String toAnyConversion = "";

	public BigInteger getToDecimalResult() {
		return toDecimalResult;
	}

	public void setToDecimalResult(BigInteger toDecimalResult) {
		NumberSystemCalculate.toDecimalResult = toDecimalResult;
	}

	public String getToAnyConversion() {
		return toAnyConversion;
	}

	public void setToAnyConversion(String toAnyConversion) {
		this.toAnyConversion = toAnyConversion;
	}

	static int changeDec(char ch) {
		int num = 0;
		if (ch >= 'A' && ch <= 'Z')
			num = ch - 'A' + 10;
		else if (ch >= 'a' && ch <= 'z')
			num = ch - 'a' + 36;
		else
			num = ch - '0';
		return num;
	}

	public void toDecimal(String input, int base) {
		BigInteger Bigtemp = BigInteger.ZERO, temp = BigInteger.ONE;
		int len = input.length();
		for (int i = len - 1; i >= 0; i--) {
			if (i != len - 1)
				temp = temp.multiply(BigInteger.valueOf(base));
			int num = changeDec(input.charAt(i));
			Bigtemp = Bigtemp.add(temp.multiply(BigInteger.valueOf(num)));
		}
		// System.out.println(Bigtemp);
		// return Bigtemp;
		this.setToDecimalResult(Bigtemp);
	}

	static char changToNum(BigInteger temp) {
		int n = temp.intValue();

		if (n >= 10 && n <= 35)
			return (char) (n - 10 + 'A');

		else if (n >= 36 && n <= 61)
			return (char) (n - 36 + 'a');

		else
			return (char) (n + '0');
	}

	public void toAnyConversion(BigInteger Bigtemp, BigInteger base) {
		String ans = "";
		while (Bigtemp.compareTo(BigInteger.ZERO) != 0) {
			BigInteger temp = Bigtemp.mod(base);
			Bigtemp = Bigtemp.divide(base);
			char ch = changToNum(temp);
			ans = ch + ans;
		}
		// return ans;
		this.setToAnyConversion(ans);
	}

	public void anyToAny(String input, int scouceBase, BigInteger targetBase) {
		toDecimal(input, scouceBase);
		toAnyConversion(this.getToDecimalResult(), targetBase);

	}
}