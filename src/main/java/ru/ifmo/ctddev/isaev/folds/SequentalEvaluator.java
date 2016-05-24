package ru.ifmo.ctddev.isaev.folds;

import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class SequentalEvaluator extends FoldsEvaluator {

    public SequentalEvaluator(Classifiers classifiers, DataSetFilter dataSetFilter, DataSetSplitter dataSetSplitter) {
        super(classifiers, dataSetSplitter, dataSetFilter);
    }


    public SelectionResult getSelectionResult(DataSet dataSet, Point point, RunStats stats) {
        FeatureDataSet filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        double f1Score = getF1Score(dataSet, point, stats.getMeasures());
        logger.debug("Point {}; F1 score: {}", point, FeatureSelectionAlgorithm.FORMAT.format(f1Score));
        SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        stats.updateBestResult(result);
        return result;
    }

    @Override
    public SelectionResult getSelectionResultPar(DataSet dataSet, Point point, RunStats stats, ExecutorService executorService) {
        FeatureDataSet filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<DataSetPair> dataSetPairs = dataSetSplitter.split(instanceDataSet);
        CountDownLatch latch = new CountDownLatch(dataSetPairs.size());
        List<Double> f1Scores = Collections.synchronizedList(new ArrayList<>(dataSetPairs.size()));
        List<Future> futures = dataSetPairs.stream().map(ds -> executorService.submit(() -> {
            double score = getF1Score(ds);
            f1Scores.add(score);
            latch.countDown();
        })).collect(Collectors.toList());
        try {
            latch.await();
            futures.forEach(f -> {
                assert f.isDone();
            });
            double f1Score = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
            logger.debug("Point {}; F1 score: {}", point, f1Score);
            SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
            stats.updateBestResult(result);
            return result;
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting on latch interrupted! ", e);
        }
    }

    @Override
    public String getName() {
        return "Seq";
    }
}
