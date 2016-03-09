package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.SVM;
import ru.ifmo.ctddev.isaev.dataset.*;
import ru.ifmo.ctddev.isaev.feature.DatasetFilter;
import ru.ifmo.ctddev.isaev.feature.FitCriterion;
import ru.ifmo.ctddev.isaev.feature.VDM;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class SimpleMeLiF {
    public static void main(String[] args) {
        DataSet dataSet = datasetReader.readCsv(args[0]);
        new SimpleMeLiF(dataSet).run();
    }

    protected final Set<Point> visitedPoints = new TreeSet<>();

    private final DataSet dataSet;

    private static final DatasetReader datasetReader = new DatasetReader();

    private static final DatasetFilter datasetFilter = new DatasetFilter();

    private static final DatasetSplitter datasetSplitter = new DatasetSplitter();

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    public SimpleMeLiF(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    private static final double DELTA = 0.3;

    private void run() {
        double score1 = performCoordinateDescend(new Point(1, 0));
        double score2 = performCoordinateDescend(new Point(0, 1));
        double score3 = performCoordinateDescend(new Point(1, 1));
        System.out.println("Total scores: ");
        System.out.println(score1);
        System.out.println(score2);
        System.out.println(score3);
    }

    protected Double visitPoint(Point point, double baseScore) {
        if (!visitedPoints.contains(point)) {
            double score = getF1Score(point);
            visitedPoints.add(new Point(point));
            if (score > baseScore) {
                return score;
            }
        }
        return null;
    }

    protected double performCoordinateDescend(Point point) {
        double baseScore = getF1Score(point);
        //writeResults(folder, baseScore, point.getCoordinates());
        visitedPoints.add(new Point(point));

        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            double[] coordinates = point.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                double initCoordValue = coordinates[i];
                coordinates[i] = initCoordValue + DELTA;
                Double currentScore = visitPoint(point, baseScore);
                if (currentScore != null) {
                    baseScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue - DELTA;
                currentScore = visitPoint(point, baseScore);
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

    private double getF1Score(Point point) {
        FeatureDataSet filteredDs = datasetFilter.filterDataset(dataSet.toFeatureSet(), 100, point, new VDM(), new FitCriterion());
        InstanceDataSet instanceDataSet = filteredDs.toInstanceSet();
        List<Double> f1Scores = datasetSplitter.splitRandomly(instanceDataSet, 20, 5).stream().map(dsPair -> {
            Classifier classifier = new SVM();
            classifier.train(dsPair.getTrainSet());
            List<Double> predictedValues = classifier.test(dsPair.getTestSet());
            List<Integer> rounded = predictedValues.stream().map(d -> (int) Math.round(d)).collect(Collectors.toList());
            List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
            return scoreCalculator.calculateF1Score(expectedValues, rounded);
        }).collect(Collectors.toList());
        return f1Scores.stream().mapToDouble(d -> d).average().getAsDouble();
    }
}
