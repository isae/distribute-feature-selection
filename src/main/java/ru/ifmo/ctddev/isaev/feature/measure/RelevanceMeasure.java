package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;


/**
 * @author iisaev
 */
public interface RelevanceMeasure {
    double evaluate(Feature feature, List<Integer> classes);
}
