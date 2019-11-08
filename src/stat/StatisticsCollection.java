/**
 * Copyright (c) 2011 The Regents of The University of Michigan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met: redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer;
 * redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution;
 * neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author David Meisner (meisner@umich.edu)
 *
 */
package stat;

import java.io.Serializable;
import java.util.Date;
import java.text.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.io.*;

import constants.Constants.StatName;
import constants.Constants.TimeWeightedStatName;
import debug.Sim;

import event.ExperimentOutput;

/**
 * A StatisticsCollection tracks all the statistics in the simulation. It
 * monitors the state of these simulations (if they're warm or converged) and
 * appropriately notify the others.
 * 
 * @author David Meisner (meisner@umich.edu)
 */
public class StatisticsCollection implements Serializable {

	/**
	 * file to temporary store statistics output
	 */
	public static FileWriter logFile;

	/** The serialization id. */
	private static final long serialVersionUID = 1L;

	// fanyao added: option indicates whether save to file
	public static boolean SAVE_TO_FILE = true;

	/** A map between statistics names and the statistic. */
	private HashMap<StatName, Statistic> statsMap;

	/**
	 * A map between time weighted statistics names and the time weighted
	 * statistic.
	 */
	private HashMap<TimeWeightedStatName, TimeWeightedStatistic> twStatsMap;

	/**
	 * The statistics which must be converged in order for the simulation to be
	 * done.
	 */
	private Vector<Statistic> convergeStats;

	/**
	 * The statistics which must be warm in order for the simulation to be warm.
	 */
	private Vector<Statistic> warmStats;

	// TODO double check we really need these
	/** A fake statistic to return if an unknown statistic name is requested. */
	private FakeStatistic fakeStatistic;

	/**
	 * A fake time weighted statistic to return if an unknown time weighted
	 * statistic name is requested.
	 */
	private FakeTimeWeightedStatistic twFakeStatistic;

	private String sDate; // time when fanyao_log file is generated

	/**
	 * Creates a new StatisticsCollection.
	 */
	public StatisticsCollection() {
		this.convergeStats = new Vector<Statistic>();
		this.warmStats = new Vector<Statistic>();
		this.statsMap = new HashMap<StatName, Statistic>();
		this.twStatsMap = new HashMap<TimeWeightedStatName, TimeWeightedStatistic>();
		this.fakeStatistic = new FakeStatistic();
		this.twFakeStatistic = new FakeTimeWeightedStatistic();
		Date date = ExperimentOutput.expDate;
		DateFormat simple = new SimpleDateFormat("MM_dd_HH_mm");
		sDate = simple.format(date);
	}

	/**
	 * Creates a new StatisticsCollection.
	 * 
	 * @param aStatsMap
	 *            - a mapping of statistic names to statistics
	 * @param aConvergeStats
	 *            - a vector of statistics that must be converged to end the
	 *            simulation
	 */
	public StatisticsCollection(final HashMap<StatName, Statistic> aStatsMap,
			final Vector<Statistic> aConvergeStats) {
		this();
		this.statsMap = aStatsMap;
		this.convergeStats = aConvergeStats;
	}

