package ru.ifmo.ctddev.isaev.space

import ru.ifmo.ctddev.isaev.DataSetEvaluator
import ru.ifmo.ctddev.isaev.FeatureDataSet
import ru.ifmo.ctddev.isaev.RelevanceMeasure
import ru.ifmo.ctddev.isaev.point.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KClass

/**
 * @author iisaev
 */
class LinePoint(val x: Double, val y: Double)

class Intersection(val line1: Line, val line2: Line, val point: LinePoint)

class Line(val name: String, val from: LinePoint, val to: LinePoint) {
    private val k: Double = (to.y - from.y) / (to.x - from.x)

    private val b: Double = to.y - k * to.x

    constructor(name: String, ys: DoubleArray) : this(name, LinePoint(0.0, ys[0]), LinePoint(1.0, ys[1])) {
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
                        evaluatedData: List<DoubleArray>): DoubleArray {
    return evaluatedData.map { it[pos] }.toDoubleArray()
}

typealias Matrix = List<List<Double>>

fun getEvaluatedData(xData: List<Point>,
                     dataSet: FeatureDataSet,
                     measureClasses: List<KClass<out RelevanceMeasure>>
): List<DoubleArray> {
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)// [number of measures x number of features]
    return xData.map { point -> evaluateTransponedDataSet(point, valuesForEachMeasure) }
}

fun getEvaluatedData(xData: List<Point>,
                     dataSet: FeatureDataSet,
                     measureClasses: List<KClass<out RelevanceMeasure>>,
                     position: Int
): List<Double> {
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)// [number of measures x number of features]
    val measuresForEachFeature = 0.until(dataSet.features.size).map { i -> valuesForEachMeasure.map { it[i] } } // [number of features x number of measures]
    return xData
            .map { point ->
                evaluateDataSet(point, listOf(measuresForEachFeature[position]))[0]
            }
}

fun getEvaluatedData(xData: List<Point>,
                     dataSet: FeatureDataSet,
                     measureClasses: List<KClass<out RelevanceMeasure>>,
                     positions: Set<Int>
): List<DoubleArray> {
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)// [number of measures x number of features]
    val measuresForEachFeature = 0.until(dataSet.features.size).map { i -> valuesForEachMeasure.map { it[i] } } // [number of features x number of measures]
    val measuresToProcess = positions.map { measuresForEachFeature[it] }
    return xData
            .map { point -> evaluateDataSet(point, measuresToProcess) }
}

fun <T> calculateTime(block: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}

fun evaluateTransponedDataSet(measureCosts: Point,
                              valuesForEachMeasure: List<DoubleArray> // [number of measures x number of features]
): DoubleArray {
    val result = DoubleArray(valuesForEachMeasure[0].size)
    valuesForEachMeasure[0].indices.forEach { i ->
        val m = (0 until measureCosts.coordinates.size)
                .sumByDouble { measureCosts.coordinates[it] * valuesForEachMeasure[it][i] }
        result[i] = m
    }
    return result
}


fun evaluateDataSet(measureCosts: Point,
                    measuresForEachFeature: Matrix
): DoubleArray {
    val result = DoubleArray(measuresForEachFeature.size)
    for (i in 0 until measuresForEachFeature.size) {
        val m = (0 until measureCosts.coordinates.size)
                .sumByDouble { measureCosts.coordinates[it] * measuresForEachFeature[i][it] }
        result[i] = m
    }
    return result
}

fun getPointOnUnitSphere(angle: Double): Point {
    return getPointOnUnitSphere(doubleArrayOf(angle))
}

// https://ru.wikipedia.org/wiki/Гиперсфера#Гиперсферические_координаты
fun getPointOnUnitSphere(angles: DoubleArray): Point { //TODO: review for 
    val result = DoubleArray(angles.size + 1, { 1.0 })
    val sines = DoubleArray(angles.size) //cache for faster computation 
    val cosines = DoubleArray(angles.size)
    angles.forEachIndexed({ i, angle ->
        sines[i] = sin(angle)
        cosines[i] = cos(angle)
    })
    0.until(result.size - 1).forEach { i ->
        i.until(angles.size).forEach { j ->
            result[i] *= sines[j]
        }
    }
    1.until(result.size).forEach { i ->
        result[i] *= cosines[i - 1]
    }
    return Point.fromRawCoords(*result)
}