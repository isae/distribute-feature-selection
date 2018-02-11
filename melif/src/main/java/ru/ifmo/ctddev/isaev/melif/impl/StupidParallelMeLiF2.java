package ru.ifmo.ctddev.isaev.melif.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSet;
import ru.ifmo.ctddev.isaev.SelectionResult;
import ru.ifmo.ctddev.isaev.point.Point;
import ru.ifmo.ctddev.isaev.results.RunStats;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * Implementation, that evaluates each point in separate thread
 *
 * @author iisaev
 */
public class StupidParallelMeLiF2 extends ParallelMeLiF {

    private static final Logger LOGGER = LoggerFactory.getLogger(StupidParallelMeLiF2.class);


    protected final Set<Point> visitedPoints = new ConcurrentSkipListSet<>();

    public StupidParallelMeLiF2(AlgorithmConfig config, DataSet dataSet) {
        this(config, dataSet, Executors.newFixedThreadPool(100 / config.getFoldsEvaluator().getDataSetSplitter().getTestPercent()));
    }

    public StupidParallelMeLiF2(AlgorithmConfig config, DataSet dataSet, ExecutorService executorService) {
        super(config, dataSet, executorService);
    }

    @Override
    public RunStats run(@NotNull String name, @NotNull Point[] points, int pointsToVisit) {
        return run(name, points, true);
    }

    @Override
    public RunStats run(@NotNull Point[] points) {
        return run("Stupid", points, true);
    }

    @Override
    public RunStats run(String name, Point[] points, boolean shutdown) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != config.getMeasures().length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        RunStats runStats = new RunStats(config, dataSet, name);
        logger.info("Started {} at {}", name, runStats.getStartTime());

        logger.info("Started {} at {}", getClass().getSimpleName(), runStats.getStartTime());
        List<SelectionResult> scores = Arrays.asList(points).stream()
                .map(p -> performCoordinateDescend(p, runStats))
                .collect(Collectors.toList());
        logger.info("Total scores: ");
        scores.stream().mapToDouble(SelectionResult::getScore).forEach(System.out::println);
        logger.info("Max score: {} at point {}",
                runStats.getBestResult().getScore(),
                runStats.getBestResult().getPoint()
        );
        LocalDateTime finishTime = LocalDateTime.now();
        runStats.setFinishTime(finishTime);
        logger.info("Finished {} at {}", getClass().getSimpleName(), finishTime);
        logger.info("Working time: {} seconds", runStats.getWorkTime());
        if (shutdown) {
            executorService.shutdown();
        }
        return runStats;
    }

    protected SelectionResult visitPoint(Point point, RunStats measures, SelectionResult bestResult) {
        if (!visitedPoints.contains(point)) {
            SelectionResult score = foldsEvaluator.getSelectionResult(dataSet, point, measures);
            visitedPoints.add(new Point(point));
            return score;
        }
        return bestResult;
    }
}
