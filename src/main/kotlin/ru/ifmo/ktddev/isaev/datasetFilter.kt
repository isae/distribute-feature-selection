package ru.ifmo.ktddev.isaev

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.result.Point
import java.util.*

/**
 * @author iisaev
 */
sealed class DataSetFilter {

    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    protected fun evaluateFeatures(original: FeatureDataSet, measureCosts: Point,
                                   measures: Array<RelevanceMeasure>): Sequence<EvaluatedFeature> {
        return evaluateMeasures(original.features.asSequence(), original.classes, measureCosts, *measures)
                .sortedBy { it.measure }
    }

    abstract fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet
}

class PercentFilter(private val percents: Int) : DataSetFilter() {

    override fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet {
        val evaluatedFeatures = evaluateFeatures(original, measureCosts, measures).toList()
        val featureToSelect = (evaluatedFeatures.size.toDouble() * percents / 100).toInt()
        val filteredFeatures = ArrayList<Feature>(evaluatedFeatures.subList(0, featureToSelect))
        return FeatureDataSet(filteredFeatures, original.classes, original.name)
    }

}

class PreferredSizeFilter(val preferredSize: Int) : DataSetFilter() {

    init {
        logger.info("Initialized dataset filter with preferred size {}", preferredSize)
    }

    override fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet {
        val filteredFeatures = evaluateFeatures(original, measureCosts, measures)
                .take(preferredSize)
        return FeatureDataSet(filteredFeatures.toList(), original.classes, original.name)
    }

}

class WyrdCuttingRuleFilter : DataSetFilter() {

    override fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet {
        val filteredFeatures = evaluateFeatures(original, measureCosts, measures)
        val mean = filteredFeatures
                .map({ it.measure })
                .average()
        val std = Math.sqrt(
                filteredFeatures
                        .map({ it.measure })
                        .map { x -> Math.pow(x - mean, 2.0) }
                        .average()
        )
        val inRange = filteredFeatures
                .filter { f -> isInRange(f, mean, std) }
                .count()
        val result = filteredFeatures.toList().subList(0, inRange)
        return FeatureDataSet(result, original.classes, original.name)
    }

    private fun isInRange(feature: EvaluatedFeature,
                          mean: Double,
                          std: Double): Boolean {
        return feature.measure > mean - std && feature.measure < mean + std
    }

}
