package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class FitCriterion implements RelevanceMeasure {
    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
        List<Integer> values = feature.getValues();
        Double mean0 = calculateMean(0, values, classes);
        Double mean1 = calculateMean(1, values, classes);
        Double var0 = calculateVariance(0, mean0, values, classes);
        Double var1 = calculateVariance(1, mean1, values, classes);

        long fcpSum = IntStream.range(0, classes.size())
                .filter(i -> calculateFCP(values.get(i), mean0, mean1, var0, var1)
                        .equals(classes.get(i))).count();
        return (double) fcpSum / classes.size();
    }

    private Double calculateVariance(int clazz, Double mean0, List<Integer> values, List<Integer> classes) {
        return IntStream.range(0, classes.size())
                .filter(i -> classes.get(i) == clazz)
                .mapToDouble((index) -> Math.pow(values.get(index) - mean0, 2))
                .average().getAsDouble();
    }

    private Double calculateMean(int clazz, List<Integer> values, List<Integer> classes) {
        return IntStream.range(0, classes.size())
                .filter(i -> classes.get(i) == clazz)
                .map(values::get)
                .average().getAsDouble();
    }

    private Integer calculateFCP(Integer value, Double mean0, Double mean1, Double var0, Double var1) {
        Double val0 = Math.abs(value - mean0) / var0;
        Double val1 = Math.abs(value - mean1) / var1;
        return val0 < val1 ? 0 : 1;
    }
}
