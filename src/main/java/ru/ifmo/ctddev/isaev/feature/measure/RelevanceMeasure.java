package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;


/**
 * @author iisaev
 */
public abstract class RelevanceMeasure {
    public abstract double evaluate(Feature feature, List<Integer> classes);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
