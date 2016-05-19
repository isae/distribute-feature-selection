package ru.ifmo.ctddev.isaev.melif.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.melif.MeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final Set<Point> visitedPoints = new TreeSet<>();

    public BasicMeLiF(AlgorithmConfig config, DataSet dataSet) {
        super(config, dataSet);
    }

    @Override
    public RunStats run(Point[] points) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != config.getMeasures().length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        RunStats runStats = new RunStats(config, dataSet);

        LocalDateTime startTime = LocalDateTime.now();
        runStats.setStartTime(startTime);
        logger.info("Started {} at {}", getClass().getSimpleName(), startTime);
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
        logger.info("Finished {} at {}", getClass().getSimpleName(), finishTime);
        logger.info("Working time: {} seconds", ChronoUnit.SECONDS.between(startTime, finishTime));
        return runStats;
    }

    protected SelectionResult visitPoint(Point point, RunStats measures, SelectionResult bestResult) {
        if (!visitedPoints.contains(point)) {
            SelectionResult score = getSelectionResult(point, measures);
            visitedPoints.add(new Point(point));
            return score;
        }
        return bestResult;
    }

    protected SelectionResult performCoordinateDescend(Point point, RunStats runStats) {
        SelectionResult bestScore = getSelectionResult(point, runStats);
        visitedPoints.add(point);
        if (runStats.getBestResult() != null && runStats.getScore() > bestScore.getF1Score()) {
            bestScore = runStats.getBestResult();
        }

        boolean smthChanged = true;
        double[] coordinates = point.getCoordinates();

        while (smthChanged) {
            smthChanged = false;

            for (int i = 0; i < coordinates.length; i++) {

                Point plusDelta = new Point(coordinates);
                plusDelta.getCoordinates()[i] += config.getDelta();
                SelectionResult plusScore = visitPoint(plusDelta, runStats, bestScore);
                if (plusScore.betterThan(bestScore)) {
                    bestScore = plusScore;
                    coordinates = plusDelta.getCoordinates();
                    smthChanged = true;
                    break;
                }

                Point minusDelta = new Point(coordinates);
                minusDelta.getCoordinates()[i] -= config.getDelta();
                SelectionResult minusScore = visitPoint(minusDelta, runStats, bestScore);
                if (minusScore.betterThan(bestScore)) {
                    bestScore = minusScore;
                    coordinates = minusDelta.getCoordinates();
                    smthChanged = true;
                    break;
                }
            }
        }
        return bestScore;
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

    protected SelectionResult getSelectionResult(Point point, RunStats stats) {
        FeatureDataSet filteredDs = datasetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.getMeasures());
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.split(instanceDataSet)
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        double f1Score = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
        logger.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), f1Score);
        SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        stats.updateBestResultUnsafe(result);
        return result;
    }
}
