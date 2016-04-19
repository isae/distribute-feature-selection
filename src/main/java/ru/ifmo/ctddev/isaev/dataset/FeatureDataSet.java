package ru.ifmo.ctddev.isaev.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class FeatureDataSet extends DataSet {
    private final List<Feature> features;

    private final List<Integer> classes;

    public FeatureDataSet(List<Feature> features, List<Integer> classes, String name) {
        super(name);
        if (!classes.stream().allMatch(i -> i == 0 || i == 1)) {
            throw new IllegalArgumentException("All classes values should be 0 or 1");
        }
        features.forEach(f -> {
            if (f.getValues().size() != classes.size()) {
                throw new IllegalArgumentException(String.format("Feature %s has wrong number of values", f.getName()));
            }
        });
        this.features = Collections.unmodifiableList(features);
        this.classes = Collections.unmodifiableList(classes);
    }

    public List<Integer> getClasses() {
        return classes;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    @Override
    public FeatureDataSet toFeatureSet() {
        return this;
    }

    @Override
    public InstanceDataSet toInstanceSet() {
        final int[] counter = {0};
        List<DataInstance> instances = IntStream.range(0, classes.size()).mapToObj(i -> {
            List<Integer> values = new ArrayList<>();
            features.forEach(feature -> {
                values.add(feature.getValues().get(i));
            });
            return new DataInstance("instance" + (++counter[0]), classes.get(i), values);
        }).collect(Collectors.toList());
        return new InstanceDataSet(instances);
    }

    @Override
    public int getFeatureCount() {
        return features.size();
    }

    @Override
    public int getInstanceCount() {
        return classes.size();
    }
}
