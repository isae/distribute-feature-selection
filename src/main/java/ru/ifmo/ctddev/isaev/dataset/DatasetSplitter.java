package ru.ifmo.ctddev.isaev.dataset;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class DatasetSplitter {
    private final Random random = new Random();

    public List<DatasetPair> splitRandomly(DataSet original, int testPercent, int times) {
        return IntStream.range(0, times).mapToObj(i -> splitRandomly(original, testPercent)).collect(Collectors.toList());
    }

    public List<DatasetPair> splitSequentially(DataSet original, int testPercent, int times) {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    public DatasetPair splitRandomly(DataSet original, int testPercent) {
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
        return new DatasetPair(new InstanceDataSet(trainInstances), new InstanceDataSet(testInstances));
    }
}
