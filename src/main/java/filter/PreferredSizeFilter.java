package filter;

import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author iisaev
 */
public class PreferredSizeFilter extends DatasetFilter {
    private final int preferredSize;

    public PreferredSizeFilter(int preferredSize) {
        this.preferredSize = preferredSize;
    }

    public FeatureDataSet filterDataSet(FeatureDataSet original, Point measureCosts,
                                        RunStats runStats) {
        List<Feature> filteredFeatures = evaluateFeatures(original, measureCosts, runStats)
                .limit(preferredSize)
                .collect(Collectors.toList());
        return new FeatureDataSet(filteredFeatures, original.getClasses(), original.getName());
    }

}
