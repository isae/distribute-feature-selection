package ru.ifmo.ctddev.isaev.splitter;

import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Use {@link OrderSplitter} instead
 *
 * @author iisaev
 */
@Deprecated
public class SequentalSplitter extends AbstractDatasetSplitter {
    private final Random random = new Random();

    protected final int testPercent;

    public SequentalSplitter(int testPercent) {
        this.testPercent = testPercent;
        logger.info("Initialized dataset splitter with test percent {}", testPercent);
    }

    public List<DataSetPair> split(DataSet original) {
        List<DataSetPair> result = new ArrayList<>();
        int folds = (int) ((double) 100 / testPercent);
        List<DataInstance> instances = new ArrayList<>(original.toInstanceSet().getInstances());
        int testSize = (int) ((double) instances.size() * testPercent / 100);
        int startPosition = 0;
        while (startPosition < instances.size()) {
            int endPosition = Math.min(startPosition + testSize, instances.size());
            List<DataInstance> beforeCV = new ArrayList<>(instances.subList(0, startPosition));
            List<DataInstance> cv = instances.subList(startPosition, endPosition);
            List<DataInstance> afterCV = instances.subList(endPosition, instances.size());
            beforeCV.addAll(afterCV);
            result.add(new DataSetPair(
                    new InstanceDataSet(beforeCV),
                    new InstanceDataSet(new ArrayList<>(cv))
            ));
            startPosition = endPosition;
        }
        assert result.size() == folds;
        return result;
    }
}
