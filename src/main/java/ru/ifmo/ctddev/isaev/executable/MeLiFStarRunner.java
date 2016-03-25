package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.melif.impl.MeLifStar;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * @author iisaev
 */
public class MeLiFStarRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeLiFStarRunner.class);

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
        AlgorithmConfig config = new AlgorithmConfig(0.3, 5, 20, Classifiers.WEKA_SVM, 100, measures);
        LocalDateTime startTime = LocalDateTime.now();
        MeLifStar meLifStar = new MeLifStar(config, dataSet, 20);
        meLifStar.run(points);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MeLifStar at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("Star work time: {} seconds", starWorkTime);
        meLifStar.getExecutorService().shutdown();
    }
}
