package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.F1Score;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.classifier.TrainedClassifier;
import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.feature.FitCriterion;
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.feature.SpearmanRankCorrelation;
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty;
import ru.ifmo.ctddev.isaev.feature.measure.VDM;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;
import ru.ifmo.ctddev.isaev.folds.SequentalEvaluator;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
public class MultipleComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleComparison.class);

    private static final F1Score score = new F1Score();

    protected static double getScore(DataSetPair dsPair) {
        Classifier classifier = Classifiers.SVM.newClassifier();
        TrainedClassifier trainedClassifier = classifier.train(dsPair.getTrainSet());
        List<Integer> actual = trainedClassifier.test(dsPair.getTestSet())
                .stream()
                .map(d -> (int) Math.round(d))
                .collect(Collectors.toList());
        List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
        return score.calculate(expectedValues, actual);
    }

    public static void main(String[] args) throws FileNotFoundException {
        new File("table_results").mkdir();

        String startTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH:mm"));
        MDC.put("fileName", startTimeString + "/COMMON");
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1),
        };
        int testPercent = 20;
        int threadsCount;
        int threadsNeeded = points.length * 2 * (100 / testPercent);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Available processors: {}; Threads needed: {}", availableProcessors, threadsNeeded);
        if (threadsNeeded > 5 * availableProcessors) {
            threadsCount = 5 * availableProcessors;
        } else {
            threadsCount = threadsNeeded;
        }
        LOGGER.info("Initialized executor service with {} workers", threadsCount);
        DataSetReader dataSetReader = new DataSetReader();
        File dataSetDir = new File(args[0]);
        assert dataSetDir.exists();
        assert dataSetDir.isDirectory();
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        //ForkJoinPool executorService = new ForkJoinPool(threadsCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Pr<RunStats, RunStats>> executionResults = new ArrayList<>();
        List<Pr<Double, Double>> results = Arrays.asList(dataSetDir.listFiles()).stream()
                .filter(f -> f.getAbsolutePath().endsWith(".csv"))
                .map(file -> {
                    MDC.put("fileName", startTimeString + "/" + file.getName());
                    return file;
                })
                .map(dataSetReader::readCsv)
                .map(dataSet -> {
                    List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
                    Collections.shuffle(order);
                    DataSetFilter dataSetFilter = new PreferredSizeFilter(100);
                    FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                            Classifiers.SVM,
                            dataSetFilter, new OrderSplitter(10, order), score
                    );
                    AlgorithmConfig config = new AlgorithmConfig(0.25, foldsEvaluator, measures);
                    LocalDateTime startTime = LocalDateTime.now();
                    LOGGER.info("Starting SimpleMeliF at {}", startTime);
                    BasicMeLiF basicMeLiF = new BasicMeLiF(config, dataSet);
                    RunStats simpleStats = basicMeLiF.run(points);
                    LocalDateTime simpleFinish = LocalDateTime.now();
                    LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);

                    ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, dataSet, executorService);
                    RunStats parallelStats = parallelMeLiF.run("Parallel", points, false);
                    LocalDateTime parallelFinish = LocalDateTime.now();

                    long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
                    long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
                    LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                            simpleStats.getVisitedPoints(),
                            simpleStats.getBestResult().getPoint(),
                            simpleStats.getBestResult().getScore()
                    });
                    LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                            parallelStats.getVisitedPoints(),
                            parallelStats.getBestResult().getPoint(),
                            parallelStats.getBestResult().getScore()
                    });
                    LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                            getSpeedImprovementPercent(simpleStats.getWorkTime(), parallelStats.getWorkTime()));
                    MDC.remove("fileName");

                    executionResults.add(new Pr<>(simpleStats, parallelStats));

                    DataSetSplitter tenFoldSplitter = new OrderSplitter(10, order);

                    List<Double> basicScores = tenFoldSplitter.split(
                            dataSetFilter.filterDataSet(dataSet.toFeatureSet(), simpleStats.getBestResult().getPoint(), measures)
                    ).stream()
                            .map(MultipleComparison::getScore)
                            .collect(Collectors.toList());

                    List<Double> parallelScores = tenFoldSplitter.split(
                            dataSetFilter.filterDataSet(dataSet.toFeatureSet(), parallelStats.getBestResult().getPoint(), measures)
                    )
                            .stream().map(MultipleComparison::getScore)
                            .collect(Collectors.toList());
                    assert basicScores.size() == parallelScores.size();
                    return new Pr<>(basicScores, parallelScores);
                })
                .flatMap(pr -> IntStream.range(0, pr.getBasic().size()).mapToObj(i -> new Pr<>(pr.getBasic().get(i), pr.getParallel().get(i))))
                .collect(Collectors.toList());
        executorService.shutdown();
        MDC.put("fileName", "COMMON-" + startTimeString);

        List<Double> expected = new ArrayList<>();
        List<Double> actual = new ArrayList<>();
        results.forEach(pr -> {
            expected.add(pr.getBasic());
            actual.add(pr.getParallel());
        });
        LOGGER.info("Expected values over all datasets: {}", expected);
        LOGGER.info("Actual values over all datasets: {}", actual);
        LOGGER.info("Spearman rank correlation: {}", new SpearmanRankCorrelation().evaluate(actual, expected));

        PrintWriter writer = new PrintWriter("table_results/" + startTimeString + ".csv");
        writer.println(fullCsvRepresentation(executionResults.stream().map(pr -> Arrays.asList(pr.getBasic(), pr.getParallel())).collect(Collectors.toList())));
        writer.close();
    }
}
