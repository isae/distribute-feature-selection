package ru.ifmo.ctddev.isaev.feature.measure;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.ctddev.isaev.RelevanceMeasure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public abstract class CorrelationBasedMeasure extends RelevanceMeasure {
    CorrelationBasedMeasure(@NotNull Double minValue, @NotNull Double maxValue) {
        super(minValue, maxValue);
    }

    protected class Distribution {
        private int sum;

        private Map<Integer, Integer> distribution = new HashMap<>(); //value -> number of instances having it

        int getSum() {
            return sum;
        }

        void setSum(int sum) {
            this.sum = sum;
        }

        public Map<Integer, Integer> getDistribution() {
            return distribution;
        }

        public Distribution() {
        }

        public Distribution(int sum, Map<Integer, Integer> distribution) {
            this.sum = sum;
            this.distribution = distribution;
        }
    }

    Map<Integer, Distribution> calculateDistribution(List<Integer> values, List<Integer> classes) {
        Map<Integer, Distribution> distributions = new HashMap<>();
        classes.stream().distinct().forEach(clazz -> {
            Distribution ds = new Distribution();
            distributions.put(clazz, ds);
        });
        for (int i = 0; i < classes.size(); ++i) {
            distributions.get(classes.get(i)).setSum(distributions.get(classes.get(i)).getSum() + 1);
            Integer prevValue = distributions.get(classes.get(i)).getDistribution().get(values.get(i));
            if (prevValue == null) {
                prevValue = 0;
            }
            distributions.get(classes.get(i)).getDistribution().put(values.get(i), prevValue + 1);
        }
        return distributions;
    }

    Distribution calculateDistribution(List<Integer> values) {
        return new Distribution(values.size(), values.stream().collect(Collectors.toMap(i -> i, i -> 1, (k, v) -> k + v)));
    }
}
