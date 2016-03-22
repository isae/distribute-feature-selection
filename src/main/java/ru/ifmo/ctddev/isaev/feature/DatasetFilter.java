package ru.ifmo.ctddev.isaev.feature;

import ru.ifmo.ctddev.isaev.result.EvaluatedFeature;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class DatasetFilter {
    private final MeasureEvaluator measureEvaluator = new MeasureEvaluator();

    public FeatureDataSet filterDataset(FeatureDataSet original, Integer preferredSize, Point measureCosts,
                                        RunStats runStats) {
        List<Feature> filteredFeatures = measureEvaluator
                .evaluateFeatureMeasures(
                        original.getFeatures().stream(),
                        original.getClasses(),
                        measureCosts,
                        runStats.getMeasures()
                ).sorted(Comparator.comparingDouble(EvaluatedFeature::getMeasure))
                .limit(preferredSize)
                .map((evaluatedFeature) -> {
                    return evaluatedFeature.getFeature();
                }).collect(Collectors.toList());
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
