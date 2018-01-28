package ru.ifmo.ctddev.isaev.feature

import ru.ifmo.ctddev.isaev.Feature

/**
 * @author iisaev
 */
abstract class RelevanceMeasure {
    abstract fun evaluate(feature: Feature, classes: List<Int>): Double

    override fun toString(): String {
        return javaClass.simpleName
    }
}

class SpearmanRankCorrelation : RelevanceMeasure() {

    override fun evaluate(feature: Feature, classes: List<Int>): Double {
        return evaluate(
                feature.values.map { it.toDouble() },
                classes.map { it.toDouble() }
        )
    }

    fun evaluate(values: List<Double>, classes: List<Double>): Double {

        val xMean = values.average()
        val yMean = classes.average()

        val sumDeviationsXY = values.map { it - xMean }
                .zip(classes.map { it - yMean })
                .map { (devX, devY) -> devX * devY }
                .sum()

        val squaredDeviationX = values
                .map { it - xMean }
                .map { it * it }
                .sum()

        val squaredDeviationY = classes
                .map { it - yMean }
                .map { it * it }
                .sum()

        return sumDeviationsXY / Math.sqrt(squaredDeviationX * squaredDeviationY)
    }
}

class FitCriterion : RelevanceMeasure() {
    override fun evaluate(feature: Feature, classes: List<Int>): Double {
        val values = feature.values
        val mean0 = calculateMean(0, values, classes)
        val mean1 = calculateMean(1, values, classes)
        val var0 = calculateVariance(0, mean0, values, classes)
        val var1 = calculateVariance(1, mean1, values, classes)

        val fcpSum = values.map { calculateFCP(it, mean0, mean1, var0, var1) }
                .zip(classes)
                .filter { (fcp, clazz) -> fcp == clazz }
                .count()
        return fcpSum.toDouble() / classes.size
    }

    private fun calculateVariance(clazz: Int, mean0: Double, values: List<Int>, classes: List<Int>): Double {
        return (0 until classes.size)
                .filter { i -> classes[i] == clazz }
                .map { index -> Math.pow(values[index] - mean0, 2.0) }
                .average()
    }

    private fun calculateMean(clazz: Int, values: List<Int>, classes: List<Int>): Double {
        return (0 until classes.size)
                .filter { classes[it] == clazz }
                .map({ values[it] })
                .average()
    }

    private fun calculateFCP(value: Int, mean0: Double, mean1: Double, var0: Double, var1: Double): Int {
        val val0 = Math.abs(value - mean0) / var0
        val val1 = Math.abs(value - mean1) / var1
        return if (val0 < val1) 0 else 1
    }
}
