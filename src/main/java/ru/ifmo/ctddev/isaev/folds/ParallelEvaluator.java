package ru.ifmo.ctddev.isaev.folds;

import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.splitter.DatasetSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class ParallelEvaluator extends FoldsEvaluator {
    private final ExecutorService executorService;

    public ParallelEvaluator(Classifiers classifiers, DatasetSplitter datasetSplitter, DataSetFilter dataSetFilter, int threads) {
        this(classifiers, datasetSplitter, dataSetFilter, Executors.newFixedThreadPool(threads));
    }

    public ParallelEvaluator(Classifiers classifiers, DatasetSplitter datasetSplitter, DataSetFilter dataSetFilter, ExecutorService executorService) {
        super(classifiers, datasetSplitter, dataSetFilter);
        this.executorService = executorService;
    }

    public SelectionResult getSelectionResult(DataSet dataSet, Point point, RunStats stats) {
        FeatureDataSet filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<DataSetPair> dataSetPairs = datasetSplitter.split(instanceDataSet);
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
}
