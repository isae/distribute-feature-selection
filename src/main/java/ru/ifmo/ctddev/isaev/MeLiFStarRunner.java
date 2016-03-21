package ru.ifmo.ctddev.isaev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        AlgorithmConfig config = new AlgorithmConfig(0.3, 5, Runtime.getRuntime().availableProcessors(), dataSet, 100);
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1)
        };
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        LocalDateTime startTime = LocalDateTime.now();
        MeLifStar meLifStar = new MeLifStar(config, 20);
        meLifStar.run(points, measures);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MeLifStar at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("Star work time: {} seconds", starWorkTime);
        meLifStar.getExecutorService().shutdown();
    }
}
