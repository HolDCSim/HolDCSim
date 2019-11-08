package experiment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import controller.RandomSSEnforcer;
import controller.WindowedSSEnforcer;

import beans.SSWindowRecord;

import stochastic.ExponentialRandom;
import utility.ServerPowerMeter;
import event.ExperimentInput;
import event.ExperimentOutput;

public class WindowedSSExperiment extends SleepScaleExperiment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * enforcer for windowed frequency and sleep state control
	 */
	private WindowedSSEnforcer ssEnforcer;

	public WindowedSSExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
        //remaining tokens for 
		parseCmdParams(argParser);

		WindowedSSEnforcer ssEnforcer = new RandomSSEnforcer(dataCenter, this,
				expConfig.getEnforcePeriod());
		this.setSSEnforcer(ssEnforcer);
		// TODO Auto-generated constructor stub
	}

	public void setSSEnforcer(WindowedSSEnforcer enforcer) {
		this.ssEnforcer = enforcer;
	}

	public WindowedSSEnforcer getSSEnforcer() {
		return this.ssEnforcer;
	}

	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("enfper", true, "enforce period");

		if(argParser.parse("enfper") == null) {
			System.err
			.println("enforce period parameter is required");
			System.exit(0);
		}

		double enforcePeriod = Double.parseDouble(argParser.parse("enfper"));
		mExpConfig.setEnforcePeriod(enforcePeriod);

	}

	@Override
	public void dumpEnergyPerfStats() {

		// each experiment calculates its own power and energy
		double power = 0.0;
		double energy = 0.0;

		ExperimentConfig exp = dataCenter.getExpConfig();
		// double tempIdle = 0.0;

		try {
			// use append mode
			File file = new File("windowedSS_results");
			File bufferFile = new File("randomss_results_bufferhis");
			FileWriter fw;
			if (!file.exists()) {
				fw = new FileWriter(new File("windowedSS_results"), true);
				fw.write("date" + String.format("%15s", "rou")
						+ String.format("%15s", "uBar")
						+ String.format("%15s", "windowSize")
						+ String.format("%15s", "averageLatency")
						+ String.format("%15s", "power"));
				fw.write("\n");
			} else {
				fw = new FileWriter(new File("randomss_results"), true);
				// fw.write("\n");
			}

			FileWriter bufferW = new FileWriter(bufferFile);
			Vector<SSWindowRecord> records = ssEnforcer.getWindowedRecords();

			for (SSWindowRecord record : records) {
				double lowPower = ServerPowerMeter.calculateLowPower(
						exp, record.getSpeed(), record.getSleepState());
				double activePower = ServerPowerMeter
						.calculateActivePower(exp, record.getSpeed());
				// tempIdle += record.getIdleTimePeriod();
				energy += lowPower * record.getIdleTimePeriod() + activePower
						* record.getActiveTimePeriod();

				// write the number of pending jobs at the start point of
				// each window
				bufferW.write(String.format("%-15s",
						record.getWindowStateTime()));
				bufferW.write("\t");
				bufferW.write(String.format("%-15s", record.getJobsInQueue()));
				bufferW.write("\t");
				bufferW.write(String.format("%-15s", record.getCompletedJobs()));
				bufferW.write("\n");
				// fw.write(Double.toString(record.getSpeed()) + "		");
				// fw.write(Integer.toString(record.getSleepState()));
				// fw.write("\n");
			}

			power = energy / this.simulationTime;

			fw.write(fileCreatTime + "\t\t\t" + exp.getRou() + "\t\t\t"
					+ exp.getuBar() + "\t\t\t"
					+ dataCenter.getExpConfig().getEnforcePeriod() + "\t\t\t"
					+ averageLatencies.get(0) + "\t\t\t" + power);
			fw.write("\n");

			fw.flush();
			bufferW.flush();

			fw.close();
			bufferW.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		super.initialize();
		WindowedSSEnforcer ssEnforcer = new RandomSSEnforcer(dataCenter, this,
				mExpConfig.getEnforcePeriod());
		this.setSSEnforcer(ssEnforcer);
	}

}
