package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DatasetSplitter;
import filter.DatasetFilter;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;


/**
 * @author iisaev
 */
public class AlgorithmConfig {
    private final double delta;

    private final int folds;

    private final int testPercent;

    private final Classifiers classifiers;

    private final RelevanceMeasure[] measures;

    private DatasetFilter dataSetFilter;

    private DatasetSplitter dataSetSplitter;

    public AlgorithmConfig(double delta, int folds, int testPercent, Classifiers classifiers, RelevanceMeasure[] measures) {
        this.delta = delta;
        this.folds = folds;
        this.testPercent = testPercent;
        this.classifiers = classifiers;
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

    public Classifiers getClassifiers() {
        return classifiers;
    }

    public RelevanceMeasure[] getMeasures() {
        return measures;
    }

    public DatasetSplitter getDataSetSplitter() {
        return dataSetSplitter;
    }

    public DatasetFilter getDataSetFilter() {
        return dataSetFilter;
    }

    public void setDataSetFilter(DatasetFilter dataSetFilter) {
        this.dataSetFilter = dataSetFilter;
    }

    public void setDataSetSplitter(DatasetSplitter datasetSplitter) {
        this.dataSetSplitter = datasetSplitter;
    }
}
