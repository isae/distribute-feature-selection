package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author iisaev
 */
public class VDM extends CorrelationBasedMeasure {
    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
        Map<Integer, CorrelationBasedMeasure.Distribution> distributions = calculateDistribution(feature.getValues(), classes);
        Set<Integer> distinctValues = new HashSet<>(feature.getValues());
        final double[] result = {0};
        Distribution db0 = distributions.get(0);
        Distribution db1 = distributions.get(1);
        distinctValues.forEach(dv -> {
            Integer from0 = db0.getDistribution().get(dv);
            if (from0 == null) {
                from0 = 0;
            }
            Integer from1 = db1.getDistribution().get(dv);
            if (from1 == null) {
                from1 = 0;
            }
            double toAdd = Math.abs(((double) from0 / db0.getSum() - (double) from1 / db1.getSum()));
            result[0] += toAdd;
        });
        return result[0] / 2;
    }


}
