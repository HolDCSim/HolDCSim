package utility;

import experiment.ExperimentConfig;
import experiment.ExperimentConfig.ServerType;

public class ServerPowerMeter {

	/**
	 * Calculates the power being used when the server is in a low power sleep state. ExperimentConfig is taken
	 * in order to import the various dynamic and static power values for each sleep state from the uesr
	 * configuration file.
	 * @param expConfig
	 * @param speed
	 * @param sleepState
	 * @return Returns the amount of power being used by a server in the given configuration, in the given sleep state, and with the given speed
	 */
	public static double calculateLowPower(ExperimentConfig expConfig, double speed, int sleepState) {

		double power = 0.0;
		switch (sleepState) {
		case 0:
		    power = calculateActivePower(expConfig,speed);
		    break;
		case 1:
			power = Math.pow(speed, 3) * expConfig.getServerStaticPower(1) + expConfig.getServerStaticPower(1);
			break;
		case 2:
			power = Math.pow(speed, 2) * expConfig.getServerStaticPower(2) + expConfig.getServerStaticPower(2);
			break;
		case 3:
			power = expConfig.getServerStaticPower(3) + expConfig.getServerStaticPower(3);
			break;
		case 4:
			power = expConfig.getServerStaticPower(4) + expConfig.getServerStaticPower(4);
			break;
		case 5:
			power = expConfig.getServerStaticPower(5) + expConfig.getServerStaticPower(5);
			break;
		case 6:
			power = 0.0;

		}

		return power;
	}

	/**
	 * Calculates the amount of power that it takes to wakeup a server from the sleep state provided to the active
	 * state, using the power values for each sleep state as defined in the user configuration file. The wakeup power
	 * is calculated by averaging the idle power in the initial sleep state and the active power the server is transitioning to.
	 * @param expConfig
	 * @param speed
	 * @param sleepState
	 * @return the power consumed during the wakeup from the sleep state provided to the active state
	 */
	public static double calculateWakeupPower(ExperimentConfig expConfig, double speed, int sleepState) {

		double power = 0.0;
		double peakPower = Math.pow(speed, 3) * 130 + 120;
		switch (sleepState) {
		// case 0:
		// power = Math.pow(speed, 3) * 130 + 120;
		case 1:
			power = (Math.pow(speed, 3) * expConfig.getServerStaticPower(1) + expConfig.getServerDynamicPower(1) + peakPower)/2;
			break;
		case 2:
			power = (Math.pow(speed, 2) * expConfig.getServerStaticPower(2) + expConfig.getServerStaticPower(2) + peakPower)/2;
			break;
		case 3:
			power = (expConfig.getServerStaticPower(3) + expConfig.getServerStaticPower(3) + peakPower)/2;
			break;
		case 4:
			power = (expConfig.getServerStaticPower(4) + expConfig.getServerStaticPower(4) + peakPower) /2;
			break;
		case 5:
			power = (expConfig.getServerStaticPower(5) + expConfig.getServerStaticPower(5) + peakPower) /2;
			break;
		case 6:
			power = (0.0 + peakPower) /2;

		}

		return power;
	}

	/**
	 * Calculates the active power of a server under the configuration and speed provided
	 * @param expConfig
	 * @param speed
	 * @return Active power for the server.
	 */
	public static double calculateActivePower(ExperimentConfig expConfig, double speed) {
		return Math.pow(speed, 3) * expConfig.getServerStaticPower(0) + expConfig.getServerDynamicPower(0) ;
	}
	
	//total dynamic power for a core : processor dynamic power divided
	//by number of cores
//	public static double getCorePower(double aSpeed, int aSleepState){
//		double power = 0.0;
//		switch (aSleepState) {
//		// case 0:
//		// power = Math.pow(speed, 3) * 130 + 120;
//		case 1:
//			power = Math.pow(aSpeed, 3) * 75;
//			break;
//		case 2:
//			power = Math.pow(aSpeed, 2) * 47;
//			break;
//		case 3:
//			power = 22.0;
//			break;
//		case 4:
//			power = 15.0;
//			break;
//		case 5:
//			power = 15.0;
//			break;
//
//		}
//
//		return power;
//	}
	
	public static double getPlatformPower(int aSleepState){
		switch(aSleepState){
		case 1:
		case 2:
		case 3:
		case 4:
			return 60.5;
		case 5: 
			return 13.1;
		default:
			return 0.0;
		}
	}

}