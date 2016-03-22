package ru.ifmo.ctddev.isaev.result;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.Collections;
import java.util.List;


/**
 * @author iisaev
 */
public class SelectionResult implements Comparable<SelectionResult> {
    private final List<Feature> selectedFeatures;

    private final Point point;

    private final double f1Score;

    public SelectionResult(List<Feature> selectedFeatures, Point point, double f1Score) {
        this.point = point;
        this.selectedFeatures = Collections.unmodifiableList(selectedFeatures);//todo constructor is redundant?
        this.f1Score = f1Score;
    }

    public List<Feature> getSelectedFeatures() {
        return selectedFeatures;
    }

    public double getF1Score() {
        return f1Score;
    }

    @Override
    public int compareTo(SelectionResult o) {
        return Double.compare(f1Score, o.f1Score);
    }

    public Point getPoint() {
        return point;
    }

    public boolean betterThan(SelectionResult bestScore) {
        return this.compareTo(bestScore) == 1;
    }
}
