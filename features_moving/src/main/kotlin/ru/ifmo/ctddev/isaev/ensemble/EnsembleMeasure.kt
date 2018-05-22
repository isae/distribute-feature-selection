package ru.ifmo.ctddev.isaev.ensemble

import ru.ifmo.ctddev.isaev.Feature
import ru.ifmo.ctddev.isaev.RelevanceMeasure

/**
 * @author iisaev
 */
class EnsembleMeasure(private val leftCost: Double,
                      private val leftMeasure: RelevanceMeasure,
                      private val rightCost: Double,
                      private val rightMeasure: RelevanceMeasure) : RelevanceMeasure(0.0, 1.0) {
    override fun evaluate(feature: Feature, classes: List<Int>): Double {
        return leftCost * leftMeasure.evaluate(feature, classes) + rightCost * rightMeasure.evaluate(feature, classes)
    }
}