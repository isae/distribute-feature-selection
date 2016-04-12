package ru.ifmo.ctddev.isaev.result;

import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;


/**
 * @author iisaev
 */
public class RunStats implements Comparable<RunStats> {
    private RelevanceMeasure[] measures;

    private long workTime;

    public double getScore() {
        return getBestResult().getF1Score() / workTime;
    }

    private Classifiers usedClassifier;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    public RunStats(AlgorithmConfig config) {
        this.measures = config.getMeasures();
        this.usedClassifier = config.getClassifiers();
    }

    public Classifiers getUsedClassifier() {
        return usedClassifier;
    }

    public long getWorkTime() {
        return workTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
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

    private static final class StatsHolder {
        private SelectionResult bestResult = null;

        private volatile long visitedPoints;
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

    public void updateBestResultUnsafe(SelectionResult bestResult) {
        ++holder.visitedPoints;
        if (holder.bestResult != null) {
            if (holder.bestResult.compareTo(bestResult) == -1) {
                holder.bestResult = bestResult;
            }
        } else {
            holder.bestResult = bestResult;
        }
    }

    public void setMeasures(RelevanceMeasure[] measures) {
        this.measures = measures;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }
}
