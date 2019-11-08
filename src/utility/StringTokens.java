package utility;

import java.util.Arrays;
import java.util.LinkedList;

import debug.Sim;

public class StringTokens {
	private LinkedList<String> args;

	public StringTokens(String[] params) {
		if (params == null) {
			args = null;
			Sim.warning("no params passed");
		}

		else {
			args = new LinkedList<String>(Arrays.asList(params));
//			/**remve the first one since it is the simconfig path*/
//			args.remove(0);
		}
	}

	public String popToken() {
		if (args == null || args.size() == 0) {
			Sim.warning("no more params found");
			return null;
		} else {
			return args.remove(0);
		}

	}

	public int getRemainingParams() {
		if (args == null) {
			return 0;
		} else
			return args.size();
	}

	public String getLastToken(){
		if(args == null || args.size() == 0){
			Sim.warning("could not retrieve last param");
			return null;
		}
		else{
			return args.removeLast();
		}
		
			
	}
}
