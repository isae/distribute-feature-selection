package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.None
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.Line
import ru.ifmo.ctddev.isaev.space.getEvaluatedData
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import java.awt.BasicStroke
import java.awt.Color
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val measures = listOf(VDM::class, SpearmanRankCorrelation::class)
    val dataSet = KnownDatasets.ARIZONA5.read()
    //val n = 100
    //val xData = 0.rangeTo(n).map { (it.toDouble()) / n }
    val xData = 0.rangeTo(100).map { x -> Point(x.toDouble() / 100, (100 - x).toDouble() / 100) }//listOf(listOf(0.0, 1.0), listOf(1.0, 0.0))
    println(xData.map { x -> x.toString() })
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    // Create Chart
    val chartBuilder = XYChartBuilder()
            .width(1024)
            .height(768)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Ensemble feature measure")
    val chart = XYChart(chartBuilder)
    val featuresToTake = 50
    val lines = 0.rangeTo(featuresToTake).map { feature(it) }.mapIndexed { index, coords -> Line("Feature $index", doubleArrayOf(coords.first(), coords.last())) }
    //lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    val features = 0.rangeTo(featuresToTake).map { feature(it) }
    features.forEachIndexed({ index, line -> addLine("Feature $index", xData, line, chart) })
    val intersections = lines.flatMap { first -> lines.map { setOf(first, it) } }
            .filter { it.size > 1 }
            .distinct()
            .map { ArrayList(it) }.mapNotNull { it[0].intersect(it[1]) }
            .sortedBy { it.point.x }
    println("Found ${intersections.size} intersections")
    intersections.forEach { println("Intersection of ${it.line1.name} and ${it.line2.name} in point (%.2f, %.2f)".format(it.point.x, it.point.y)) }
    intersections.forEach { point ->
        val x = point.point.x
        val y = point.point.y
        val series = chart.addSeries(UUID.randomUUID().toString().take(10), listOf(x, x), listOf(0.0, y))
        series.lineColor = Color.BLACK
        series.markerColor = Color.BLACK
        series.marker = None()
        series.lineStyle = BasicStroke(0.1f)
    }
    val pointsToTry = 0.rangeTo(intersections.size)
            .map { i ->
                val left = if (i == 0) 0.0 else intersections[i - 1].point.x
                val right = if (i == intersections.size) 1.0 else intersections[i].point.x
                (left + right) / 2
            }
            .map { "%.3f".format(it) }
            .distinct()
            .map { it.toDouble() }
            .map { Point(it, 1 - it) }

    println("Found ${pointsToTry.size} points to try")
    pointsToTry.forEach { println("(%.3f, %.3f)".format(it.coordinates[0], it.coordinates[1])) }


    // Show it
    SwingWrapper(chart).displayChart()

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}

private fun addLine(name: String, line: Line, chart: XYChart) {
    chart.addSeries(name, listOf(line.from.x, line.to.x), listOf(line.from.y, line.to.y)).marker = SeriesMarkers.NONE
}

private fun addLine(name: String, xData: List<Point>, line: DoubleArray, chart: XYChart) {
    chart.addSeries(name, xData.map { it.coordinates[0] }.toDoubleArray(), line).marker = SeriesMarkers.NONE
}