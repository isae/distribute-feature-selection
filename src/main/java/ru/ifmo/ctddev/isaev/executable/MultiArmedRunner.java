package ru.ifmo.ctddev.isaev.executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty;
import ru.ifmo.ctddev.isaev.feature.measure.VDM;
import ru.ifmo.ctddev.isaev.melif.impl.MultiArmedBanditMeLiF;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ktddev.isaev.*;

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
        int threads = 2;
        int splitNumber = 2;
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        RelevanceMeasure[] measures = new RelevanceMeasure[] {new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        List<Integer> order = IntStream.range(0, dataSet.getInstanceCount()).mapToObj(i -> i).collect(Collectors.toList());
        Collections.shuffle(order);
        FoldsEvaluator foldsEvaluator = new SequentalEvaluator(
                Classifiers.SVM,
                new PreferredSizeFilter(100), new OrderSplitter(10, order), new F1Score()
        );
        AlgorithmConfig config = new AlgorithmConfig(0.1, foldsEvaluator, measures);
        LocalDateTime startTime = LocalDateTime.now();
        MultiArmedBanditMeLiF meLif = new MultiArmedBanditMeLiF(config, dataSet, threads, splitNumber);
        RunStats runStats = meLif.run("MultiArmedMeLiF", meLif.getPointQueuesNumber() + (int) (12.5 * Math.sqrt(threads)));
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished MultiArmedBanditMeLiF at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("MultiArmedBanditMeLiF work time: {} seconds", starWorkTime);
        LOGGER.info("Visited {} points; best point is {} with score {}", new Object[] {
                runStats.getVisitedPoints(),
                runStats.getBestResult().getPoint(),
                runStats.getBestResult().getScore()
        });
    }
}
