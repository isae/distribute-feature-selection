package filter;

import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.result.EvaluatedFeature;
import ru.ifmo.ctddev.isaev.result.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class PercentFilter extends DatasetFilter {
    private final int percents;

    public PercentFilter(int percents) {
        this.percents = percents;
    }

    public FeatureDataSet filterDataSet(FeatureDataSet original, Point measureCosts,
                                        RelevanceMeasure[] measures) {
        List<EvaluatedFeature> evaluatedFeatures = evaluateFeatures(original, measureCosts, measures).collect(Collectors.toList());
        int featureToSelect = (int) (((double) evaluatedFeatures.size() * percents) / 100);
        List<Feature> filteredFeatures = new ArrayList<>(evaluatedFeatures.subList(0, featureToSelect));
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
