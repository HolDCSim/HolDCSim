package utility;

import java.util.*;

public class FlowSizeGen {
	private static Random rand;
	public FlowSizeGen(){
		
		rand = new Random(System.currentTimeMillis());
	}
	public static double getNextFlowSize(){
		//genreate flow size from 50~100 Mbits
		return rand.nextInt(50) + 50;
	}
}
