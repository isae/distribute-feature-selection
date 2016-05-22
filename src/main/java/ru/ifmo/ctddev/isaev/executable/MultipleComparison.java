package ru.ifmo.ctddev.isaev.executable;

import ru.ifmo.ctddev.isaev.filter.DatasetFilter;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.ScoreCalculator;
import ru.ifmo.ctddev.isaev.classifier.Classifier;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.BasicMeLiF;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.DatasetSplitter;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm.FORMAT;


/**
 * @author iisaev
 */
public class MultipleComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleComparison.class);

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    protected static double getF1Score(DataSetPair dsPair) {
        Classifier classifier = Classifiers.WEKA_SVM.newClassifier();
        classifier.train(dsPair.getTrainSet());
        List<Integer> actual = classifier.test(dsPair.getTestSet())
                .stream()
                .map(d -> (int) Math.round(d))
                .collect(Collectors.toList());
        List<Integer> expectedValues = dsPair.getTestSet().toInstanceSet().getInstances().stream().map(DataInstance::getClazz).collect(Collectors.toList());
        return scoreCalculator.calculateF1Score(expectedValues, actual);
    }

    private static String htmlRepresentation(List<List<RunStats>> executionResults) {
        StringBuilder sb = new StringBuilder();
        Stream<String> start = Stream.of("Dataset", "Shape", "Features", "Instances");
        Stream<String> header = Stream.concat(start, executionResults.get(0).stream().flatMap(
                stats -> Stream.of(
                        String.format("%s time", stats.getAlgorithmName()),
                        String.format("%s best point", stats.getAlgorithmName()),
                        String.format("%s score", stats.getAlgorithmName()),
                        String.format("%s visited points", stats.getAlgorithmName()))
        ));
        String headerStr = String.join(";", header.collect(Collectors.toList()));
        sb.append(headerStr).append("\n");
        executionResults.stream()
                .forEach(pr -> {
                    Stream<Object> rowStart = Stream.of(
                            pr.get(0).getDataSetName(),
                            String.format("%dx%d", pr.get(0).getFeatureCount(), pr.get(0).getInstanceCount()),
                            pr.get(0).getFeatureCount(),
                            pr.get(0).getInstanceCount());
                    Stream<Object> row = Stream.concat(rowStart, pr.stream().flatMap(
                            stats -> Stream.of(
                                    stats.getWorkTime(),
                                    stats.getBestResult().getPoint(),
                                    FORMAT.format(stats.getBestResult().getF1Score()),
                                    stats.getVisitedPoints())
                    ));
                    sb.append(String.join(";", row.map(Objects::toString).collect(Collectors.toList()))).append("\n");
                });
        return sb.toString();
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
        AlgorithmConfig config = new AlgorithmConfig(0.25, Classifiers.WEKA_SVM, measures);
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
                    config.setDataSetSplitter(new OrderSplitter(testPercent, order));
                    DatasetFilter datasetFilter = new PreferredSizeFilter(100);
                    config.setDataSetFilter(datasetFilter);
                    LocalDateTime startTime = LocalDateTime.now();
                    LOGGER.info("Starting SimpleMeliF at {}", startTime);
                    BasicMeLiF basicMeLiF = new BasicMeLiF(config, dataSet);
                    RunStats simpleStats = basicMeLiF.run(points);
                    LocalDateTime simpleFinish = LocalDateTime.now();
                    LOGGER.info("Starting ParallelMeliF at {}", simpleFinish);

                    ParallelMeLiF parallelMeLiF = new ParallelMeLiF(config, dataSet, executorService);
                    RunStats parallelStats = parallelMeLiF.run(points, false);
                    LocalDateTime parallelFinish = LocalDateTime.now();

                    long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
                    long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
                    LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                            simpleStats.getVisitedPoints(),
                            simpleStats.getBestResult().getPoint(),
                            simpleStats.getBestResult().getF1Score()
                    });
                    LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                            parallelStats.getVisitedPoints(),
                            parallelStats.getBestResult().getPoint(),
                            parallelStats.getBestResult().getF1Score()
                    });
                    LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                            getSpeedImprovementPercent(simpleStats.getWorkTime(), parallelStats.getWorkTime()));
                    MDC.remove("fileName");

                    executionResults.add(new Pr<>(simpleStats, parallelStats));

                    DatasetSplitter tenFoldSplitter = new OrderSplitter(10, order);

                    List<Double> basicScores = tenFoldSplitter.split(
                            datasetFilter.filterDataSet(dataSet.toFeatureSet(), simpleStats.getBestResult().getPoint(), measures)
                    ).stream()
                            .map(MultipleComparison::getF1Score)
                            .collect(Collectors.toList());

                    List<Double> parallelScores = tenFoldSplitter.split(
                            datasetFilter.filterDataSet(dataSet.toFeatureSet(), parallelStats.getBestResult().getPoint(), measures)
                    )
                            .stream().map(MultipleComparison::getF1Score)
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
        writer.println(htmlRepresentation(executionResults.stream().map(pr -> Arrays.asList(pr.getBasic(), pr.getParallel())).collect(Collectors.toList())));
        writer.close();
    }
}
