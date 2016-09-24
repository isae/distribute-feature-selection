package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;
import ru.ifmo.ctddev.isaev.folds.SequentalEvaluator;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class BasicRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicRunner.class);

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
        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
        Collections.shuffle(order);
        FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                Classifiers.SVM,
                new PreferredSizeFilter(100), new OrderSplitter(10, order)
        );
        AlgorithmConfig config = new AlgorithmConfig(0.1, foldsEvaluator, measures);
        LocalDateTime startTime = LocalDateTime.now();
        BasicMeLiF meLif = new BasicMeLiF(config, dataSet);
        RunStats runStats = meLif.run(points);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished BasicMeLiF at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("BasicMeLiF work time: {} seconds", starWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                runStats.getVisitedPoints(),
                runStats.getBestResult().getPoint(),
                runStats.getBestResult().getF1Score()
        });
    }
}