	public void printFullHistory() {
		//Sim.debug(2,"Printing Full History");
		try {
			// file write appending mode
			logFile = new FileWriter(
					new File("fanyao_statistics_" + sDate), true);
		} catch (IOException e) {
			e.printStackTrace();

		}
		Iterator<Entry<TimeWeightedStatName, TimeWeightedStatistic>> it = twStatsMap
				.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<TimeWeightedStatName, TimeWeightedStatistic> pairs = it
					.next();
			TimeWeightedStatistic twStatistics = (TimeWeightedStatistic) pairs
					.getValue();
			twStatistics.printFullHistory();
		}

	}

	/**
	 * Gets a statistic by name.
	 * 
	 * @param name
	 *            - the name of the statistic
	 * @return the statistic
	 */
	public Statistic getStat(final StatName name) {
		Statistic stat = this.statsMap.get(name);

		if (stat == null) {
			stat = this.fakeStatistic;
		}

		return stat;
	}

	/**
	 * Gets a time weighted statistic by name.
	 * 
	 * @param name
	 *            - the name of the statistic
	 * @return the statistic
	 */
	public TimeWeightedStatistic getTimeWeightedStat(
			final TimeWeightedStatName name) {
		TimeWeightedStatistic stat = this.twStatsMap.get(name);

		if (stat == null) {
			stat = this.twFakeStatistic;
		}

		return stat;
	}

	/**
	 * Checks if all the needed statistics have converged.
	 * 
	 * @return if all the needed statistics have converged
	 */
	public boolean allStatsConverged() {

		boolean allConverged = true;
		Iterator<Statistic> iter = this.convergeStats.iterator();

		while (iter.hasNext()) {
			Statistic stat = iter.next();
			if (!stat.isConverged()) {
				allConverged = false;
			}
		}

		return allConverged;
	}

	public void flushStatistics() {
		// Use Runnable interface not as an thread
		// FIXME:currently only flush time weighted statistics
		RawStatLogger logWriter = new RawStatLogger();
		Iterator<Map.Entry<TimeWeightedStatName, TimeWeightedStatistic>> it = twStatsMap
				.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<TimeWeightedStatName, TimeWeightedStatistic> entry = it
					.next();
			logWriter.setNameAndStats(entry.getKey(), entry.getValue()
					.getFullHistory());
			logWriter.run();
		}
	}

	/**
	 * Reports that a statistic is warm by outputting a message, removing it
	 * from the collection of statistics that need to be warmed and notifying
	 * all the other statistics that warm up is done once the final statistic is
	 * warmed up.
	 * 
	 * @param statistic
	 *            - the statistic to report
	 */
	public void reportWarmed(final Statistic statistic) {
		System.out.println(statistic.getStatName() + " reported it is warm");
		this.warmStats.remove(statistic);

		if (warmStats.isEmpty()) {
			Iterator<Statistic> iter = this.convergeStats.iterator();
			while (iter.hasNext()) {
				Statistic stat = iter.next();
				stat.setOtherStatsWarmed(true);
			}
		}
	}

	/**
	 * Adds a statistic to the collection of stats that need to be warmed up.
	 * 
	 * @param stat
	 *            - the statistic
	 */
	public void setWarmupStat(final StatName stat) {
		this.warmStats.add(this.getStat(stat));
	}

	/**
	 * Prints the convergence status of the statistics that need to converge.
	 */
	public void printConvergedOutputs() {
		Iterator<Statistic> iter = this.convergeStats.iterator();
		while (iter.hasNext()) {
			Statistic stat = iter.next();
			System.out.println(stat.getStatName() + " Average "
					+ stat.getAverage());
			System.out.println(stat.getStatName() + " Quantile("
					+ stat.getQuantileSetting() + "): "
					+ stat.getQuantile(stat.getQuantileSetting()));
		}
	}

	/**
	 * Returns an interator of the statistics in the collection.
	 * 
	 * @return an interator of the statistics in the collection.
	 */
	public Iterator<Statistic> getAllStats() {
		return this.convergeStats.iterator();
	}

	/**
	 * Adds a statistic to the collection.
	 * 
	 * @param name
	 *            - the name of the statistic
	 * @param stat
	 *            - the statistic
	 */
	public void addStatistic(final StatName name, final Statistic stat) {

		if (this.statsMap.get(name) != null) {
			Sim.fatalError("Already added " + name);
		}

		this.statsMap.put(name, stat);
		this.convergeStats.add(stat);
	}

	/**
	 * Adds a time weighted statistic to the collection.
	 * 
	 * @param name
	 *            - the name of the statistic
	 * @param stat
	 *            - the statistic
	 */
	public void addTimeWeightedStatistic(final TimeWeightedStatName name,
			final TimeWeightedStatistic stat) {

		if (this.twStatsMap.get(name) != null) {
			Sim.fatalError("Already added " + name);
		}

		this.twStatsMap.put(name, stat);
		this.convergeStats.add(stat);
	}

	// TODO comment these once we find out if they're still needed
	private final class FakeStatistic extends Statistic {

		public FakeStatistic() {
			super(null, null, 0, 0, 0, 0);

		}

		@Override
		public void addSample(final double value) {

		}

	}

	private final class FakeTimeWeightedStatistic extends TimeWeightedStatistic {

		public FakeTimeWeightedStatistic() {
			super(null, null, 0, 0, 0, 0, 0);
		}

		@Override
		public void addSample(final double value, final double time) {

		}

	}

	/**
	 * Calls {@link Statistic#printStatInfo()} on all the statistics.
	 */
	public void printAllStatInfo() {
		Iterator<Statistic> iter = this.convergeStats.iterator();
		while (iter.hasNext()) {
			Statistic stat = iter.next();
			stat.printStatInfo();
		}
	}

	/**
	 * Checks if all the statistics are in steady state.
	 * 
	 * @return if all the statistics are in steady state.
	 */
	public boolean allStatsSteadyState() {
		boolean allSteadyState = true;
		Iterator<Statistic> iter = this.convergeStats.iterator();

		while (iter.hasNext()) {
			Statistic stat = iter.next();
			if (!stat.isSteadyState()) {
				allSteadyState = false;
			}
		}

		return allSteadyState;
	}

	/**
	 * Combines this statistic collection with another one.
	 * 
	 * @param stats
	 *            - the statistic collection to combine with this one
	 * @return the combined collection
	 */
	public StatisticsCollection combine(final StatisticsCollection stats) {
		HashMap<StatName, Statistic> tempStatsMap = new HashMap<StatName, Statistic>();
		Set<StatName> keys = this.statsMap.keySet();
		Iterator<StatName> iter = keys.iterator();
		Vector<Statistic> tempConvergedStats = new Vector<Statistic>();
		while (iter.hasNext()) {
			StatName key = iter.next();
			Statistic myStat = this.statsMap.get(key);
			Statistic theirStat = stats.getStat(key);
			Statistic combinedStat = myStat.combineStatistics(theirStat);
			tempStatsMap.put(key, combinedStat);
			tempConvergedStats.add(combinedStat);
		}
		StatisticsCollection combinedCollection = new StatisticsCollection(
				tempStatsMap, tempConvergedStats);

		return combinedCollection;
	}

}
