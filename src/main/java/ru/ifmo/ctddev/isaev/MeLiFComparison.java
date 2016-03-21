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
public class MeLiFComparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeLiFComparison.class);

    private static double getSpeedImprovementPercent(long prevSeconds, long curSeconds) {
        long diff = prevSeconds - curSeconds;
        return (double) diff / prevSeconds * 100;
    }

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
        LOGGER.info("Starting SimpleMeliF at {}", startTime);
        new SimpleMeLiF(config).run(points, measures);
        LocalDateTime simpleFinish = LocalDateTime.now();
        LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);
        ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, 20);
        parallelMeLiF.run(points, measures);
        LocalDateTime parallelFinish = LocalDateTime.now();
        LOGGER.info("Starting MeLifStar at {}", parallelFinish);
        MeLifStar meLifStar = new MeLifStar(config, 20);
        meLifStar.run(points, measures);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MeLifStar at {}", starFinish);
        long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
        long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(parallelFinish, starFinish);
        LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
        LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
        LOGGER.info("Star work time: {} seconds", starWorkTime);
        LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                getSpeedImprovementPercent(simpleWorkTime, parallelWorkTime));
        LOGGER.info("Multi-threaded to star version speed improvement: {}%",
                getSpeedImprovementPercent(simpleWorkTime, starWorkTime));
        parallelMeLiF.getExecutorService().shutdown();
        meLifStar.getExecutorService().shutdown();
    }
}
