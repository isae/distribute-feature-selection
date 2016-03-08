package ru.ifmo.ctddev.isaev.feature;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author iisaev
 */
public interface RelevanceMeasure {
    Set<Integer> twoClasses = new HashSet<>(Arrays.asList(0, 1));

    double evaluate(Feature feature, List<Integer> classes);
}
