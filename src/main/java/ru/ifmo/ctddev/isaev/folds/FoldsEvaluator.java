package ru.ifmo.ctddev.isaev.folds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.ScoreCalculator;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public abstract class FoldsEvaluator {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public abstract SelectionResult getSelectionResult(DataSet dataSet, Point point, RunStats stats);

    protected final DataSetSplitter dataSetSplitter;

    protected final Classifiers classifiers;

    protected static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    public FoldsEvaluator(Classifiers classifiers, DataSetSplitter dataSetSplitter, DataSetFilter dataSetFilter) {
        this.dataSetSplitter = dataSetSplitter;
        this.dataSetFilter = dataSetFilter;
        this.classifiers = classifiers;
    }

    protected final DataSetFilter dataSetFilter;

    protected double getF1Score(DataSetPair dsPair) {
        Classifier classifier = classifiers.newClassifier();
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

    public abstract String getName();

    protected double getF1Score(FeatureDataSet filteredDs) {
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = dataSetSplitter.split(instanceDataSet)
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        return f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    protected double getF1Score(DataSet dataSet, Point point, RelevanceMeasure[] measures) {
        FeatureDataSet filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, measures);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = dataSetSplitter.split(instanceDataSet)
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        return f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    public DataSetSplitter getDataSetSplitter() {
        return dataSetSplitter;
    }

    public Classifiers getClassifiers() {
        return classifiers;
    }

    public static ScoreCalculator getScoreCalculator() {
        return scoreCalculator;
    }

    public DataSetFilter getDataSetFilter() {
        return dataSetFilter;
    }
}
