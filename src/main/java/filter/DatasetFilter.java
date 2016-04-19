package filter;

import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.feature.MeasureEvaluator;
import ru.ifmo.ctddev.isaev.result.EvaluatedFeature;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;

import java.util.stream.Stream;


/**
 * @author iisaev
 */
public abstract class DatasetFilter {
    private final MeasureEvaluator measureEvaluator = new MeasureEvaluator();

    protected Stream<EvaluatedFeature> evaluateFeatures(FeatureDataSet original, Point measureCosts,
                                                        RunStats runStats) {
        return measureEvaluator
                .evaluateFeatureMeasures(
                        original.getFeatures().stream(),
                        original.getClasses(),
                        measureCosts,
                        runStats.getMeasures()
                ).sorted((o1, o2) -> {
                    if (o1.getMeasure() == o2.getMeasure()) {
                        return 0;
                    } else {
                        return o1.getMeasure() < o2.getMeasure() ? 1 : -1;
                    }
                });
    }

    public abstract FeatureDataSet filterDataSet(FeatureDataSet original, Point measureCosts,
                                                 RunStats runStats);
}
