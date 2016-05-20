package ru.ifmo.ctddev.isaev.melif.impl;

import filter.DatasetFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.ScoreCalculator;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.splitter.DatasetSplitter;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class FeatureSelectionAlgorithm {

    public static final DecimalFormat FORMAT = new DecimalFormat("#.###");

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    protected double getF1Score(DataSetPair dsPair) {
        Classifier classifier = config.getClassifiers().newClassifier();
        classifier.train(dsPair.getTrainSet());
        List<Integer> actual = classifier.test(dsPair.getTestSet())
                .stream()
                .map(d -> (int) Math.round(d))
                .collect(Collectors.toList());
        List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
        double result = scoreCalculator.calculateF1Score(expectedValues, actual);
        if (logger.isTraceEnabled()) {
            logger.trace("Expected values: {}", Arrays.toString(expectedValues.toArray()));
            logger.trace("Actual values: {}", Arrays.toString(actual.toArray()));
        }
        return result;
    }


    protected double getF1Score(Point point, RelevanceMeasure[] measures) {
        FeatureDataSet filteredDs = datasetFilter.filterDataSet(dataSet.toFeatureSet(), point, measures);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.split(instanceDataSet)
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        return f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    protected double getF1Score(FeatureDataSet filteredDs) {
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.split(instanceDataSet)
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        return f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    protected SelectionResult getSelectionResult(Point point, RunStats stats) {
        FeatureDataSet filteredDs = datasetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        double f1Score = getF1Score(point, stats.getMeasures());
        logger.debug("Point {}; F1 score: {}", point, FORMAT.format(f1Score));
        SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        stats.updateBestResultUnsafe(result);
        return result;
    }

}
