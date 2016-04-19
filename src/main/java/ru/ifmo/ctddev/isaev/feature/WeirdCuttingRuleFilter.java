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
public class WeirdCuttingRuleFilter extends DatasetFilter{

    public FeatureDataSet filterDataset(FeatureDataSet original, Point measureCosts,
                                        RunStats runStats) {
        List<Feature> filteredFeatures = evaluateFeatures(original, measureCosts, runStats)
                .limit(123)
                .collect(Collectors.toList());
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
