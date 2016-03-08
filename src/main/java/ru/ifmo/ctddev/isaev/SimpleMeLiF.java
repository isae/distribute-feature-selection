package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;
import java.util.TreeSet;


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

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    public SimpleMeLiF(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    private static final double DELTA = 0.3;

    private void run() {
        performCoordinateDescend(new Point(1, 0, 0, 0));
        performCoordinateDescend(new Point(0, 1, 0, 0));
        performCoordinateDescend(new Point(0, 0, 1, 0));
        performCoordinateDescend(new Point(0, 0, 0, 1));
        performCoordinateDescend(new Point(1, 1, 1, 1));
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
        //EvaluateOptimisationPoint.main(new String[] {"start", folder.getAbsolutePath(), String.valueOf(point.getCoordinates()[0]), String.valueOf(point.getCoordinates()[1]), String.valueOf(point.getCoordinates()[2]), String.valueOf(point.getCoordinates()[3])});
        File subFolder = new File(folder.getAbsolutePath() + "//7_AUC_scores_WPCA_SVM//");
        double auc = 0;
        int experiments = 0;
        for (File experiment : subFolder.listFiles()) {
            if (!experiment.isDirectory()) {
                continue;
            }
            experiments++;
            BufferedReader br = new BufferedReader(new FileReader(experiment.listFiles()[0].getAbsolutePath() + "//rank"));
            auc += Double.parseDouble(br.readLine());
            br.close();
        }
        auc /= experiments;

        //BackupUtilities.storePoint(folder.getAbsolutePath(), point.getCoordinates(), auc);
        //BackupUtilities.deleteAllOddFolders(3, folder.getAbsolutePath() + File.separator);
        return auc;
    }
}
