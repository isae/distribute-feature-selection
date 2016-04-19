package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * @author iisaev
 */
public class ThreadedVsSequentialComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedVsSequentialComparison.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1),
        };
        /*Point[] points = new Point[] {
                new Point(1)
        };*/
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        //RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM()};

        AlgorithmConfig config = new AlgorithmConfig(0.1, 3, 20, Classifiers.WEKA_SVM, 100, measures);
        int threads = 20;
        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.info("Starting SimpleMeliF at {}", startTime);
        RunStats simpleStats = new BasicMeLiF(config, dataSet).run(points);
        LocalDateTime simpleFinish = LocalDateTime.now();
        LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);
        ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, dataSet, threads);
        RunStats parallelStats = parallelMeLiF.run(points);
        LocalDateTime parallelFinish = LocalDateTime.now();
        long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
        long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
        LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                simpleStats.getVisitedPoints(),
                simpleStats.getBestResult().getPoint().getCoordinates(),
                simpleStats.getBestResult().getF1Score()
        });
        LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                parallelStats.getVisitedPoints(),
                parallelStats.getBestResult().getPoint().getCoordinates(),
                parallelStats.getBestResult().getF1Score()
        });
        LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                getSpeedImprovementPercent(simpleStats.getWorkTime(), parallelStats.getWorkTime()));
        parallelMeLiF.getExecutorService().shutdown();
    }
}
