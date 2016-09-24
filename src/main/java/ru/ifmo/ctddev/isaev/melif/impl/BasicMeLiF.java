package ru.ifmo.ctddev.isaev.melif.impl;

import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.melif.MeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * Core single-threaded MeLiF implementation
 *
 * @author iisaev
 */
public class BasicMeLiF extends FeatureSelectionAlgorithm implements MeLiF {

    protected final Set<Point> visitedPoints = new TreeSet<>();

    public BasicMeLiF(AlgorithmConfig config, DataSet dataSet) {
        super(config, dataSet);
    }

    @Override
    public RunStats run(String name, Point[] points, int pointsToVisit) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != config.getMeasures().length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        RunStats runStats = new RunStats(config, dataSet, name);

        logger.info("Started {} at {}", name, runStats.getStartTime());
        List<SelectionResult> scores = Arrays.asList(points).stream()
                .map(p -> performCoordinateDescend(p, runStats))
                .collect(Collectors.toList());
        logger.info("Total scores: ");
        scores.stream().mapToDouble(SelectionResult::getF1Score).forEach(System.out::println);
        logger.info("Max score: {} at point {}",
                runStats.getBestResult().getF1Score(),
                runStats.getBestResult().getPoint().getCoordinates()
        );
        LocalDateTime finishTime = LocalDateTime.now();
        runStats.setFinishTime(finishTime);
        logger.info("Finished {} at {}", name, finishTime);
        logger.info("Working time: {} seconds", runStats.getWorkTime());
        return runStats;
    }

    @Override
    public RunStats run(Point[] points) {
        return run("Basic", points, 0);
    }

    protected SelectionResult visitPoint(Point point, RunStats measures, SelectionResult bestResult) {
        if (!visitedPoints.contains(point)) {
            SelectionResult score = foldsEvaluator.getSelectionResult(dataSet, point, measures);
            visitedPoints.add(new Point(point));
            return score;
        }
        return bestResult;
    }

    protected SelectionResult performCoordinateDescend(Point point, RunStats runStats) {
        SelectionResult bestScore = foldsEvaluator.getSelectionResult(dataSet, point, runStats);
        visitedPoints.add(point);
        if (runStats.getBestResult() != null && runStats.getScore() > bestScore.getF1Score()) {
            bestScore = runStats.getBestResult();
        }

        boolean smthChanged = true;
        double[] coordinates = point.getCoordinates();
        int gen = 0;
        while (smthChanged) {
            smthChanged = false;

            for (int i = 0; i < coordinates.length; i++) {
                if (runStats.getBestResult() != null && Math.abs(runStats.getBestResult().getF1Score() - 1.0) < 0.0001) {
                    return runStats.getBestResult();
                }
                final int finalI = i;
                Point plusDelta = new Point(gen, (c) -> c[finalI] += config.getDelta(), coordinates);
                SelectionResult plusScore = visitPoint(plusDelta, runStats, bestScore);

                if (plusScore.betterThan(bestScore)) {
                    bestScore = plusScore;
                    coordinates = plusDelta.getCoordinates();
                    smthChanged = true;
                    break;
                }

                Point minusDelta = new Point(gen, (c) -> c[finalI] -= config.getDelta(), coordinates);
                SelectionResult minusScore = visitPoint(minusDelta, runStats, bestScore);
                if (minusScore.betterThan(bestScore)) {
                    bestScore = minusScore;
                    coordinates = minusDelta.getCoordinates();
                    smthChanged = true;
                    break;
                }
            }
            ++gen;
        }
        return bestScore;
    }
}
