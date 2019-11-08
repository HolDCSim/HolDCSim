package experiment;

import infrastructure.OnOffServer;
import infrastructure.Server;
import infrastructure.UniprocessorServer;

import java.util.Vector;
import job.Task;
import loadpredictor.FakeLoadPredictor;

import scheduler.MultiServerScheduler;
import scheduler.OnOffScheduler;
import stochastic.ExponentialRandom;
import event.ExperimentInput;
import event.ExperimentOutput;

import experiment.ExperimentConfig.ServerType;

public class OnOffExperiment extends OnSleepExperiment {

	private OnOffScheduler onoffScheduler;

	/**
	 * fanyao
	 */
	private static final long serialVersionUID = 1L;

	public OnOffExperiment(String theExperimentName,
			ExponentialRandom aRandom, ExperimentInput theExperimentInput,
			ExperimentOutput thExperimentOutput, ExperimentConfig expConfig,
			ArgumentParser argParser) {
		super(theExperimentName, aRandom, theExperimentInput,
				thExperimentOutput, expConfig, argParser);

		this.statsFileName = "ONOFF_stats";
		this.sleepStateMode = "on_off";
	}

	@Override
	public void initialize() {
		super.initialize();
		Vector<Server> servers = dataCenter.getServers();

		// for original delayoff implementation
		/*******************************************************
		 * onoff2Scheduler = (OnOff2Scheduler) taskScheduler;
		 *******************************************************/

		onoffScheduler = (OnOffScheduler) taskScheduler;

		for (Server aServer : servers) {
			UniprocessorServer sServer = (UniprocessorServer) aServer;
			// FIXME: current the off state is hard coded to 6
			sServer.setNextFixedSleepState(6);
		}
	}

	// @Override
	// protected void setTaskScheduler() {
	// taskScheduler = new OnOffScheduler(this);
	// }

	@Override
	protected void setTaskScheduler() {
		// OnOff2Scheduler is not original delayoff experiment implementation
		/**********************************************
		 * taskScheduler = new OnOff2Scheduler(this);
		 **********************************************/
		taskScheduler = new OnOffScheduler(this);
		MultiServerScheduler msScheduler = (MultiServerScheduler) taskScheduler;
		msScheduler.setQueuePredictor(new FakeLoadPredictor(msScheduler, msScheduler
				.getQueueHis()));
	}

	@Override
	protected void initialServerType() {
		this.serverType = ServerType.ON_OFF_SERVER;
	}

	public void shutoffTranServer(OnOffServer aServer, double time) {
		onoffScheduler.shutoffTranServer(aServer, time);
	}

	public Task getUnassignedJobInQueue() {
		return onoffScheduler.getUnassignedJobInQueue();
	}

	public void removeTranServer(OnOffServer server) {
		// TODO Auto-generated method stub
		onoffScheduler.removeTranServer(server);

	}

	@Override
	public double getNextSetupTime() {
		// for our comparison
		// return meanSetupTime;
		return setupTimeGen.getNextDouble();
	}

}
