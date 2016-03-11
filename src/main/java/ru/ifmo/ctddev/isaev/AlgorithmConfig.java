package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataSet;


/**
 * @author iisaev
 */
public class AlgorithmConfig {
    private final double delta;

    private final int folds;

    private final int testPercent;

    private final DataSet initialDataset;

    private final int featureCount;

    public AlgorithmConfig(double delta, int folds, int testPercent, DataSet initialDataset, int featureCount) {
        this.delta = delta;
        this.folds = folds;
        this.testPercent = testPercent;
        this.initialDataset = initialDataset;
        this.featureCount = featureCount;
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

    public DataSet getInitialDataset() {
        return initialDataset;
    }

    public int getFeatureCount() {
        return featureCount;
    }
}
