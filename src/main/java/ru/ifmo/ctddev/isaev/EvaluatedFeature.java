package ru.ifmo.ctddev.isaev;

/**
 * @author iisaev
 */
public class EvaluatedFeature {
    private Feature feature;

    private double measure;

    public EvaluatedFeature(Feature feature, double measure) {
        this.feature = feature;
        this.measure = measure;
    }

    public Feature getFeature() {
        return feature;
    }

    public double getMeasure() {
        return measure;
    }
}
