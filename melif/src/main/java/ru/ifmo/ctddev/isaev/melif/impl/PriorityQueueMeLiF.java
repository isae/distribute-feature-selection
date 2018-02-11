package ru.ifmo.ctddev.isaev.melif.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSet;
import ru.ifmo.ctddev.isaev.PriorityExecutor;
import ru.ifmo.ctddev.isaev.SelectionResult;
import ru.ifmo.ctddev.isaev.melif.MeLiF;
import ru.ifmo.ctddev.isaev.point.Point;
import ru.ifmo.ctddev.isaev.results.RunStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class PriorityQueueMeLiF extends FeatureSelectionAlgorithm implements MeLiF {

    private static final double PRIORITY_INCREMENT = 0.1;

    private final PriorityExecutor executorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PriorityQueueMeLiF.class);

    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    private final List<Point> startingPoints = new ArrayList<>();

    public PriorityQueueMeLiF(AlgorithmConfig config, DataSet dataSet, int threads) {
        super(config, dataSet);
        int dimension = config.getMeasures().length;

        double[] allEqual = new double[dimension];
        Arrays.fill(allEqual, 1.0);
        startingPoints.add(new Point(allEqual));

        IntStream.range(0, dimension)
                .forEach(coord -> {
                    double[] coordinates = new double[dimension];
                    coordinates[coord] = 1.0;
                    coordinates[dimension - 1] = 1.0;
                    startingPoints.add(new Point(coordinates));
                });
        this.executorService = new PriorityExecutor(threads);
    }

    @Override
    public RunStats run(@NotNull Point[] points) {
        return run("PqMeLiF", 75);
    }

    @Override
    public RunStats run(@NotNull String name, @NotNull Point[] points, int latchSize) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());
        CountDownLatch latch = new CountDownLatch(latchSize);
        final Supplier<Boolean> stopCondition = () -> {
            latch.countDown();
            if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getScore() - 1.0) < 0.0001) {
                while (latch.getCount() != 0) {
                    latch.countDown();
                }
            }
            return latch.getCount() == 0;
        };
        startingPoints.forEach(point -> executorService.submitWithPriority(
                new PointProcessingTask(point, stopCondition, runStats),
                1.0
        ));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdownNow();
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getScore(),
                runStats.getBestResult().getPoint()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }

    class PointProcessingTask implements Callable<Double> {
        Point point;

        Supplier<Boolean> stopCondition;

        RunStats runStats;

        public PointProcessingTask(Point point, Supplier<Boolean> stopCondition, RunStats runStats) {
            this.point = point;
            this.stopCondition = stopCondition;
            this.runStats = runStats;
        }

        @Override
        public Double call() throws Exception {
            if (stopCondition.get()) {
                return 0.0;
            }
            if (visitedPoints.contains(point)) {
                logger.warn("Point is already processed: " + point);
                return 0.0;
            }
            logger.info("Processing point {}", point);
            SelectionResult res = foldsEvaluator.getSelectionResult(dataSet, point, runStats);
            visitedPoints.add(point);
            List<Point> neighbours = point.getNeighbours(config.getDelta());
            executorService.increaseTasksPriorities(PRIORITY_INCREMENT);
            neighbours.forEach(p -> {
                if (!visitedPoints.contains(p)) {
                    executorService.submitWithPriority(new PointProcessingTask(p, stopCondition, runStats),
                            res.getScore()
                    );
                }
            });
            return res.getScore();
        }
    }

    public RunStats run(String name, int latchSize) {
        return run(name, null, latchSize);
    }

    public RunStats runUntilNoImproveOnLastN(String name, Point[] points, int untilStop) {
        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());
        CountDownLatch latch = new CountDownLatch(1);
        final Supplier<Boolean> stopCondition = () -> {
            if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getScore() - 1.0) < 0.0001) {
                while (latch.getCount() != 0) {
                    latch.countDown();
                }
            }
            if (latch.getCount() == 0) {
                return true;
            }
            if (runStats.getNoImprove() > untilStop) {
                latch.countDown();
                return true;
            } else {
                return false;
            }
        };
        startingPoints.forEach(point -> executorService.submitWithPriority(
                new PointProcessingTask(point, stopCondition, runStats),
                1.0
        ));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        executorService.shutdownNow();
        LOGGER.info("Max score: {} at point {}",
                runStats.getBestResult().getScore(),
                runStats.getBestResult().getPoint()
        );
        runStats.setFinishTime(LocalDateTime.now());
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), runStats.getFinishTime());
        LOGGER.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }
}
