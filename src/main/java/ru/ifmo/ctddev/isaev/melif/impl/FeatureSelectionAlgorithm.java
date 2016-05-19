package ru.ifmo.ctddev.isaev.melif.impl;

import filter.DatasetFilter;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.ScoreCalculator;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.splitter.DatasetSplitter;

/**
 * @author iisaev
 */
public class FeatureSelectionAlgorithm {
    protected final DatasetFilter datasetFilter;

    protected final DatasetSplitter datasetSplitter;

    protected static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    protected final AlgorithmConfig config;

    protected final DataSet dataSet;

    public FeatureSelectionAlgorithm(AlgorithmConfig config, DataSet dataSet) {
        this.config = config;
        this.datasetSplitter = config.getDataSetSplitter();
        this.datasetFilter = config.getDataSetFilter();
        this.dataSet = dataSet;
    }

}
