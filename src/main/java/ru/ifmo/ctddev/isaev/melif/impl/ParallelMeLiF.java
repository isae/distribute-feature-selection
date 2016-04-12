package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;

import java.util.*;
import java.util.concurrent.*;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class ParallelMeLiF extends BasicMeLiF {
    private final ExecutorService executorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelMeLiF.class);

    public ExecutorService getExecutorService() {
        return executorService;
    }

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    public ParallelMeLiF(AlgorithmConfig config, DataSet dataSet, int threads) {
        this(config, dataSet, Executors.newFixedThreadPool(threads));
    }

    public ParallelMeLiF(AlgorithmConfig config, DataSet dataSet, ExecutorService executorService) {
        super(config, dataSet);
        this.executorService = executorService;
    }

    @Override
    public RunStats run(Point[] points) {
        RunStats result = super.run(points);
        getExecutorService().shutdown();
        return result;
    }

    @Override
    protected SelectionResult visitPoint(Point point, RunStats measures, SelectionResult bestResult) {
        SelectionResult score = getSelectionResult(point, measures);
        visitedPoints.add(new Point(point));
        return score;
    }

    protected SelectionResult performCoordinateDescend(Point point, RunStats runStats) {
        SelectionResult bestScore = getSelectionResult(point, runStats);
        visitedPoints.add(point);
        if (runStats.getBestResult() != null && runStats.getScore()>bestScore.getF1Score()) {
            bestScore = runStats.getBestResult();
        }
        runStats.updateBestResultUnsafe(bestScore);

        double[] coordinates = point.getCoordinates();
        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            for (int i = 0; i < coordinates.length; i++) {
                CountDownLatch latch = new CountDownLatch(2);

                Point plusDelta = new Point(coordinates);
                plusDelta.getCoordinates()[i] += config.getDelta();
                Future<SelectionResult> plusDeltaScore = getSelectionResultFuture(runStats, bestScore, plusDelta, latch);

                Point minusDelta = new Point(coordinates);
                minusDelta.getCoordinates()[i] -= config.getDelta();
                Future<SelectionResult> minusDeltaScore = getSelectionResultFuture(runStats, bestScore, minusDelta, latch);

                try {
                    latch.await();
                    if (plusDeltaScore.get().betterThan(bestScore)) {
                        bestScore = plusDeltaScore.get();
                        runStats.updateBestResultUnsafe(plusDeltaScore.get());
                        coordinates = plusDelta.getCoordinates();
                        smthChanged = true;
                    }
                    if (minusDeltaScore.get().betterThan(bestScore)) {
                        bestScore = minusDeltaScore.get();
                        runStats.updateBestResultUnsafe(minusDeltaScore.get());
                        coordinates = minusDelta.getCoordinates();
                        smthChanged = true;
                    }
                    if (smthChanged) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new IllegalArgumentException("Waiting on latch interrupted!");
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bestScore;
    }

    private Future<SelectionResult> getSelectionResultFuture(RunStats runStats, SelectionResult bestScore, Point plusDelta, CountDownLatch latch) {
        Future<SelectionResult> plusDeltaScore;
        if (!visitedPoints.contains(plusDelta)) {
            plusDeltaScore = executorService.submit(() -> {
                SelectionResult result = visitPoint(plusDelta, runStats, bestScore);
                latch.countDown();
                return result;
            });
        } else {
            latch.countDown();
            plusDeltaScore = CompletableFuture.completedFuture(bestScore);
        }
        return plusDeltaScore;
    }

    protected SelectionResult getSelectionResult(Point point, RunStats stats) {
        FeatureDataSet filteredDs = datasetFilter.filterDataset(dataSet.toFeatureSet(), config.getFeatureCount(), point, stats);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<DataSetPair> dataSetPairs = datasetSplitter.splitRandomly(instanceDataSet, config.getTestPercent(), config.getFolds());
        CountDownLatch latch = new CountDownLatch(dataSetPairs.size());
        List<Double> f1Scores = Collections.synchronizedList(new ArrayList<>(dataSetPairs.size()));
        dataSetPairs.forEach(ds -> executorService.submit(() -> {
            double score = getF1Score(ds);
            f1Scores.add(score);
            latch.countDown();
        }));
        try {
            latch.await();
            double f1Score = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
            LOGGER.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), f1Score);
            return new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting on latch interrupted! ", e);
        }
    }
}
