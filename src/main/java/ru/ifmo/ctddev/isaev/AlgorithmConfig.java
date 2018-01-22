package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;


/**
 * @author iisaev
 */
public class AlgorithmConfig {
    private final double delta;

    private final FoldsEvaluator foldsEvaluator;

    private final RelevanceMeasure[] measures;

    public AlgorithmConfig(double delta, FoldsEvaluator foldsEvaluator, RelevanceMeasure[] measures) {
        this.delta = delta;
        this.foldsEvaluator = foldsEvaluator;
        this.measures = measures;
    }

    public double getDelta() {
        return delta;
    }


    public RelevanceMeasure[] getMeasures() {
        return measures;
    }

    public FoldsEvaluator getFoldsEvaluator() {
        return foldsEvaluator;
    }
}
