package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public abstract class CorrelationBasedMeasure implements RelevanceMeasure {
    protected class Distribution {
        int sum;

        Map<Integer, Integer> distribution = new HashMap<>(); //value -> number of instances having it
    }

    @Override
    public abstract double evaluate(Feature feature, List<Integer> classes);

    protected Map<Integer, Distribution> calculateDistributions(Feature feature, List<Integer> classes) {
        List<Integer> values = feature.getValues();
        Set<Integer> distinctValues = values.stream().collect(Collectors.toSet());
        Map<Integer, Distribution> distributions = new HashMap<>();
        twoClasses.forEach(clazz -> {
            Distribution ds = new Distribution();
            distinctValues.forEach(v -> {
                ds.distribution.put(v, 0);
            });
            distributions.put(clazz, ds);
        });
        for (int i = 0; i < classes.size(); ++i) {
            ++distributions.get(classes.get(i)).sum;
            Integer prevValue = distributions.get(classes.get(i)).distribution.get(values.get(i));
            distributions.get(classes.get(i)).distribution.put(values.get(i), prevValue + 1);
        }
        return distributions;
    }
}
