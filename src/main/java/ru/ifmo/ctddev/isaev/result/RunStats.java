package ru.ifmo.ctddev.isaev.result;

import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.Classifiers;
import ru.ifmo.ctddev.isaev.DataSet;
import ru.ifmo.ctddev.isaev.SelectionResult;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;


/**
 * @author iisaev
 */
public class RunStats implements Comparable<RunStats> {
    private RelevanceMeasure[] measures;

    private long workTime;

    private final String dataSetName;

    private final int featureCount;

    private final int instanceCount;

    public double getScore() {
        return getBestResult().getScore() / workTime;
    }

    private final Classifiers usedClassifier;

    private final String algorithmName;

    private final LocalDateTime startTime;

    private LocalDateTime finishTime;

    public RunStats(AlgorithmConfig config, DataSet dataSet, String algorithmName) {
        this.measures = config.getMeasures();
        this.usedClassifier = config.getFoldsEvaluator().getClassifiers();
        this.dataSetName = dataSet.getName();
        this.featureCount = dataSet.getFeatureCount();
        this.instanceCount = dataSet.getInstanceCount();
        startTime = LocalDateTime.now();
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public Classifiers getUsedClassifier() {
        return usedClassifier;
    }

    public long getWorkTime() {
        return workTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
        this.workTime = ChronoUnit.SECONDS.between(startTime, finishTime);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    @Override
    public int compareTo(RunStats o) {
        return Comparator.comparingDouble(RunStats::getScore).compare(this, o);
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public int getNoImprove() {
        return holder.noImprove;
    }

    private static final class StatsHolder {
        private SelectionResult bestResult = null;

        private volatile int visitedPoints;

        private volatile int noImprove = 0;
    }

    private final StatsHolder holder = new StatsHolder();

    public SelectionResult getBestResult() {
        return holder.bestResult;
    }

    public long getVisitedPoints() {
        return holder.visitedPoints;
    }

    public void updateBestResult(SelectionResult bestResult) {
        synchronized (holder) {
            updateBestResultUnsafe(bestResult);
        }
    }

    private void updateBestResultUnsafe(SelectionResult bestResult) {
        ++holder.visitedPoints;
        ++holder.noImprove;
        if (holder.bestResult != null) {
            if (holder.bestResult.compareTo(bestResult) == -1) {
                holder.bestResult = bestResult;
                holder.noImprove = 0;
            }
        } else {
            holder.bestResult = bestResult;
            holder.noImprove = 0;
        }
    }

    public void setMeasures(RelevanceMeasure[] measures) {
        this.measures = measures;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }
}
