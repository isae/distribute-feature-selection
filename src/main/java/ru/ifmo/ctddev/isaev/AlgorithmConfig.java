package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public class AlgorithmConfig {
    private final double delta;

    private final int folds;

    private final int testPercent;

    private final Classifiers classifiers;

    private final int featureCount;

    private final RelevanceMeasure[] measures;

    public AlgorithmConfig(double delta, int folds, int testPercent, Classifiers classifiers, int featureCount, RelevanceMeasure[] measures) {
        this.delta = delta;
        this.folds = folds;
        this.testPercent = testPercent;
        this.classifiers = classifiers;
        this.featureCount = featureCount;
        this.measures = measures;
    }

    public double getDelta() {
        return delta;
    }

    public int getFolds() {
        return folds;
    }

    public int getTestPercent() {
        return testPercent;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public Classifiers getClassifiers() {
        return classifiers;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }
}
