package ru.ifmo.ctddev.isaev.feature

import ru.ifmo.ctddev.isaev.EvaluatedFeature
import ru.ifmo.ctddev.isaev.Feature
import ru.ifmo.ctddev.isaev.result.Point

/**
 * @author iisaev
 */
fun evaluateMeasures(features: Sequence<Feature>,
                     classes: List<Int>,
                     measureCosts: Point,
                     vararg measures: RelevanceMeasure): Sequence<EvaluatedFeature> {
    if (measureCosts.coordinates.size != measures.size) {
        throw IllegalArgumentException("Number of given measures mismatch with measureCosts dimension")
    }

    fun evaluateFeature(feature: Feature): Double {
        return measures
                .map { m -> m.evaluate(feature, classes) }
                .toDoubleArray()
                .zip(measureCosts.coordinates)
                .sumByDouble { it.first * it.second }
    }

    return features.map { EvaluatedFeature(it, evaluateFeature(it)) }
}
