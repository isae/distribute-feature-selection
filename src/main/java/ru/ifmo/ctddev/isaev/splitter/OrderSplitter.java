package ru.ifmo.ctddev.isaev.splitter;

import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.DataSet;
import ru.ifmo.ctddev.isaev.dataset.DataSetPair;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class OrderSplitter extends SequentalSplitter {
    private final Random random = new Random();

    public OrderSplitter(int testPercent, List<Integer> order) {
        super(testPercent);
        this.order = order;
        if (logger.isTraceEnabled()) {
            logger.trace("Initialized dataset splitter with order {}", Arrays.toString(order.toArray()));
        }
    }

    private final List<Integer> order;

    public List<DataSetPair> splitRandomly(DataSet original, int testPercent, int times) {
        return IntStream.range(0, times).mapToObj(i -> splitRandomly(original, testPercent)).collect(Collectors.toList());
    }

    public List<DataSetPair> split(DataSet original) {
        List<DataSetPair> result = new ArrayList<>();
        int folds = (int) ((double) 100 / testPercent);
        List<DataInstance> instancesBeforeShuffle = new ArrayList<>(original.toInstanceSet().getInstances());
        List<DataInstance> instances = new ArrayList<>();
        if (order != null) {
            final List<DataInstance> finalInstances = instances;
            order.forEach(i -> finalInstances.add(instancesBeforeShuffle.get(i)));
        } else {
            instances = instancesBeforeShuffle;
        }
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

    public DataSetPair splitRandomly(DataSet original, int testPercent) {
        int testInstanceNumber = (int) ((double) original.getInstanceCount() * testPercent) / 100;
        Set<Integer> selectedInstances = new HashSet<>();
        while (selectedInstances.size() != testInstanceNumber) {
            selectedInstances.add(random.nextInt(original.getInstanceCount()));
        }
        InstanceDataSet instanceSet = original.toInstanceSet();
        List<DataInstance> trainInstances = new ArrayList<>(original.getInstanceCount() - testInstanceNumber);
        List<DataInstance> testInstances = new ArrayList<>(testInstanceNumber);
        IntStream.range(0, original.getInstanceCount()).forEach(i -> {
            if (selectedInstances.contains(i)) {
                testInstances.add(instanceSet.getInstances().get(i));
            } else {
                trainInstances.add(instanceSet.getInstances().get(i));
            }
        });
        return new DataSetPair(new InstanceDataSet(trainInstances), new InstanceDataSet(testInstances));
    }
}
