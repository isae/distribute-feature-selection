package ru.ifmo.ctddev.isaev.executable;

import ru.ifmo.ctddev.isaev.filter.PreferredSizeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.AlgorithmConfig;
import ru.ifmo.ctddev.isaev.DataSetReader;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.melif.impl.MultiArmedBanditMeLiF;
import ru.ifmo.ctddev.isaev.splitter.SequentalSplitter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * @author iisaev
 */
public class MultiArmedRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiArmedRunner.class);

    public static void main(String[] args) {
        DataSetReader dataSetReader = new DataSetReader();
        DataSet dataSet = dataSetReader.readCsv(args[0]);
        RelevanceMeasure[] measures = new RelevanceMeasure[]{new VDM(), new FitCriterion(), new SymmetricUncertainty(), new SpearmanRankCorrelation()};
        AlgorithmConfig config = new AlgorithmConfig(0.3, Classifiers.WEKA_SVM, measures);
        config.setDataSetFilter(new PreferredSizeFilter(100));
        config.setDataSetSplitter(new SequentalSplitter(20));
        LocalDateTime startTime = LocalDateTime.now();
        MultiArmedBanditMeLiF meLif = new MultiArmedBanditMeLiF(config, dataSet, 20, 3);
        meLif.run(true);
        LocalDateTime starFinish = LocalDateTime.now();
        LOGGER.info("Finished BasicMeLiF at {}", starFinish);
        long starWorkTime = ChronoUnit.SECONDS.between(startTime, starFinish);
        LOGGER.info("Star work time: {} seconds", starWorkTime);
    }
}
