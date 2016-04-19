package ru.ifmo.ctddev.isaev.feature.measure;

import ru.ifmo.ctddev.isaev.dataset.Feature;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class SpearmanRankCorrelation implements RelevanceMeasure {
    @Override
    public double evaluate(Feature feature, List<Integer> classes) {
        List<Integer> values = feature.getValues();

        double xMean = values.stream().collect(Collectors.averagingDouble(i -> (double) i));
        double yMean = classes.stream().collect(Collectors.averagingDouble(i -> (double) i));

        double sumDeviationsXY = IntStream.range(0, classes.size()).mapToDouble(i -> {
            double devX = ((double) values.get(i) - xMean);
            double devY = ((double) classes.get(i) - yMean);
            return devX * devY;
        }).sum();

        double squaredDeviationX = IntStream.range(0, values.size()).mapToDouble(i -> {
            double devX = ((double) values.get(i) - xMean);
            return devX * devX;
        }).sum();

        double squaredDeviationY = IntStream.range(0, classes.size()).mapToDouble(i -> {
            double devY = ((double) classes.get(i) - yMean);
            return devY * devY;
        }).sum();

        return sumDeviationsXY / Math.sqrt(squaredDeviationX * squaredDeviationY);
    }
}
