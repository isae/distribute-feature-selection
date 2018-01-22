package ru.ifmo.ctddev.isaev

import ru.ifmo.ctddev.isaev.dataset.Feature
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure
import ru.ifmo.ctddev.isaev.folds.FoldsEvaluator
import ru.ifmo.ctddev.isaev.result.Point
import java.lang.Double.compare

/**
 * @author iisaev
 */
class AlgorithmConfig(val delta: Double,
                      val foldsEvaluator: FoldsEvaluator,
                      val measures: Array<RelevanceMeasure>)

class EvaluatedFeature(feature: Feature, val measure: Double)
    : Feature(feature.name, feature.values)

class SelectionResult(val selectedFeatures: List<Feature>,
                      val point: Point,
                      val score: Double) : Comparable<SelectionResult> {

    override fun compareTo(other: SelectionResult): Int {
        return compare(score, other.score)
    }

    fun betterThan(bestScore: SelectionResult): Boolean {
        return this.compareTo(bestScore) == 1
    }
}
