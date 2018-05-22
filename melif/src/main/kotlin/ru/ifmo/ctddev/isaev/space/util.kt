package ru.ifmo.ctddev.isaev.space

import ru.ifmo.ctddev.isaev.DataSetEvaluator
import ru.ifmo.ctddev.isaev.FeatureDataSet
import ru.ifmo.ctddev.isaev.RelevanceMeasure
import ru.ifmo.ctddev.isaev.point.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

typealias EvaluatedDataSet = List<DoubleArray>// [number of measures x number of features]

fun evaluateDataSet(dataSet: FeatureDataSet,
                    measureClasses: Array<out RelevanceMeasure>
): EvaluatedDataSet {
    return DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)
}

fun evaluatePoints(xData: List<Point>,
                   ds: EvaluatedDataSet
): List<DoubleArray> {
    return xData.map { point -> evaluatePoint(point, ds) }
}

fun evaluatePoints(xData: List<Point>,
                   dataSet: FeatureDataSet,
                   measureClasses: Array<out RelevanceMeasure>
): List<DoubleArray> {
    val valuesForEachMeasure = evaluateDataSet(dataSet, measureClasses)
    return xData.map { point -> evaluatePoint(point, valuesForEachMeasure) }
}

fun evaluatePoints(xData: List<Point>,
                   dataSet: FeatureDataSet,
                   measureClasses: Array<out RelevanceMeasure>,
                   position: Int
): List<Double> {
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measureClasses)// [number of measures x number of features]
    val measuresForEachFeature = 0.until(dataSet.features.size).map { i -> valuesForEachMeasure.map { it[i] } } // [number of features x number of measures]
    return xData
            .map { point ->
                evaluateDataSet(point, listOf(measuresForEachFeature[position]))[0]
            }
}

fun evaluatePoints(xData: List<Point>,
                   dataSet: FeatureDataSet,
                   measureClasses: Array<out RelevanceMeasure>,
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

fun evaluatePoint(measureCosts: Point,
                  valuesForEachMeasure: EvaluatedDataSet // [number of measures x number of features]
): DoubleArray {
    val result = DoubleArray(valuesForEachMeasure[0].size)
    valuesForEachMeasure[0].indices.forEach { i ->
        result[i] = measureCosts.coordinates.indices.sumByDouble { measureCosts.coordinates[it] * valuesForEachMeasure[it][i] }
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
fun getPointOnUnitSphereOld(angles: DoubleArray): Point { //TODO: review for 
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

// https://keisan.casio.com/exec/system/1359534351
fun getPointOnUnitSphere(angles: DoubleArray): Point { //TODO: review for
    val result = DoubleArray(angles.size + 1, { 1.0 })
    when (angles.size) {
        1 -> {
            result[0] = cos(angles[0])
            result[1] = sin(angles[0])
        }
        2 -> {
            result[0] = cos(angles[0]) * sin(angles[1])
            result[1] = sin(angles[0]) * sin(angles[1])
            result[2] = cos(angles[1])
        }
        else -> TODO("Not implemented conversion from sphere to cartesian for ${angles.size} dimensions")
    }
    return Point.fromRawCoords(*result)
}