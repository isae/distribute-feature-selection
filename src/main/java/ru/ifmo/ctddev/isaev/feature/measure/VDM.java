package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class VDM extends CorrelationBasedMeasure {
    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
        Map<Integer, CorrelationBasedMeasure.Distribution> distributions = calculateDistributions(feature, classes);
        Set<Integer> distinctValues = feature.getValues().stream().collect(Collectors.toSet());
        final double[] result = {0};
        distinctValues.forEach(dv -> {
            Distribution db0 = distributions.get(0);
            Distribution db1 = distributions.get(1);
            result[0] += ((double) db0.distribution.get(dv) / db0.sum - (double) db1.distribution.get(dv) / db1.sum);
        });
        return result[0] / 2;
    }


}
