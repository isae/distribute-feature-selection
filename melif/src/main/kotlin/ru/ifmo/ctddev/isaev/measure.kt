package ru.ifmo.ctddev.isaev

/**
 * @author iisaev
 */
abstract class RelevanceMeasure(val minValue: Double, val maxValue: Double) {
    abstract fun evaluate(feature: Feature, classes: List<Int>): Double

    fun evaluate(original: FeatureDataSet): DoubleArray {
        return original.features.map { evaluate(it, original.classes) }
                .toDoubleArray()
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}

private fun toDoubleArray(values: List<Int>): DoubleArray {
    val featureValues = DoubleArray(values.size)
    values.forEachIndexed { index, value ->
        featureValues[index] = value.toDouble()
    }
    return featureValues
}

class SpearmanRankCorrelation : RelevanceMeasure(-1.0, 1.0) {

    override fun evaluate(feature: Feature, classes: List<Int>): Double {
        val featureValues = toDoubleArray(feature.values)
        val doubleClasses = toDoubleArray(classes)
        return evaluate(
                featureValues,
                doubleClasses
        )
    }

    fun evaluate(values: DoubleArray, classes: DoubleArray): Double {

        val xMean = values.average()
        val yMean = classes.average()

        val sumDeviationsXY = values.indices
                .map { i ->
                    val devX = values[i] - xMean
                    val devY = classes[i] - yMean
                    devX * devY
                }
                .sum()

        val squaredDeviationX = values.asSequence()
                .map { it - xMean }
                .map { it * it }
                .sum()

        val squaredDeviationY = classes.asSequence()
                .map { it - yMean }
                .map { it * it }
                .sum()

        return sumDeviationsXY / Math.sqrt(squaredDeviationX * squaredDeviationY)
    }
}

class FitCriterion : RelevanceMeasure(0.0, 1.0) {
    override fun evaluate(feature: Feature, classes: List<Int>): Double {
        val values = feature.values
        val mean0 = calculateMean(0, values, classes)
        val mean1 = calculateMean(1, values, classes)
        val var0 = calculateVariance(0, mean0, values, classes)
        val var1 = calculateVariance(1, mean1, values, classes)

        val fcpSum = values.indices
                .filter { i ->
                    val fcp = calculateFCP(values[i], mean0, mean1, var0, var1)
                    val clazz = classes[i]
                    fcp == clazz
                }
                .count()
        return fcpSum.toDouble() / classes.size
    }

    private fun calculateVariance(clazz: Int, mean0: Double, values: List<Int>, classes: List<Int>): Double {
        return classes.indices
                .filter { i -> classes[i] == clazz }
                .map { index -> Math.pow(values[index] - mean0, 2.0) }
                .average()
    }

    private fun calculateMean(clazz: Int, values: List<Int>, classes: List<Int>): Double {
        return classes.indices
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
