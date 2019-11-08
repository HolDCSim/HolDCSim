package controller;

import infrastructure.*;
import infrastructure.Core.PowerState;

import java.util.Vector;

import beans.SSWindowRecord;
import event.*;
import experiment.Experiment;
import experiment.SingletonJobExperiment;

public abstract class WindowedSSEnforcer {
	protected DataCenter dataCenter;
	protected double enforcePeriod;
	protected Experiment experiment;

	protected Vector<SSWindowRecord> windowedRecords = new Vector<SSWindowRecord>();

	public WindowedSSEnforcer(DataCenter dataCenter,
			SingletonJobExperiment experiment, double enforcePeriod) {
		this.dataCenter = dataCenter;
		this.enforcePeriod = enforcePeriod;
		this.experiment = experiment;

		SSWindowRecord record = new SSWindowRecord();

		record.setWindowStateTime(0.0);
		record.setWindowSize(enforcePeriod);
		record.setJobsInQueue(0);
		record.setSleepState(experiment.getDataCenter().getExpConfig()
				.getInitialSleepState());
		record.setSpeed(experiment.getExpConfig().getSpeed());
		windowedRecords.add(record);

		RecalculateSSEevent ssEvent = new RecalculateSSEevent(enforcePeriod,
				experiment, this);
		this.experiment.addEvent(ssEvent);
	}

	public void enforceSleepScale(double time) {
		EnergyServer server = (EnergyServer)dataCenter.getServers().get(0);
		AbstractSleepController abController = server.getSleepController();

		// conclude the statistics for the current window before starting the
		// next window
		SSWindowRecord record = windowedRecords.lastElement();
		//if (abController.getCurrentState() == PowerState.LOW_POWER_SLEEP) {
		if (!server.isActive() && !server.isTransitionToActive()) {
			// the idle time should be be longer than window period
			double lastIdle = time - abController.getLastIdleTime();
			if (Double.compare(lastIdle, record.getWindowSize()) > 0)
				lastIdle = record.getWindowSize();

			record.incrementIdleTime(lastIdle);
			record.setActiveTimePeriod(record.getWindowSize()
					- record.getIdleTimePeriod());

		} else
			record.setActiveTimePeriod(record.getWindowSize()
					- record.getIdleTimePeriod());

		// create entry for the next window period

		int jobsInQueue = dataCenter.getJobsInQueue();

		SSWindowRecord nextRecord = new SSWindowRecord();
		windowedRecords.add(nextRecord);

		int sleepState = getNextSleepState();
		double speed = getNextFrequency();
		nextRecord.setJobsInQueue(jobsInQueue);
		;
		nextRecord.setWindowStateTime(time);
		nextRecord.setSleepState(sleepState);
		nextRecord.setSpeed(speed);
		nextRecord.setWindowSize(enforcePeriod);

		// do the enforce here, in sleepscale we only consider one server with
		// one core

		server.setNextFixedSleepState(sleepState);
		server.setDvfsSpeed(time, speed);

		// generate the new enforce event
		RecalculateSSEevent ssEvent = new RecalculateSSEevent(time
				+ enforcePeriod, experiment, this);
		this.experiment.addEvent(ssEvent);

	}

	protected abstract int getNextSleepState();

	protected abstract double getNextFrequency();

	public void incrementCurIdleTime(final double duration) {
		SSWindowRecord record = windowedRecords.get(windowedRecords.size() - 1);
		record.incrementIdleTime(duration);
	}

	public double getCurrentWindowStartTime() {
		SSWindowRecord record = windowedRecords.lastElement();
		return record.getWindowStateTime();
	}

	public Vector<SSWindowRecord> getWindowedRecords() {
		return windowedRecords;
	}

	public void incrementCompletedJobs() {
		// TODO Auto-generated method stub
		SSWindowRecord record = windowedRecords.lastElement();
		record.incrementCompletedJobs();
	}

}
