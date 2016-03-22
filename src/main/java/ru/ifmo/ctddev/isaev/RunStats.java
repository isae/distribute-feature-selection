package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public class RunStats {
    private RelevanceMeasure[] measures;

    private static final class StatsHolder {
        private SelectionResult bestResult;
    }

    private final StatsHolder holder = new StatsHolder();

    public SelectionResult getBestResult() {
        return holder.bestResult;
    }

    public void updateBestResult(SelectionResult bestResult) {
        synchronized (holder) {
            if (holder.bestResult != null) {
                if (holder.bestResult.compareTo(bestResult) == -1) {
                    holder.bestResult = bestResult;
                }
            } else {
                holder.bestResult = bestResult;
            }
        }
    }

    public void setMeasures(RelevanceMeasure[] measures) {
        this.measures = measures;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }
}
