package ru.ifmo.ctddev.isaev.space

import ru.ifmo.ctddev.isaev.EvaluatedFeature
import ru.ifmo.ctddev.isaev.FeatureDataSet
import ru.ifmo.ctddev.isaev.point.Point
import kotlin.math.abs

/**
 * @author iisaev
 */
class LinePoint(val x: Double, val y: Double)

class Intersection(val line1: Line, val line2: Line, val point: LinePoint)

class Line(val name: String, val from: LinePoint, val to: LinePoint) {
    private val k: Double = (to.y - from.y) / (to.x - from.x)

    private val b: Double = to.y - k * to.x

    constructor(name: String, ys: List<Number>) : this(name, LinePoint(0.0, ys[0].toDouble()), LinePoint(1.0, ys[1].toDouble())) {
        assert(ys.size == 2)
    }

    fun intersect(second: Line): Intersection? {
        return if (abs(k - second.k) < 0.00001) {
            null // parallel lines
        } else {
            val x = (b - second.b) / (second.k - k)
            val y = (second.k * b - k * second.b) / (second.k - k)
            if (x < 0.0 || x > 1.0) {
                null // intersection is out of range
            } else {
                Intersection(this, second, LinePoint(x, y))
            }
        }
    }
}

fun getFeaturePositions(pos: Int,
                        evaluatedData: List<List<Double>>): List<Number> {
    val result = evaluatedData.map { it[pos] }
    println(result.map { String.format("%.2f", it) })
    return result
}

fun getEvaluatedData(xData: List<List<Double>>, dataSet: FeatureDataSet, valuesForEachMeasure: List<List<Double>>): List<List<Double>> {
    return xData
            .map {
                val sortedFeatures = evaluateDataSet(dataSet, it, valuesForEachMeasure)
                        .zip(generateSequence(0, { it + 1 }))
                        .sortedBy { it.first.name }
                        .toList()
                val point = sortedFeatures
                        .map { it.first.measure }
                point
            }
}

fun evaluateDataSet(dataSet: FeatureDataSet,
                    costs: List<Double>,
                    valuesForEachMeasure: List<List<Double>>): Sequence<EvaluatedFeature> {
    val measureCosts = Point(*costs.toDoubleArray())
    val ensembleMeasures = 0.until(dataSet.features.size)
            .map { i ->
                val measuresForParticularFeature = valuesForEachMeasure
                        .map { it[i] }
                measureCosts.coordinates
                        .zip(measuresForParticularFeature)
            }
            .map { sumByDouble(it) }
    val sortedBy = dataSet.features.zip(ensembleMeasures)
            .map { (f, m) -> EvaluatedFeature(f, m) }
            .sortedBy { it.measure }
    return sortedBy
            .asSequence()
}

fun sumByDouble(it: List<Pair<Double, Double>>) = it
        .sumByDouble { (measureCost, measureValue) -> measureCost * measureValue }