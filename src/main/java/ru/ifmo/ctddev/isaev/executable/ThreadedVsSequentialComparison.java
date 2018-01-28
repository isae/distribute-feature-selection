package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.*;
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty;
import ru.ifmo.ctddev.isaev.feature.measure.VDM;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.point.Point;
import ru.ifmo.ctddev.isaev.point.RunStats;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class ThreadedVsSequentialComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedVsSequentialComparison.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        Point[] points = new Point[] {
                new Point(1.0, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1.0, 1, 1, 1),
        };
        /*Point[] points = new Point[] {
                new Point(1)
        };*/
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        //ru.ifmo.ctddev.isaev.RelevanceMeasure[] measures = new ru.ifmo.ctddev.isaev.RelevanceMeasure[] {new VDM()};

        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
        Collections.shuffle(order);
        FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                Classifiers.SVM,
                new PreferredSizeFilter(100), new OrderSplitter(10, order), new F1Score()
        );
        AlgorithmConfig config = new AlgorithmConfig(0.25, foldsEvaluator, measures);
        int threads = 20;
        LocalDateTime startTime = LocalDateTime.now();
        LOGGER.info("Starting SimpleMeliF at {}", startTime);
        BasicMeLiF basicMeLiF = new BasicMeLiF(config, dataSet);
        RunStats simpleStats = basicMeLiF.run(points);
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
                simpleStats.getBestResult().getScore()
        });
        LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                parallelStats.getVisitedPoints(),
                parallelStats.getBestResult().getPoint().getCoordinates(),
                parallelStats.getBestResult().getScore()
        });
        LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                getSpeedImprovementPercent(simpleStats.getWorkTime(), parallelStats.getWorkTime()));
        parallelMeLiF.getExecutorService().shutdown();
    }
}
