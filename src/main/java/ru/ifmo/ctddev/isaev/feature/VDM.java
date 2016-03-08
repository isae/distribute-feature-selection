package ru.ifmo.ctddev.isaev.feature;

import ru.ifmo.ctddev.isaev.Feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class VDM implements RelevanceMeasure {
    private class Distribution {
        int sum;

        Map<Integer, Integer> distribution; //value -> number of instances having it
    }

    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
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
        calculateDistribution(distributions, classes, values);

        final double[] result = {0};
        distinctValues.forEach(dv -> {
            Distribution db0 = distributions.get(0);
            Distribution db1 = distributions.get(0);
            result[0] += (db0.distribution.get(dv) / db0.sum - db1.distribution.get(dv) / db1.sum);
        });

        return result[0] / 2;
    }

    private void calculateDistribution(Map<Integer, Distribution> distributions, List<Integer> classes, List<Integer> values) {
        for (int i = 0; i < classes.size(); ++i) {
            ++distributions.get(classes.get(i)).sum;
            Integer prevValue = distributions.get(classes.get(i)).distribution.get(values.get(i));
            distributions.get(classes.get(i)).distribution.put(values.get(i), prevValue + 1);
        }
    }
}
