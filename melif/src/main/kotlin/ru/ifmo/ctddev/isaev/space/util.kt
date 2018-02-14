package ru.ifmo.ctddev.isaev.space

import ru.ifmo.ctddev.isaev.DataSetEvaluator
import ru.ifmo.ctddev.isaev.EvaluatedFeature
import ru.ifmo.ctddev.isaev.FeatureDataSet
import ru.ifmo.ctddev.isaev.RelevanceMeasure
import ru.ifmo.ctddev.isaev.point.Point
import kotlin.math.abs
import kotlin.reflect.KClass

/**
 * @author iisaev
 */
class LinePoint(val x: Double, val y: Double)

class Intersection(val line1: Line, val line2: Line, val point: LinePoint)

class Line(val name: String, val from: LinePoint, val to: LinePoint) {
    private val k: Double = (to.y - from.y) / (to.x - from.x)

    private val b: Double = to.y - k * to.x

    constructor(name: String, ys: List<Number>) : this(name, LinePoint(0.0, ys[0].toDouble()), LinePoint(1.0, ys[1].toDouble())) {
        if (ys.size != 2) {
            throw IllegalStateException("Invalid constructor invocation, list size is not 2")
        }
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

typealias Matrix = List<List<Double>>

fun getEvaluatedData(xData: List<Point>, // [number of points x number of features]
                     dataSet: FeatureDataSet,
                     measureClasses: List<KClass<out RelevanceMeasure>>
): List<List<Double>> {
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)// [number of measures x number of features]
    val measuresForEachFeature = 0.until(dataSet.features.size).map { i -> valuesForEachMeasure.map { it[i] } } // [number of features x number of measures]
    return xData
            .map { point ->
                evaluateDataSet(dataSet, point, measuresForEachFeature)
                        .map { it.measure }
            }
}

fun evaluateDataSet(dataSet: FeatureDataSet,
                    measureCosts: Point,
                    measuresForEachFeature: Matrix
): List<EvaluatedFeature> {
    val result = ArrayList<EvaluatedFeature>(measuresForEachFeature.size)
    for (i in 0..measuresForEachFeature.size - 1) {
        var m = 0.0
        for (j in 0..measureCosts.coordinates.size - 1) {
            m += measureCosts.coordinates[j] * measuresForEachFeature[i][j]
        }
        result.add(EvaluatedFeature(dataSet.features[i], m))
    }
    return result
}

fun sumByDouble(it: List<Pair<Double, Double>>) = it
        .sumByDouble { (measureCost, measureValue) -> measureCost * measureValue }