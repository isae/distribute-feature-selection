package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.FitCriterion;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.feature.SpearmanRankCorrelation;
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty;
import ru.ifmo.ctddev.isaev.feature.measure.VDM;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;
import ru.ifmo.ctddev.isaev.folds.SequentalEvaluator;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelNopMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.util.Collections;
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
                .filter(clf -> clf == Classifiers.SVM)
                .map(clf -> {
                    LOGGER.info("Classifier: {}", clf);
                    List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
                    Collections.shuffle(order);
                    FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                            clf,
                            new PreferredSizeFilter(100), new OrderSplitter(10, order)
                    );
                    AlgorithmConfig config = new AlgorithmConfig(0.1, foldsEvaluator, measures);
                    ParallelMeLiF meLiF = new ParallelMeLiF(config, dataSet, 20);
                    RunStats result = meLiF.run(points);
                    return result;
                })
                .collect(Collectors.toList());
        RunStats svmStats = allStats.stream().filter(s -> s.getUsedClassifier() == Classifiers.SVM).findAny().get();
        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
        Collections.shuffle(order);
        FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                Classifiers.SVM,
                new PreferredSizeFilter(100), new OrderSplitter(10, order)
        );
        AlgorithmConfig nopMelifConfig = new AlgorithmConfig(0.1, foldsEvaluator, measures);
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
