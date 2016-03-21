package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * todo may be faster
 *
 * @author iisaev
 */
public class SymmetricUncertainty extends CorrelationBasedMeasure {
    private static final double LOG_2 = Math.log(2);

    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
        Map<Integer, Distribution> distributions = calculateDistributions(feature, classes);
        double xPriorEntropy = getPriorEntropy(feature.getValues());
        double yPriorEntropy = getPriorEntropy(classes);
        double posteriorEntropy = getPosteriorEntropy(distributions);
        return 2 * (xPriorEntropy - posteriorEntropy) / (xPriorEntropy + yPriorEntropy);
    }

    private double getPosteriorEntropy(Map<Integer, Distribution> distributions) {
        int fullSum = distributions.values().stream().mapToInt(d -> d.sum).sum();
        return -distributions.values().stream().map(d -> {
            double classProb = (double) d.sum / fullSum;
            return classProb * d.distribution.entrySet().stream().map(e -> {
                double p = (double) e.getValue() / d.sum;
                double log2P = Math.log(e.getValue() / d.sum) / LOG_2;
                return p * log2P;
            }).collect(Collectors.summingDouble(doub -> doub));
        }).collect(Collectors.summingDouble(d -> d));
    }

    private double getPriorEntropy(List<Integer> values) {
        Map<Integer, Integer> distribution = values.stream().collect(Collectors.toMap(i -> i, i -> i, (k, v) -> v + 1));
        return -distribution.entrySet().stream().map(e -> {
            double p = (double) e.getValue() / values.size();
            double log2P = Math.log(e.getValue() / values.size()) / LOG_2;
            return p * log2P;
        }).collect(Collectors.summingDouble(d -> d));
    }
}
