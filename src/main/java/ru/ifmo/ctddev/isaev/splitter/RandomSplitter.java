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
public class RandomSplitter {
    private final Random random = new Random();

    public RandomSplitter(int testPercent, int times) {
        this.testPercent = testPercent;
        this.times = times;
    }

    private final int testPercent;

    private final int times;

    public List<DataSetPair> split(DataSet original) {
        return IntStream.range(0, times).mapToObj(i -> splitRandomly(original)).collect(Collectors.toList());
    }

    private DataSetPair splitRandomly(DataSet original) {
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
