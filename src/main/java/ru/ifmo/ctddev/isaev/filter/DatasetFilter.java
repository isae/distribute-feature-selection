package ru.ifmo.ctddev.isaev.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import ru.ifmo.ctddev.isaev.feature.MeasureEvaluator;
import ru.ifmo.ctddev.isaev.feature.measure.RelevanceMeasure;
import ru.ifmo.ctddev.isaev.result.EvaluatedFeature;
import ru.ifmo.ctddev.isaev.result.Point;

import java.util.stream.Stream;


/**
 * @author iisaev
 */
public abstract class DatasetFilter {
    private final MeasureEvaluator measureEvaluator = new MeasureEvaluator();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Stream<EvaluatedFeature> evaluateFeatures(FeatureDataSet original, Point measureCosts,
                                                        RelevanceMeasure[] measures) {
        return measureEvaluator
                .evaluateFeatureMeasures(
                        original.getFeatures().stream(),
                        original.getClasses(),
                        measureCosts,
                        measures
                ).sorted((o1, o2) -> {
                    if (o1.getMeasure() == o2.getMeasure()) {
                        return 0;
                    } else {
                        return o1.getMeasure() < o2.getMeasure() ? 1 : -1;
                    }
                });
    }

    public abstract FeatureDataSet filterDataSet(FeatureDataSet original, Point measureCosts,
                                                 RelevanceMeasure[] measures);
}
