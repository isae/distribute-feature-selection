package ru.ifmo.ctddev.isaev;

import java.util.Collections;
import java.util.List;


/**
 * @author iisaev
 */
public class Dataset {
    private List<List<Double>> features;

    private List<Integer> classes;

    public Dataset(List<List<Double>> features, List<Integer> classes) {
        if (!classes.stream().allMatch(i -> i == 0 || i == 1)) {
            throw new IllegalArgumentException("All classes values should be 0 or 1");
        }
        this.features = Collections.unmodifiableList(features);
        this.classes = Collections.unmodifiableList(classes);
    }

    public List<Integer> getClasses() {
        return classes;
    }

    public List<List<Double>> getFeatures() {
        return features;
    }
}
