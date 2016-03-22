package ru.ifmo.ctddev.isaev.feature;

import ru.ifmo.ctddev.isaev.result.EvaluatedFeature;
import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author iisaev
 */
public class MeasureEvaluator {
    public Stream<EvaluatedFeature> evaluateFeatureMeasures(Stream<Feature> features,
                                                     List<Integer> classes,
                                                     Point measureCosts,
                                                     RelevanceMeasure... measures) {
        if (measureCosts.getCoordinates().length != measures.length) {
            throw new IllegalArgumentException("Number of given measures mismatch with measureCosts dimension");
        }
        return features.map(feature -> {
            List<Double> measureValues = Arrays.stream(measures).map(m -> m.evaluate(feature, classes)).collect(Collectors.toList());
            double result = 0.0;
            for (int i = 0; i < measureCosts.getCoordinates().length; ++i) {
                result += measureCosts.getCoordinates()[i] * measureValues.get(i);
            }
            return new EvaluatedFeature(feature, result);
        });
    }
}
