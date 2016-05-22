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
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm.FORMAT;


/**
 * @author iisaev
 */
public class MultipleComparison2 extends Comparison {
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

    private static String csvRepresentation(List<List<RunStats>> executionResults) {
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
        List<List<RunStats>> results = Arrays.asList(dataSetDir.listFiles()).stream()
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

                    RunStats basicStats = new BasicMeLiF(config, dataSet).run(points);
                    RunStats parallelStats = new ParallelMeLiF(config, dataSet, threadsCount).run(points);
                    RunStats priorityStats = new PriorityQueueMeLiF(config, dataSet, threadsCount).run();
                    MDC.remove("fileName");
                    return Arrays.asList(basicStats, parallelStats, priorityStats);
                })
                .collect(Collectors.toList());
        MDC.put("fileName", "COMMON-" + startTimeString);

        PrintWriter writer = new PrintWriter("table_results/" + startTimeString + ".csv");
        writer.println(csvRepresentation(results));
        writer.close();
    }
}
