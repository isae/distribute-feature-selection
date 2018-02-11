package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.Circle
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import java.awt.BasicStroke
import java.awt.Color
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.reflect.KClass


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val measures: Array<KClass<out RelevanceMeasure>> = arrayOf(VDM::class, SpearmanRankCorrelation::class)
    val dataSet = DataSetReader().readCsv(args[0])
    //val n = 100
    //val xData = 0.rangeTo(n).map { (it.toDouble()) / n }
    val xData = listOf(0.0, 1.0)
    println(xData.map { String.format("%.2f", it) })
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measures.toList())
    val evaluatedData = getEvaluatedData(xData, dataSet, valuesForEachMeasure)

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    // Create Chart
    val chartBuilder = XYChartBuilder()
            .width(1024)
            .height(768)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Ensemble feature measure")
    val chart = XYChart(chartBuilder)
    val featuresToTake = 20
    val lines = 0.rangeTo(featuresToTake).map { feature(it) }.mapIndexed { index, coords -> Line("Feature $index", coords) }
    lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    for (i in 0..featuresToTake) {
    }
    val intersections = lines.flatMap { first -> lines.map { setOf(first, it) } }
            .filter { it.size > 1 }
            .distinct()
            .map { ArrayList(it) }
            .map { it[0].intersect(it[1]) }
            .filterNotNull()
            .sortedBy { it.point.x }
    println("Found ${intersections.size} intersections")
    intersections.forEach { println("Intersection of ${it.line1.name} and ${it.line2.name} in point (%.2f, %.2f)".format(it.point.x, it.point.y)) }
    intersections.forEach { point ->
        val x = point.point.x
        val y = point.point.y
        val series = chart.addSeries(UUID.randomUUID().toString().take(10), listOf(x, x), listOf(0.0, y))
        series.lineColor = Color.BLACK
        series.markerColor = Color.BLACK
        series.marker = Circle()
        series.lineStyle = BasicStroke(0.1f)
    }
    val pointsToTry = 0.rangeTo(intersections.size)
            .map { i ->
                val left = if (i == 0) 0.0 else intersections[i - 1].point.x
                val right = if (i == intersections.size) 1.0 else intersections[i].point.x
                (left + right) / 2
            }
            .map { "%.2f".format(it) }
            .distinct()
            .map { it.toDouble() }
            .map { Point(it, 1 - it) }

    println("Found ${pointsToTry.size} points to try")
    pointsToTry.forEach { println("(%.2f, %.2f)".format(it.coordinates[0], it.coordinates[1])) }


    // Show it
    SwingWrapper(chart).displayChart()

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}

private fun addLine(name: String, line: Line, chart: XYChart) {
    chart.addSeries(name, listOf(line.from.x, line.to.x), listOf(line.from.y, line.to.y)).marker = SeriesMarkers.NONE
}

private fun getFeaturePositions(pos: Int,
                                evaluatedData: List<List<Double>>): List<Number> {
    val result = evaluatedData.map { it[pos] }
    println(result.map { String.format("%.2f", it) })
    return result
}

private fun getEvaluatedData(xData: List<Double>, dataSet: FeatureDataSet, valuesForEachMeasure: List<List<Double>>): List<List<Double>> {
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

private fun evaluateDataSet(dataSet: FeatureDataSet,
                            c1: Double,
                            valuesForEachMeasure: List<List<Double>>): Sequence<EvaluatedFeature> {
    val c2 = 1 - c1
    val measureCosts = Point(c1, c2)
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

private fun sumByDouble(it: List<Pair<Double, Double>>) = it
        .sumByDouble { (measureCost, measureValue) -> measureCost * measureValue }

private class LinePoint(val x: Double, val y: Double)

private class Intersection(val line1: Line, val line2: Line, val point: LinePoint)

private class Line(val name: String, val from: LinePoint, val to: LinePoint) {
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