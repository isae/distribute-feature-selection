package ru.ifmo.ctddev.isaev.filter;

import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.result.Point;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class PreferredSizeFilter extends DataSetFilter {
    private final int preferredSize;

    public PreferredSizeFilter(int preferredSize) {
        this.preferredSize = preferredSize;
        logger.info("Initialized dataset filter with preferred size {}", preferredSize);
    }

    public FeatureDataSet filterDataSet(FeatureDataSet original, Point measureCosts,
                                        RelevanceMeasure[] measures) {
        List<Feature> filteredFeatures = evaluateFeatures(original, measureCosts, measures)
                .limit(preferredSize)
                .collect(Collectors.toList());
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
