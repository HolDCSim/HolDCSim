package stat;

import java.io.*;
import java.util.*;

import constants.Constants;
import constants.Constants.TimeWeightedStatName;


public class RawStatLogger implements Runnable {

	private String statisticName;
	private Vector<Vector<Double>> statistics;

	public static String enumToString(TimeWeightedStatName name) {

		String fileName;
		switch (name) {
		case THROUGHPUT:
			fileName = "throughputStats.stat";
			break;
		case SERVER_UTILIZATION:
			fileName = "serverU.stat";
			break;
		case SERVER_POWER:
			fileName = "serverPower.stat";
			break;
		case PORT_UTILIZATION:
			fileName = "portU.stat";
			break;
		case SERVER_IDLE_FRACTION:
			fileName = "idleFrac.stat";
			break;
		case SWITCH_POWER:
			fileName = "switchPower.stat";
			break;
		default:
			fileName = null;
		}
		return fileName;
	}

	public static boolean checkBufferedLog(TimeWeightedStatName name,
			Vector<Vector<Double>> log) {
		String fileName;
		if (log.size() < Constants.SIZE_TO_WRITE_FILE) {
			log = null;
			return false;
		}

		fileName = enumToString(name);
		if (StatisticsCollection.SAVE_TO_FILE) {
			RawStatLogger logWriter = new RawStatLogger(fileName, log);
			Thread fileThread = new Thread(logWriter);
			fileThread.start();
		}

		else if (null != log) {
			log.removeAllElements();
		}
		return true;
	}

	public RawStatLogger(String name, Vector<Vector<Double>> statistics) {
		this.statisticName = name;
		this.statistics = statistics;
	}

	public RawStatLogger() {

	}

	public void setNameAndStats(TimeWeightedStatName name,
			Vector<Vector<Double>> stats) {
		this.statisticName = enumToString(name);
		this.statistics = stats;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (null == statisticName || null == statistics)
			return;
		try {
			File logFile = new File(statisticName);
			// the file writer uses append mode
			if (!logFile.exists())
				logFile.createNewFile();
			FileWriter fw = new FileWriter(logFile.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);

			synchronized (logFile) {
				for (Vector<Double> vDouble : statistics) {
					if (vDouble.size() != 2)
						continue;
					bw.write(Double.toString(vDouble.get(0)) + " "
							+ Double.toString(vDouble.get(1)) + "\n");
				}
				bw.close();

			}
			statistics.removeAllElements();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
