package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public abstract class CorrelationBasedMeasure implements RelevanceMeasure {
    protected class Distribution {
        private int sum;

        private Map<Integer, Integer> distribution = new HashMap<>(); //value -> number of instances having it

        public int getSum() {
            return sum;
        }

        public void setSum(int sum) {
            this.sum = sum;
        }

        public Map<Integer, Integer> getDistribution() {
            return distribution;
        }

        public Distribution() {
        }

        public void setDistribution(Map<Integer, Integer> distribution) {
            this.distribution = distribution;
        }

        public Distribution(int sum, Map<Integer, Integer> distribution) {
            this.sum = sum;
            this.distribution = distribution;
        }
    }

    @Override
    public abstract double evaluate(Feature feature, List<Integer> classes);

    protected Map<Integer, Distribution> calculateDistribution(List<Integer> values, List<Integer> classes) {
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

    protected Distribution calculateDistribution(List<Integer> values) {
        return new Distribution(values.size(), values.stream().collect(Collectors.toMap(i -> i, i -> 1, (k, v) -> k + v)));
    }
}
