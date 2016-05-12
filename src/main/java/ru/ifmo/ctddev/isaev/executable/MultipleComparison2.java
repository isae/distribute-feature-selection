package ru.ifmo.ctddev.isaev.executable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import filter.DatasetFilter;
import filter.PreferredSizeFilter;
import j2html.tags.Tag;
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
import ru.ifmo.ctddev.isaev.melif.impl.StupidParallelMeLiF;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static j2html.TagCreator.*;


/**
 * @author iisaev
 */
public class MultipleComparison2 extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleComparison2.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    private static String htmlRepresentation(List<RunStats[]> executionResults) {
        Tag[] rows = Stream.concat(
                Stream.of(
                        tr().with(
                                th("Dataset"),
                                th("Shape"),
                                th("Features"),
                                th("Instances"),
                                th("Basic time"),
                                th("Basic best point"),
                                th("Basic score"),
                                th("Basic visited points"),
                                th("Parallel time"),
                                th("Parallel best point"),
                                th("Parallel score"),
                                th("Parallel visited points"),
                                th("Stupid time"),
                                th("Stupid best point"),
                                th("Stupid score"),
                                th("Stupid visited points")
                        )
                ),
                executionResults.stream()
                        .map(pr -> {
                            return tr().with(
                                    td(pr[0].getDataSetName()),
                                    td(String.format("%dx%d", pr[0].getFeatureCount(), pr[0].getInstanceCount())),
                                    td(String.valueOf(pr[0].getFeatureCount())),
                                    td(String.valueOf(pr[0].getInstanceCount())),
                                    td(String.valueOf(pr[0].getWorkTime())),
                                    td(String.valueOf(pr[0].getBestResult().getPoint())),
                                    td(String.valueOf(pr[0].getVisitedPoints())),
                                    td(String.format("%.3f", pr[0].getBestResult().getF1Score())),
                                    td(String.valueOf(pr[1].getWorkTime())),
                                    td(String.valueOf(pr[1].getScore())),
                                    td(String.valueOf(pr[1].getVisitedPoints())),
                                    td(String.format("%.3f", pr[1].getBestResult().getF1Score())),
                                    td(String.valueOf(pr[2].getWorkTime())),
                                    td(String.valueOf(pr[2].getScore())),
                                    td(String.valueOf(pr[2].getVisitedPoints())),
                                    td(String.format("%.3f", pr[2].getBestResult().getF1Score()))
                            );
                        })
        ).collect(Collectors.toList()).toArray(new Tag[0]);
        return html().with(
                head().with(
                        title("Experiment results")
                ),
                body().with(
                        h1("Experiment results"),
                        table().with(rows)
                )
        ).render();
    }

    public static void main(String[] args) throws JsonProcessingException, FileNotFoundException {
        new File("html_results").mkdir();

        String startTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH:mm"));
        MDC.put("fileName", startTimeString + "/COMMON");
        Point[] points = new Point[]{
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
        RelevanceMeasure[] measures = new RelevanceMeasure[]{new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        AlgorithmConfig config = new AlgorithmConfig(0.25, Classifiers.WEKA_SVM, measures);
        //ForkJoinPool executorService = new ForkJoinPool(threadsCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        ExecutorService stupidService = Executors.newFixedThreadPool(5);
        List<RunStats[]> executionResults = new ArrayList<>();
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

                    StupidParallelMeLiF stupidParallelMeLiF = new StupidParallelMeLiF(config, dataSet, stupidService);
                    RunStats stupidStats = stupidParallelMeLiF.run(points, false);
                    LocalDateTime stupidFinish = LocalDateTime.now();

                    long simpleWorkTime = ChronoUnit.SECONDS.between(startTime, simpleFinish);
                    long parallelWorkTime = ChronoUnit.SECONDS.between(simpleFinish, parallelFinish);
                    long stupidWorkTime = ChronoUnit.SECONDS.between(parallelFinish, stupidFinish);
                    LOGGER.info("Single-threaded work time: {} seconds", simpleWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[]{
                            simpleStats.getVisitedPoints(),
                            simpleStats.getBestResult().getPoint().getCoordinates(),
                            simpleStats.getBestResult().getF1Score()
                    });
                    LOGGER.info("Multi-threaded work time: {} seconds", parallelWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[]{
                            parallelStats.getVisitedPoints(),
                            parallelStats.getBestResult().getPoint().getCoordinates(),
                            parallelStats.getBestResult().getF1Score()
                    });
                    LOGGER.info("Stupid Multi-threaded work time: {} seconds", stupidWorkTime);
                    LOGGER.info("Visited {} points; best point is {} with score {}", new Object[]{
                            stupidStats.getVisitedPoints(),
                            stupidStats.getBestResult().getPoint().getCoordinates(),
                            stupidStats.getBestResult().getF1Score()
                    });
                    LOGGER.info("Multi-threaded to single-threaded version speed improvement: {}%",
                            getSpeedImprovementPercent(simpleStats.getWorkTime(), parallelStats.getWorkTime()));
                    MDC.remove("fileName");

                    executionResults.add(new RunStats[]{simpleStats, parallelStats, stupidStats});

                    DatasetSplitter tenFoldSplitter = new OrderSplitter(10, order);

                    List<Double> basicScores = tenFoldSplitter.split(
                            datasetFilter.filterDataSet(dataSet.toFeatureSet(), simpleStats.getBestResult().getPoint(), measures)
                    ).stream()
                            .map(MultipleComparison2::getF1Score)
                            .collect(Collectors.toList());

                    List<Double> parallelScores = tenFoldSplitter.split(
                            datasetFilter.filterDataSet(dataSet.toFeatureSet(), parallelStats.getBestResult().getPoint(), measures)
                    )
                            .stream().map(MultipleComparison2::getF1Score)
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

        PrintWriter writer = new PrintWriter("html_results/" + startTimeString + ".html");
        writer.println(htmlRepresentation(executionResults));
        writer.close();
    }
}
