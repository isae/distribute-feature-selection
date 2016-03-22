package ru.ifmo.ctddev.isaev.result;

import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;

import java.util.Collections;


/**
 * @author iisaev
 */
public class RunStats {
    private RelevanceMeasure[] measures;

    private static final class StatsHolder {
        private SelectionResult bestResult = new SelectionResult(Collections.emptyList(), new Point(), 0.0);

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
