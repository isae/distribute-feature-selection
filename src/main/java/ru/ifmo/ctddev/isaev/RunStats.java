package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public class RunStats {
    private RelevanceMeasure[] measures;

    public void setMeasures(RelevanceMeasure[] measures) {
        this.measures = measures;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }
}
