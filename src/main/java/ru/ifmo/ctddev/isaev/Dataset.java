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
