package ru.ifmo.ctddev.isaev.feature;

import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

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
                ).sorted((o1, o2) -> o1.getMeasure() < o2.getMeasure() ? 1 : -1)
                .limit(preferredSize)
                .collect(Collectors.toList());
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
