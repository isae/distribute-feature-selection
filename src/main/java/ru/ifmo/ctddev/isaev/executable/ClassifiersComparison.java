package ru.ifmo.ctddev.isaev.executable;

import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelNopMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.RandomSplitter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class ClassifiersComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassifiersComparison.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1)
        };
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        List<RunStats> allStats = IntStream.range(0, Classifiers.values().length)
                .mapToObj(i -> Classifiers.values()[i])
                .filter(clf -> clf == Classifiers.WEKA_SVM)
                .map(clf -> {
                    LOGGER.info("Classifier: {}", clf);
                    AlgorithmConfig config = new AlgorithmConfig(0.1, clf, measures);
                    config.setDataSetSplitter(new RandomSplitter(20, 3));
                    config.setDataSetFilter(new PreferredSizeFilter(100));
                    ParallelMeLiF meLiF = new ParallelMeLiF(config, dataSet, 20);
                    RunStats result = meLiF.run(points);
                    return result;
                })
                .collect(Collectors.toList());
        RunStats svmStats = allStats.stream().filter(s -> s.getUsedClassifier() == Classifiers.WEKA_SVM).findAny().get();
        AlgorithmConfig nopMelifConfig = new AlgorithmConfig(0.1, Classifiers.WEKA_SVM, measures);
        nopMelifConfig.setDataSetSplitter(new RandomSplitter(20, 3));
        nopMelifConfig.setDataSetFilter(new PreferredSizeFilter(100));
        RunStats nopMelifStats = new ParallelNopMeLiF(nopMelifConfig, 20, (int) svmStats.getVisitedPoints()).run(points);
        allStats.forEach(stats ->
                LOGGER.info("Classifier: {}; f1Score: {}; work time: {} seconds; visited points: {}", new Object[] {
                        stats.getUsedClassifier(),
                        stats.getBestResult().getF1Score(),
                        stats.getWorkTime(),
                        stats.getVisitedPoints()
                }));
        LOGGER.info("Nop classifier work time: {}; visitedPoints: {}", new Object[] {
                nopMelifStats.getWorkTime(),
                nopMelifStats.getVisitedPoints()
        });
        LOGGER.info("Percent of time spent to classifying for svm: {}%",
                getPercentImprovement(
                        svmStats.getWorkTime() / svmStats.getVisitedPoints(),
                        nopMelifStats.getWorkTime() / nopMelifStats.getVisitedPoints()
                ));
    }
}
