package ru.ifmo.ctddev.isaev.dataset;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class DatasetSplitter {
    private final Random random = new Random();

    public List<DataSetPair> splitRandomly(DataSet original, int testPercent, int times) {
        return IntStream.range(0, times).mapToObj(i -> splitRandomly(original, testPercent)).collect(Collectors.toList());
    }

    public List<DataSetPair> splitSequentially(DataSet original, int testPercent) {
        List<DataSetPair> result = new ArrayList<>();
        int folds = (int) ((double) 100 / testPercent);
        List<DataInstance> instances = new ArrayList<>(original.toInstanceSet().getInstances());
        int testSize = (int) ((double) instances.size() * testPercent / 100);
        Collections.shuffle(instances);
        int startPosition = 0;
        while (startPosition < instances.size()) {
            int endPosition = startPosition + testSize;
            List<DataInstance> beforeCV = instances.subList(0, startPosition);
            List<DataInstance> cv = instances.subList(startPosition, endPosition);
            List<DataInstance> afterCV = instances.subList(endPosition, instances.size());
            beforeCV.addAll(afterCV);
            result.add(new DataSetPair(
                    new InstanceDataSet(new ArrayList<>(beforeCV)),
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
