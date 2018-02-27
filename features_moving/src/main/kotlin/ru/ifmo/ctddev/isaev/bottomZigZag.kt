package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.None
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.Intersection
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

val measures = listOf(VDM::class, SpearmanRankCorrelation::class)

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    //val n = 100
    //val xData = 0.rangeTo(n).map { (it.toDouble()) / n }
    val xData = 0.rangeTo(100).map { x -> Point(x.toDouble() / 100, (100 - x).toDouble() / 100) }//listOf(listOf(0.0, 1.0), listOf(1.0, 0.0))
    println(xData.map { x -> x.toString() })
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val evaluatedDataWithNumbers = evaluatedData.map { it.mapIndexed { index, d -> Pair(index, d) } }
    val cutSize = 50
    val cutsForAllPoints = evaluatedDataWithNumbers
            .map { it.sortedBy { pair -> -pair.second } } //max first
            .map { it.take(cutSize) }
            .map { it.map { pair -> pair.first } }
    val sometimesInCut = cutsForAllPoints
            .flatMap { it }
            .toSet()
    println("Sometimes in cut: ${sometimesInCut.size} features")
    val alwaysInCut = sometimesInCut.filter { featureNum ->
        cutsForAllPoints.all { it.contains(featureNum) }
    }
    println("Always in cut: ${alwaysInCut.size} features: $alwaysInCut")
    val needToProcess = sometimesInCut - alwaysInCut
    println("Need to process: ${needToProcess.size} features")

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    // Create Chart

    val features = needToProcess.map { feature(it) }
    val lines = features.mapIndexed { index, coords -> Line("Feature $index", listOf(coords.first(), coords.last())) }
    //lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    val intersections = lines.flatMap { first -> lines.map { setOf(first, it) } }
            .filter { it.size > 1 }
            .distinct()
            .map { ArrayList(it) }
            .map { it[0].intersect(it[1]) }
            .filterNotNull()
            .sortedBy { it.point.x }
    println("Found ${intersections.size} intersections")
    //intersections.forEach { println("Intersection of ${it.line1.name} and ${it.line2.name} in point (%.2f, %.2f)".format(it.point.x, it.point.y)) }

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
    //pointsToTry.forEach { println("(%.3f, %.3f)".format(it.coordinates[0], it.coordinates[1])) }

    draw(features, xData, intersections)
}

private fun draw(features: List<List<Number>>, xData: List<Point>, intersections: List<Intersection>) {
    val chartBuilder = XYChartBuilder()
            .width(1024)
            .height(768)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Ensemble feature measure")
    val chart = XYChart(chartBuilder)
    features.forEachIndexed({ index, line -> addLine("Feature $index", xData, line, chart) })
    intersections.forEach { point ->
        val x = point.point.x
        val y = point.point.y
        val series = chart.addSeries(UUID.randomUUID().toString().take(10), listOf(x, x), listOf(0.0, y))
        series.lineColor = Color.BLACK
        series.markerColor = Color.BLACK
        series.marker = None()
        series.lineStyle = BasicStroke(0.1f)
    }

    // Show it
    SwingWrapper(chart).displayChart()

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}

private fun addLine(name: String, line: Line, chart: XYChart) {
    chart.addSeries(name, listOf(line.from.x, line.to.x), listOf(line.from.y, line.to.y)).marker = SeriesMarkers.NONE
}

private fun addLine(name: String, xData: List<Point>, line: List<Number>, chart: XYChart) {
    chart.addSeries(name, xData.map { it.coordinates[0] }, line).marker = SeriesMarkers.NONE
}