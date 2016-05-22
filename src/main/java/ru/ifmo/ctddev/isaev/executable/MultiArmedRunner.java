package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import ru.ifmo.ctddev.isaev.melif.impl.MultiArmedBanditMeLiF;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.splitter.OrderSplitter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class MultiArmedRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedRunner.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        AlgorithmConfig config = new AlgorithmConfig(0.3, Classifiers.WEKA_SVM, measures);
        config.setDataSetFilter(new PreferredSizeFilter(100));
        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
        Collections.shuffle(order);
        config.setDataSetSplitter(new OrderSplitter(20, order));
        LocalDateTime startTime = LocalDateTime.now();
        MultiArmedBanditMeLiF meLif = new MultiArmedBanditMeLiF(config, dataSet, 20, 2);
        RunStats runStats = meLif.run();
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MultiArmedBanditMeLiF at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("MultiArmedBanditMeLiF work time: {} seconds", starWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                runStats.getVisitedPoints(),
                runStats.getBestResult().getPoint(),
                runStats.getBestResult().getF1Score()
        });
    }
}
