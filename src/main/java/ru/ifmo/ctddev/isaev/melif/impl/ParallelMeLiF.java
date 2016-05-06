package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


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
        return run(points, true);
    }

    public RunStats run(Point[] points, boolean shutdown) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != config.getMeasures().length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        RunStats runStats = new RunStats(config);

        LocalDateTime startTime = LocalDateTime.now();
        runStats.setStartTime(startTime);
        LOGGER.info("Started {} at {}", getClass().getSimpleName(), startTime);
        CountDownLatch pointsLatch = new CountDownLatch(points.length);
        List<Future<SelectionResult>> scoreFutures = Arrays.asList(points).stream()
                .map(p -> executorService.submit(() -> {
                    SelectionResult result = performCoordinateDescend(p, runStats);
                    pointsLatch.countDown();
                    return result;
                })).collect(Collectors.toList());
        try {
            pointsLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        scoreFutures.forEach(f -> {
            assert f.isDone();
        });
        List<SelectionResult> scores = scoreFutures.stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        LOGGER.info("Total scores: ");
        scores.stream().mapToDouble(SelectionResult::getF1Score).forEach(System.out::println);
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getF1Score(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        LocalDateTime finishTime = LocalDateTime.now();
        runStats.setFinishTime(finishTime);
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), finishTime);
        LOGGER.info("Working time: {} seconds", ChronoUnit.SECONDS.between(startTime, finishTime));
        if (shutdown) {
            getExecutorService().shutdown();
        }
        return runStats;
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
        if (runStats.getBestResult() != null && runStats.getScore() > bestScore.getF1Score()) {
            bestScore = runStats.getBestResult();
        }

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

                    assert plusDeltaScore.isDone();
                    assert minusDeltaScore.isDone();

                    if (plusDeltaScore.get().betterThan(bestScore)) {
                        bestScore = plusDeltaScore.get();
                        coordinates = plusDelta.getCoordinates();
                        smthChanged = true;
                    }
                    if (minusDeltaScore.get().betterThan(bestScore)) {
                        bestScore = minusDeltaScore.get();
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
        FeatureDataSet filteredDs = datasetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
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
            LOGGER.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), f1Score);
            SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
            stats.updateBestResult(result);
            return result;
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting on latch interrupted! ", e);
        }
    }
}
