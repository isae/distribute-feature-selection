package ru.ifmo.ctddev.isaev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.SVM;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.feature.DatasetFilter;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class SimpleMeLiF implements MeLiF {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final Set<Point> visitedPoints = new TreeSet<>();

    protected static final DatasetFilter datasetFilter = new DatasetFilter();

    protected static final DatasetSplitter datasetSplitter = new DatasetSplitter();

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    protected final AlgorithmConfig config;

    public SimpleMeLiF(AlgorithmConfig config) {
        this.config = config;
    }

    @Override
    public RunStats run(Point[] points, RelevanceMeasure[] measures) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != measures.length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        RunStats runStats = new RunStats();
        runStats.setMeasures(measures);

        LocalDateTime startTime = LocalDateTime.now();
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
        logger.info("Finished {} at {}", getClass().getSimpleName(), finishTime);
        logger.info("Working time: {} seconds", ChronoUnit.SECONDS.between(startTime, finishTime));
        return runStats;
    }

    protected SelectionResult visitPoint(Point point, RunStats measures, SelectionResult bestResult) {
        if (!visitedPoints.contains(point)) {
            SelectionResult score = getSelectionResult(point, measures);
            visitedPoints.add(new Point(point));
            if (score.compareTo(bestResult) == 1) {
                return score;
            }
        }
        return null;
    }

    protected SelectionResult performCoordinateDescend(Point point, RunStats runStats) {
        SelectionResult bestScore = getSelectionResult(point, runStats);
        visitedPoints.add(point);

        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            double[] coordinates = point.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                double initCoordValue = coordinates[i];
                coordinates[i] = initCoordValue + config.getDelta();
                SelectionResult currentScore = visitPoint(point, runStats, bestScore);
                if (currentScore != null) {
                    bestScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue - config.getDelta();
                currentScore = visitPoint(point, runStats, bestScore);
                if (currentScore != null) {
                    bestScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue;
            }
        }
        return bestScore;
    }

    protected double getF1Score(DataSetPair dsPair) {
        Classifier classifier = new SVM();
        classifier.train(dsPair.getTrainSet());
        List<Double> predictedValues = classifier.test(dsPair.getTestSet());
        List<Integer> rounded = predictedValues.stream().map(d -> (int) Math.round(d)).collect(Collectors.toList());
        List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
        logger.trace("Expected values: {}", Arrays.toString(expectedValues.toArray()));
        logger.trace("Actual values: {}", Arrays.toString(rounded.toArray()));
        return scoreCalculator.calculateF1Score(expectedValues, rounded);
    }

    protected SelectionResult getSelectionResult(Point point, RunStats stats) {
        FeatureDataSet filteredDs = datasetFilter.filterDataset(config.getInitialDataset().toFeatureSet(), config.getFeatureCount(), point, stats);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.splitRandomly(instanceDataSet, config.getTestPercent(), config.getFolds())
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        double f1Score = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
        logger.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), f1Score);
        SelectionResult result = new SelectionResult(filteredDs.getFeatures(), point, f1Score);
        stats.updateBestResult(result);
        return result;
    }
}
