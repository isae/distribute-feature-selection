package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.*;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.MeLifStar;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.SimpleMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * @author iisaev
 */
public class MeLiFComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeLiFComparison.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        AlgorithmConfig config = new AlgorithmConfig(0.3, 10, 20, dataSet, 100);
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1)
        };
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        //int threads = Runtime.getRuntime().availableProcessors();
        int threads = 20;
        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.info("Starting SimpleMeliF at {}", startTime);
        RunStats simpleStats = new SimpleMeLiF(config).run(points, measures);
        LocalDateTime simpleFinish = LocalDateTime.now();
        LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);
        ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, threads);
        RunStats parallelStats = parallelMeLiF.run(points, measures);
        LocalDateTime parallelFinish = LocalDateTime.now();
        LOGGER.info("Starting MeLifStar at {}", parallelFinish);
        MeLifStar meLifStar = new MeLifStar(config, threads);
        RunStats starStats = meLifStar.run(points, measures);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MeLifStar at {}", starFinish);
        long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
        long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(parallelFinish, starFinish);
        LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
        LOGGER.info("Visited {} points; best point is {}", simpleStats.getVisitedPoints(),
                simpleStats.getBestResult().getPoint().getCoordinates());
        LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
        LOGGER.info("Visited {} points; best point is {}", parallelStats.getVisitedPoints(),
                parallelStats.getBestResult().getPoint().getCoordinates());
        LOGGER.info("Star work time: {} seconds", starWorkTime);
        LOGGER.info("Visited {} points; best point is {}", starStats.getVisitedPoints(),
                starStats.getBestResult().getPoint().getCoordinates());
        LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                getSpeedImprovementPercent(simpleWorkTime, parallelWorkTime));
        LOGGER.info("Star to single-threaded version speed improvement: {}%",
                getSpeedImprovementPercent(simpleWorkTime, starWorkTime));
        parallelMeLiF.getExecutorService().shutdown();
        meLifStar.getExecutorService().shutdown();
    }
}
