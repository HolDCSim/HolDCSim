
package event;

import java.io.Serializable;
import java.util.Date;

import constants.Constants.StatName;
import constants.Constants.TimeWeightedStatName;

import stat.Statistic;
import stat.StatisticsCollection;
import stat.TimeWeightedStatistic;

public final class ExperimentOutput implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;
    
    public static Date expDate;

    /**
     * The collection of statistics for the simulation.
     */
    private StatisticsCollection statisticsCollection;

    /**
     * Creates a new ExperimentOutput.
     */
    public ExperimentOutput() {
    	expDate = new Date();
        this.statisticsCollection = new StatisticsCollection();

    }

    /**
     * Adds an output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     */
    public void addOutput(final StatName name,
                          final double meanPrecision,
                          final double quantile,
                          final double quantilePrecision,
                          final int warmupSamples) {
        Statistic stat = new Statistic(statisticsCollection,
                                       name,
                                       warmupSamples,
                                       meanPrecision,
                                       quantile,
                                       quantilePrecision);
        this.statisticsCollection.addStatistic(name, stat);
    }

    /**
     * Adds an output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     * @param xValues - The x values for the histogram of the output
     */
    public void addOutput(final StatName name,
                          final double meanPrecision,
                          final double quantile,
                          final double quantilePrecision,
                          final int warmupSamples,
                          final double[] xValues) {
        Statistic stat = new Statistic(statisticsCollection,
                                       name,
                                       warmupSamples,
                                       meanPrecision,
                                       quantile,
                                       quantilePrecision,
                                       xValues);
        this.statisticsCollection.addStatistic(name, stat);
    }

    /**
     * Adds a time-weigthed output to be observed by the simulation.
     *
     * @param name - The name of the simulation
     * @param meanPrecision - The precision on the mean estimate
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param quantile - The quantile to ensure precision on
     * @param quantilePrecision - the precision for the quantile
     * (e.g., .05 is less than 5% error with 95% confidence)
     * @param warmupSamples - The number of warmup samples.
     * There is no de facto way to determine what this value should be.
     * @param window - The window (in seconds) over which to take samples.

     */
    public void addTimeWeightedOutput(final TimeWeightedStatName name,
                                      final double meanPrecision,
                                      final double quantile,
                                      final double quantilePrecision,
                                      final int warmupSamples,
                                      final double window) {
        TimeWeightedStatistic stat
            = new TimeWeightedStatistic(statisticsCollection,
                                        name,
                                        warmupSamples,
                                        meanPrecision,
                                        quantile,
                                        quantilePrecision,
                                        window);
        this.statisticsCollection.addTimeWeightedStatistic(name, stat);
    }

    /**
     * Gets the statistics collection for the output.
     * @return - the statistics collection
     */
    public StatisticsCollection getStats() {
        return this.statisticsCollection;
    }
    
    public void printFullHistory(){
    	this.statisticsCollection.printFullHistory();
    }

    /**
     * Get an individual statistic of the output.
     * @param statName - the name of the individual statistic
     * @return the statistic
     */
    public Statistic getStat(final StatName statName) {
        return this.statisticsCollection.getStat(statName);
    }

    /**
     * Get an individual statistic of the output.
     * @param statName - the name of the individual statistic
     * @return the statistic
     */
    public Statistic getTimeWeightedStat(final TimeWeightedStatName statName) {
        return this.statisticsCollection.getTimeWeightedStat(statName);
    }

}
