package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.ParallelMeLiF;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class ClassifiersComparison extends Comparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassifiersComparison.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        Point[] points = new Point[] {
                new Point(1, 0, 0, 0),
                new Point(0, 1, 0, 0),
                new Point(0, 0, 1, 0),
                new Point(0, 0, 0, 1),
                new Point(1, 1, 1, 1)
        };
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        List<RunStats> allStats = IntStream.range(0, Classifiers.values().length).mapToObj(i -> {
            LOGGER.info("Classifier: {}", Classifiers.values()[i]);
            AlgorithmConfig config = new AlgorithmConfig(0.1, 3, 20, Classifiers.values()[i], 100, measures);
            ParallelMeLiF meLiF = new ParallelMeLiF(config, dataSet, 20);
            RunStats result = meLiF.run(points);
            meLiF.getExecutorService().shutdown();
            return result;
        }).collect(Collectors.toList());
        allStats.forEach(stats -> {
            LOGGER.info("Classifier: {}; f1Score: {}; work time: {} seconds", new Object[] {
                    stats.getUsedClassifier(),
                    stats.getBestResult().getF1Score(),
                    stats.getWorkTime()
            });
        });
    }
}
