package ru.ifmo.ctddev.isaev.executable;

import filter.PreferredSizeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class MultipleThreadedVsSequentialComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleThreadedVsSequentialComparison.class);

    public static void main(String[] args) {
        int threadsCount = Math.max(20, Runtime.getRuntime().availableProcessors());
        LOGGER.info("Available processors: {}; Threads count: {}", Runtime.getRuntime().availableProcessors(), threadsCount);
        DataSetReader dataSetReader = new DataSetReader();
        File dataSetDir = new File(args[0]);
        assert dataSetDir.exists();
        assert dataSetDir.isDirectory();
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1),
        };
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        AlgorithmConfig config = new AlgorithmConfig(0.1, Classifiers.WEKA_SVM, measures);
        String startTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH:mm"));
        Arrays.asList(dataSetDir.listFiles()).stream()
                .filter(f -> f.getAbsolutePath().endsWith(".csv"))
                .map(file -> {
                    MDC.put("fileName", file.getName() + "-" + startTimeString);
                    return file;
                })
                .map(dataSetReader::readCsv)
                .forEach(dataSet -> {
                    ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
                    List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
                    Collections.shuffle(order);
                    config.setDataSetSplitter(new OrderSplitter(20, order));
                    config.setDataSetFilter(new PreferredSizeFilter(100));
                    LocalDateTime startTime = LocalDateTime.now();
                    LOGGER.info("Starting SimpleMeliF at {}", startTime);
                    BasicMeLiF basicMeLiF = new BasicMeLiF(config, dataSet);
                    RunStats simpleStats = basicMeLiF.run(points);
                    LocalDateTime simpleFinish = LocalDateTime.now();
                    LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);

                    ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, dataSet, executorService);
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
                    executorService.shutdown();
                    MDC.remove("fileName");
                });

    }
}
