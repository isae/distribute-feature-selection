package ru.ifmo.ctddev.isaev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;


/**
 * @author iisaev
 */
public class SimpleMeLiF {
    public static void main(String[] args) {
        new SimpleMeLiF().run();
    }

    protected final Set<Point> visitedPoints = new TreeSet<>();

    private static final double DELTA = 0.3;

    private void run() {

    }

    protected Double visitPoint(Point point, File folder, double baseScore) throws IOException {
        if (!visitedPoints.contains(point)) {
            double score = getAUCScore(folder, point);
            visitedPoints.add(new Point(point));
            if (score > baseScore) {
                return score;
            }
        }
        return null;
    }

    protected void makeDescend(File folder, Point point) throws IOException {
        double baseScore = getAUCScore(folder, point);
        //writeResults(folder, baseScore, point.getCoordinates());
        visitedPoints.add(new Point(point));

        boolean smthChanged = true;

        while (smthChanged) {
            smthChanged = false;
            double[] coordinates = point.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                double initCoordValue = coordinates[i];
                coordinates[i] = initCoordValue + DELTA;
                Double currentScore = visitPoint(point, folder, baseScore);
                if (currentScore != null) {
                    baseScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue - DELTA;
                currentScore = visitPoint(point, folder, baseScore);
                if (currentScore != null) {
                    baseScore = currentScore;
                    smthChanged = true;
                    break;
                }
                coordinates[i] = initCoordValue;
            }
        }

    }

    private double getAUCScore(File folder, Point point) throws IOException {
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
