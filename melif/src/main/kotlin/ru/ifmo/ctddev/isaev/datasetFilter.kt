package ru.ifmo.ctddev.isaev

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.point.Point
import java.util.*

/**
 * @author iisaev
 */
fun evaluateMeasures(features: List<Feature>,
                     classes: List<Int>,
                     measureCosts: Point,
                     vararg measures: RelevanceMeasure): List<EvaluatedFeature> {
    if (measureCosts.coordinates.size != measures.size) {
        throw IllegalArgumentException("Number of given measures mismatch with measureCosts dimension")
    }

    val valuesForEachMeasure = measures.map { m ->
        features.map { m.evaluate(it, classes) }
                .toList()
                .normalize()
    }
    val ensembleMeasures = 0.until(features.size)
            .map { i -> measureCosts.coordinates.zip(valuesForEachMeasure.map { it[i] }) }
            .map { it.sumByDouble { (measureCost, measureValue) -> measureCost * measureValue } }

    return features.zip(ensembleMeasures)
            .map { (f, m) -> EvaluatedFeature(f, m) }
}

private fun Iterable<Double>.normalize(): List<Double> {
    if (!this.iterator().hasNext()) {
        return emptyList()
    }
    val max = this.max()!!
    val min = this.min()!!
    return this.map { (it - min) / (max - min) }
}

class DataSetEvaluator {
    fun evaluateFeatures(original: FeatureDataSet,
                         measureCosts: Point,
                         measures: Array<RelevanceMeasure>
    ): Sequence<EvaluatedFeature> {
        return evaluateMeasures(original.features, original.classes, measureCosts, *measures)
                .sortedBy { it.measure }
                .asSequence()
    }
}

sealed class DataSetFilter {

    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    abstract fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet
}

interface CuttingRule {
    fun cut(features: List<EvaluatedFeature>): List<EvaluatedFeature>
}

class PercentFilter(private val percents: Int) : DataSetFilter() {

    override fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet {
        val evaluatedFeatures = DataSetEvaluator().evaluateFeatures(original, measureCosts, measures).toList()
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
        val filteredFeatures = DataSetEvaluator().evaluateFeatures(original, measureCosts, measures)
                .take(preferredSize)
        return FeatureDataSet(filteredFeatures.toList(), original.classes, original.name)
    }

}

class WyrdCuttingRuleFilter : DataSetFilter() {

    override fun filterDataSet(original: FeatureDataSet, measureCosts: Point,
                               measures: Array<RelevanceMeasure>): FeatureDataSet {
        val filteredFeatures = DataSetEvaluator().evaluateFeatures(original, measureCosts, measures)
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
