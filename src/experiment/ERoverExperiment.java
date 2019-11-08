package experiment;

import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.ERoverStates;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import loadpredictor.AbstractLoadPredictor;
import loadpredictor.AllAveragePredictor;
import loadpredictor.InstantPredictor;
import loadpredictor.MixedInstantPredictor;
import loadpredictor.WindowedAveragePredictor;
import debug.Sim;
import scheduler.ShallowDeepScheduler;
import scheduler.ERoverScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;
import experiment.ExperimentConfig.ServerType;

/**
 * ERover experiment
 * @author fanyao
 *
 */
public class ERoverExperiment extends ShallowDeepExperiment {

	public int numOfMinS4Servers = 0;
	public int currentNumOfS4Servers = 0;
	
//	//FIXME: come back to this paramter settings later (added pendingThreshold, too long and mis parsed)
//			// collect taos from command line params
//			if (tokens.getRemainingParams() >= 6) {
//				double[] waitings = new double[6];
//				for (int i = 0; i < waitings.length; i++) {
//					waitings[i] = Double.parseDouble(tokens.popToken());
//					mExpConfig.setTaos(waitings);
//				}
//			}
			
	@Override
	public void parseCmdParams(ArgumentParser argParser) {
		super.parseCmdParams(argParser);
		
		argParser.addOption("singletao", true, "single tao");
		
		if(argParser.parse("singletao") == null) {
			Sim.fatalError("ERoverExperiment requires single tao param");
		}
		
		mExpConfig.setSingleTao(Double.parseDouble(argParser.parse("singletao")));
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ERoverExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);
		this.statsFileName = "MSEE_stats";
		this.sleepStateMode = "shallow-ms";
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.EROVER_SERVER;
	}

	@Override
	protected void setTaskScheduler() {
		ERoverScheduler eeScheduler = new ERoverScheduler(this);

		double predictorType = mExpConfig.getPredictorType();

		AbstractLoadPredictor predictor = null;

		
		// if it's mixed workload, use mixedinstant predictor
		if (predictorType == -2) {
			predictor = new MixedInstantPredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), this);
		}
		
		else if (predictorType == -1) {
			predictor = new InstantPredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else if (predictorType == 0) {
			predictor = new AllAveragePredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction());
		}

		else {
			predictor = new WindowedAveragePredictor(eeScheduler,
					eeScheduler.getQueueHis(),
					eeScheduler.getActiveServerHis(),
					mExpConfig.dumpPrediction(), predictorType);
		}

		taskScheduler = eeScheduler;
		eeScheduler.setQueuePredictor(predictor);
	}

	@Override
	protected void formulateResults() {
		
		super.formulateResults();
		
		double taoMiddleToDeep = AbstractSleepController.sleepStateWaitings[ERoverStates.DEEPEST_SLEEP - 1]
		- AbstractSleepController.sleepStateWaitings[ERoverStates.DEEP_SLEEP - 1];
		resultFieldValueMap.put(String.format("%-20s", "tao-middle-deep"),  String.format("%-20.2f", taoMiddleToDeep));
		
		
//		// used to log critical experiment result statistics
//		File file = new File(statsFileName);
//		FileWriter fw;
//
//		double speed = dataCenter.getExpConfig().getSpeed();
//
//		// write statistics to file
//		try {
//
//			if (!file.exists()) {
//				// use append mode
//				fw = new FileWriter(new File(statsFileName), true);
//				fw.write(String.format("%-15s", "date")
//						+ String.format("%-15s", "numOfJobs")
//						+ String.format("%-15s", "rou")
//						+ String.format("%-15s", "uBar")
//						+ String.format("%-15s", "speed")
//						+ String.format("%-15s", "sleepstateMode")
//						+ String.format("%-20s", "tao-middle-deep")
//						+ String.format("%-15s", "numOfServers")
//						+ String.format("%-15s", "Ts")
//						+ String.format("%-15s", "Tw")
//						+ String.format("%-15s", "QueuePredictor")
//						+ String.format("%-15s", "executionTime")
//						+ String.format("%-20s", "averageServiceT")
//						+ String.format("%-60s", "averageLatency")
//						+ String.format("%-60s", "NomalizedLatency")
//						+ String.format("%-15s", "totalEnergy")
//						+ String.format("%-15s", "averagePower")
//						+ String.format("%-15s", "percentile")
//						+ String.format("%-15s", "percentile95")
//						+ String.format("%-15s", "percentile99"));
//				fw.write("\n");
//			} else {
//				fw = new FileWriter(file, true);
//			}
//
//			String sPredictorType = null;
//			double predictorType = mExpConfig.getPredictorType();
//			if(predictorType == -2){
//				sPredictorType = "mixed-inst";
//			}else if (predictorType == -1) {
//				sPredictorType = "inst";
//			} else if (predictorType == 0) {
//				sPredictorType = "(0,t)";
//			} else if (predictorType > 0) {
//				sPredictorType = "windowed-"
//						+ String.format("%-5.2f", predictorType);
//			}
//
//			else {
//				Sim.fatalError("unrecognized predictor type");
//			}
//
//			double taoMiddleToDeep = AbstractSleepController.sleepStateWaitings[ERoverStates.DEEPEST_SLEEP - 1]
//					- AbstractSleepController.sleepStateWaitings[ERoverStates.DEEP_SLEEP - 1];
//			fw.write(String.format("%-15s", fileCreatTime)
//					+ String.format("%-15d", mExpConfig.getNumOfJobs())
//					+ String.format("%-15.2f", mExpConfig.getRou())
//					+ String.format("%-15.5f", mExpConfig.getuBar())
//					+ String.format("%-15.2f", speed)
//					+ String.format("%-15s", sleepStateMode)
//					+ String.format("%-20.2f", taoMiddleToDeep)
//					+ String.format("%-15d", mExpConfig.getServersToSchedule())
//					+ String.format("%-15.2f", ShallowDeepScheduler.Ts)
//					+ String.format("%-15.2f", ShallowDeepScheduler.Tw)
//					+ String.format("%-15s", sPredictorType)
//					+ String.format("%-15.2f", this.simulationTime)
//					+ String.format("%-20.5f", this.getAverageServiceTime())
//					+ String.format("%-60s", averageLatencies.toString())
//					+ String.format("%-60s", normalizedLatencies.toString())
//					+ String.format("%-15.2f", totalEnergy)
//					+ String.format("%-15.2f", totalEnergy
//							/ this.simulationTime)
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->",
//									mExpConfig.getLatencyPercent() * 100)
//									+ String.format("%.2f",
//											percentileLatencies.get(0)))
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->", 95.0)
//									+ String.format("%.2f",
//											percentileLatency95s.get(0)))
//					+ String.format(
//							"%-15s",
//							String.format("%3.1f%%->", 99.0)
//									+ String.format("%.2f",
//											percentileLatency99s.get(0))));
//			fw.write("\n");
//
//			// fw.write(fileCreatTime + "\t" + mExpConfig.getNumOfJobs() + "\t"
//			// + mExpConfig.getRou() + "\t" + mExpConfig.getuBar() + "\t"
//			// + speed + "\t" + sleepStateMode + "\t"
//			// + mExpConfig.getServersToSchedule() + "\t" + 0 + "\t" + 0
//			// + "\t" + this.simulationTime + "\t"
//			// + this.getAverageServiceTime() + "\t" + averageLatency
//			// + "\t" + normalizedLatency + "\t" + energy + "\t" + energy
//			// / this.simulationTime);
//			// fw.write("\n");
//
//			fw.flush();
//			fw.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	@Override
	public void initialize() {
		this.numOfMinS4Servers = (int) Math.floor(mExpConfig
				.getServersToSchedule() * mExpConfig.getS4ProvisionPercent());
		this.currentNumOfS4Servers = 0;
		
		//set the single tao value 
		//AbstractSleepController.sleepStateWaitings[ERoverStates.DEEPEST_SLEEP - 1] = mExpConfig.getSingleTao();
		for (int i=0; i<ERoverStates.DEEPEST_SLEEP; i++) {
			AbstractSleepController.sleepStateWaitings[i]=mExpConfig.getServerSleepWaitTime(i);
		}
		super.initialize();
	}
	
	
	
	

}
