package ru.ifmo.ctddev.isaev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.SVM;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.feature.DatasetFilter;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class SimpleMeLiF implements MeLiF {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeLiF.class);

    public static void main(String[] args) {
        DataSet dataSet = datasetReader.readCsv(args[0]);
        AlgorithmConfig config = new AlgorithmConfig(0.1, 10, 20, dataSet, 100);
        new SimpleMeLiF(config).run();
    }

    protected final Set<Point> visitedPoints = new TreeSet<>();

    private static final DataSetReader datasetReader = new DataSetReader();

    protected static final DatasetFilter datasetFilter = new DatasetFilter();

    protected static final DatasetSplitter datasetSplitter = new DatasetSplitter();

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    protected final AlgorithmConfig config;

    public SimpleMeLiF(AlgorithmConfig config) {
        this.config = config;
    }

    public void run() {

    }

    @Override
    public void run(Point[] points, RelevanceMeasure[] measures) {
        Arrays.asList(points).forEach(p -> {
            if (p.getCoordinates().length != measures.length) {
                throw new IllegalArgumentException("Each point must have same coordinates number as number of measures");
            }
        });

        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.info("Started {} at {}", getClass().getSimpleName(), startTime);
        List<Double> scores = Arrays.asList(points).stream()
                .map(p -> performCoordinateDescend(p, measures))
                .collect(Collectors.toList());
        LOGGER.info("Total scores: ");
        scores.forEach(System.out::println);
        LOGGER.info("Max score: {}", scores.stream().max(Comparator.comparingDouble(t -> t)).get());
        LocalDateTime finishTime = LocalDateTime.now();
        LOGGER.info("Finished {} at {}", getClass().getSimpleName(), finishTime);
        LOGGER.info("Working time: {} seconds", ChronoUnit.SECONDS.between(startTime, finishTime));
    }

    protected Double visitPoint(Point point, RelevanceMeasure[] measures, double baseScore) {
        if (!visitedPoints.contains(point)) {
            double score = getF1Score(point, measures);
            visitedPoints.add(new Point(point));
            if (score > baseScore) {
                return score;
            }
        }
        return null;
    }

    protected double performCoordinateDescend(Point point, RelevanceMeasure[] measures) {
        double baseScore = getF1Score(point, measures);
        visitedPoints.add(new Point(point));

        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            double[] coordinates = point.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                double initCoordValue = coordinates[i];
                coordinates[i] = initCoordValue + config.getDelta();
                Double currentScore = visitPoint(point, measures, baseScore);
                if (currentScore != null) {
                    baseScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue - config.getDelta();
                currentScore = visitPoint(point, measures, baseScore);
                if (currentScore != null) {
                    baseScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue;
            }
        }
        return baseScore;
    }

    protected double getF1Score(DataSetPair dsPair) {
        Classifier classifier = new SVM();
        classifier.train(dsPair.getTrainSet());
        List<Double> predictedValues = classifier.test(dsPair.getTestSet());
        List<Integer> rounded = predictedValues.stream().map(d -> (int) Math.round(d)).collect(Collectors.toList());
        List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
        LOGGER.trace("Expected values: {}", Arrays.toString(expectedValues.toArray()));
        LOGGER.trace("Actual values: {}", Arrays.toString(rounded.toArray()));
        return scoreCalculator.calculateF1Score(expectedValues, rounded);
    }

    protected double getF1Score(Point point, RelevanceMeasure[] measures) {
        FeatureDataSet filteredDs = datasetFilter.filterDataset(config.getInitialDataset().toFeatureSet(), config.getFeatureCount(), point, measures);
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.splitRandomly(instanceDataSet, config.getTestPercent(), config.getFolds())
                .stream().map(this::getF1Score)
                .collect(Collectors.toList());
        double result = f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
        LOGGER.debug("Point {}; F1 score: {}", Arrays.toString(point.getCoordinates()), result);
        return result;
    }
}
