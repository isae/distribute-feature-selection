package ru.ifmo.ctddev.isaev.executable;

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
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.filter.DataSetFilter;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator;
import ru.ifmo.ctddev.isaev.folds.ParallelEvaluator;
import ru.ifmo.ctddev.isaev.folds.SequentalEvaluator;
import ru.ifmo.ctddev.isaev.melif.impl.*;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class GiantComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleComparison.class);

    private static final ScoreCalculator scoreCalculator = new ScoreCalculator();

    private static final RelevanceMeasure[] MEASURES = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};


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
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        DataSetReader dataSetReader = new DataSetReader();
        File dataSetDir = new File(args[0]);
        assert dataSetDir.exists();
        assert dataSetDir.isDirectory();
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
                    DataSetFilter dataSetFilter = new PreferredSizeFilter(100);
                    DataSetSplitter dataSetSplitter = new OrderSplitter(10, order);
                    List<RunStats> allStats = new ArrayList<>();
                    for (Integer threads : Arrays.asList(availableProcessors, (int) (1.5 * availableProcessors))) {
                        for (Integer testPercent : Arrays.asList(10, 20)) {
                            for (Double delta : Arrays.asList(0.1, 0.25)) {
                                for (FoldsEvaluator foldsEvaluator : Arrays.asList(
                                        new SequentalEvaluator(
                                                Classifiers.WEKA_SVM,
                                                dataSetFilter, dataSetSplitter
                                        ),
                                        new ParallelEvaluator(
                                                Classifiers.WEKA_SVM,
                                                dataSetFilter, dataSetSplitter, 100 / testPercent
                                        )
                                )) {
                                    allStats.add(getBasicStats(delta, foldsEvaluator, points, dataSet));
                                    allStats.addAll(getStupidParallelStats(delta, foldsEvaluator, points, dataSet));
                                    allStats.addAll(getPriorityStats(threads, delta, foldsEvaluator, dataSet));
                                    allStats.addAll(getMultiArmedStats(threads, delta, foldsEvaluator, dataSet));
                                }
                            }
                        }
                    }
                    MDC.remove("fileName");
                    return allStats;

                })
                .collect(Collectors.toList());
        MDC.put("fileName", "COMMON-" + startTimeString);

        PrintWriter writer = new PrintWriter("table_results/" + startTimeString + ".csv");
        writer.println(csvRepresentation(results));
        writer.close();
    }

    private static Collection<RunStats> getMultiArmedStats(Integer threads, Double delta, FoldsEvaluator foldsEvaluator, FeatureDataSet dataSet) {
        AlgorithmConfig config = new AlgorithmConfig(delta, foldsEvaluator, MEASURES);
        List<RunStats> allStats = new ArrayList<>();
        for (Integer splitNumber : Arrays.asList(2, 3)) {
            MultiArmedBanditMeLiF meLiF1 = new MultiArmedBanditMeLiF(config, dataSet, threads, splitNumber);
            RunStats f10sqrtStats = meLiF1.run(
                    String.format("MultiArmedD%sE%sT%sF10sqrt",
                            FeatureSelectionAlgorithm.FORMAT.format(delta),
                            foldsEvaluator.getName(),
                            foldsEvaluator.getDataSetSplitter().getTestPercent())
                    , meLiF1.getPointQueuesNumber() + (int) (10 * Math.sqrt(threads)));
            MultiArmedBanditMeLiF meLiF2 = new MultiArmedBanditMeLiF(config, dataSet, threads, splitNumber);
            RunStats f20sqrtStats = meLiF2.run(
                    String.format("MultiArmedD%sE%sT%sF20sqrt",
                            FeatureSelectionAlgorithm.FORMAT.format(delta),
                            foldsEvaluator.getName(),
                            foldsEvaluator.getDataSetSplitter().getTestPercent())
                    , meLiF2.getPointQueuesNumber() + (int) (20 * Math.sqrt(threads)));
            MultiArmedBanditMeLiF meLiF3 = new MultiArmedBanditMeLiF(config, dataSet, threads, splitNumber);
            RunStats f10linStats = meLiF3.run(
                    String.format("MultiArmedD%sE%sT%sF10lin",
                            FeatureSelectionAlgorithm.FORMAT.format(delta),
                            foldsEvaluator.getName(),
                            foldsEvaluator.getDataSetSplitter().getTestPercent())
                    , meLiF3.getPointQueuesNumber() + 10 * threads);
            allStats.add(f10sqrtStats);
            allStats.add(f20sqrtStats);
            allStats.add(f10linStats);
        }
        return allStats;
    }

    private static Collection<RunStats> getPriorityStats(Integer threads, Double delta, FoldsEvaluator foldsEvaluator, FeatureDataSet dataSet) {
        AlgorithmConfig config = new AlgorithmConfig(delta, foldsEvaluator, MEASURES);
        RunStats f10Stats = new PriorityQueueMeLiF(config, dataSet, threads).run(
                String.format("PrQueueD%sE%sT%sF10",
                        FeatureSelectionAlgorithm.FORMAT.format(delta),
                        foldsEvaluator.getName(),
                        foldsEvaluator.getDataSetSplitter().getTestPercent())
                , 22 + 10 * threads);
        RunStats f5Stats = new PriorityQueueMeLiF(config, dataSet, threads).run(
                String.format("PrQueueD%sE%sT%sF5",
                        FeatureSelectionAlgorithm.FORMAT.format(delta),
                        foldsEvaluator.getName(),
                        foldsEvaluator.getDataSetSplitter().getTestPercent())
                , 22 + 5 * threads);
        return Arrays.asList(f10Stats, f5Stats);
    }

    private static Collection<? extends RunStats> getStupidParallelStats(Double delta, FoldsEvaluator foldsEvaluator, Point[] points, FeatureDataSet dataSet) {
        AlgorithmConfig config = new AlgorithmConfig(delta, foldsEvaluator, MEASURES);
        RunStats stupidParallel = new StupidParallelMeLiF(config, dataSet).run(
                String.format("StupidD%sE%sT%s",
                        FeatureSelectionAlgorithm.FORMAT.format(delta),
                        foldsEvaluator.getName(),
                        foldsEvaluator.getDataSetSplitter().getTestPercent())
                , points);
        RunStats stupidParallel2 = new StupidParallelMeLiF2(config, dataSet).run(
                String.format("Stupid2D%sE%sT%s",
                        FeatureSelectionAlgorithm.FORMAT.format(delta),
                        foldsEvaluator.getName(),
                        foldsEvaluator.getDataSetSplitter().getTestPercent())
                , points);
        return Arrays.asList(stupidParallel, stupidParallel2);
    }

    private static RunStats getBasicStats(Double delta, FoldsEvaluator foldsEvaluator, Point[] points, FeatureDataSet dataSet) {

        AlgorithmConfig config = new AlgorithmConfig(delta, foldsEvaluator, MEASURES);
        return new BasicMeLiF(config, dataSet).run(
                String.format("BasicD%sE%sT%s",
                        FeatureSelectionAlgorithm.FORMAT.format(delta),
                        foldsEvaluator.getName(),
                        foldsEvaluator.getDataSetSplitter().getTestPercent())
                , points);
    }
}