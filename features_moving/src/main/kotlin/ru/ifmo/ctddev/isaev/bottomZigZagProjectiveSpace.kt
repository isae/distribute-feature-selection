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
import ru.ifmo.ctddev.isaev.space.LinePoint
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import ru.ifmo.ctddev.isaev.space.processAllPointsFast
import java.awt.BasicStroke
import java.awt.Color
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val measures = listOf(SpearmanRankCorrelation::class, VDM::class)
    val epsilon = 1000
    val cutSize = 50
    val dataSet = KnownDatasets.DLBCL.read()

    logToConsole("Started the processing")
    val pointsInProjectiveCoords = 0.rangeTo(epsilon).map { x -> getPoint(x, epsilon) }
    logToConsole("${pointsInProjectiveCoords.size} points to calculate measures on")
    val (evaluatedData, cuttingLineY, cutsForAllPoints) = processAllPointsFast(pointsInProjectiveCoords, dataSet, measures, cutSize)
    val lastFeatureInAllCuts = cutsForAllPoints.map { it.last() }
    logToConsole("Evaluated data, calculated cutting line and cuts for all points")
    val sometimesInCut = cutsForAllPoints
            .flatMap { it }
            .toSet()
    logToConsole("Sometimes in cut: ${sometimesInCut.size} features")
    val alwaysInCut = sometimesInCut.filter { featureNum ->
        cutsForAllPoints.all { it.contains(featureNum) }
    }
    logToConsole("Always in cut: ${alwaysInCut.size} features: $alwaysInCut")
    val needToProcess = sometimesInCut - alwaysInCut
    logToConsole("Need to process: ${needToProcess.size} features")

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    val features = needToProcess.map { feature(it) }
    
    val lines = features // TODO: this is incorrect - we cannot draw lines in cartesian space from points in projective space 
            .mapIndexed { index, coords -> Line("Feature $index", doubleArrayOf(coords.first(), coords.last())) }
    //val cuttingLineY = lines.sortedBy { it.from.y }[cutSize - 1].from.y
    //lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    val lastFeatureInCutSwitchPositions = lastFeatureInAllCuts
            .mapIndexed { index, i -> Pair(index, i) }
            .filter { it.first == 0 || it.second != lastFeatureInAllCuts[it.first - 1] }
            .map { it.first }
    val pointsToTry = lastFeatureInCutSwitchPositions
            .map { getPoint(it, epsilon) }
    println(pointsToTry)

    val bottomFrontOfCuttingRule = lastFeatureInCutSwitchPositions
            .map { LinePoint(it.toDouble() / epsilon, cuttingLineY[it]) }


    logToConsole("Found ${pointsToTry.size} points to try")
    //pointsToTry.forEach { println("(%.3f, %.3f)".format(it.coordinates[0], it.coordinates[1])) }

    val pointsInCartesianCoords = pointsInProjectiveCoords
            .map { 
                val first = Math.asin(it.coordinates[0])
                Point(first, 1-first) 
            }
    draw(measures, lines, pointsInCartesianCoords, bottomFrontOfCuttingRule, cuttingLineY)
}

private fun getPoint(x: Int, epsilon: Int): Point {
    val fractionOfPi = Math.PI / epsilon
    val angle = fractionOfPi * x
    return Point.fromRawCoords(Math.sin(angle), Math.cos(angle))
}

private fun draw(measures: List<KClass<out RelevanceMeasure>>,
                 lines: List<Line>,
                 xData: List<Point>,
                 intersections: List<LinePoint>,
                 cuttingLineY: List<Double>) {
    val chartBuilder = XYChartBuilder()
            .width(1024)
            .height(768)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Ensemble feature measure")
    val chart = XYChart(chartBuilder)
    lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    chart.addSeries("Cutting line", xData.map { it.coordinates[0] }, cuttingLineY)
            .apply {
                this.marker = SeriesMarkers.NONE
                this.lineWidth = 3.0f
                this.lineColor = Color.BLACK
                this.lineStyle = BasicStroke(1.5f)
            }
    intersections.forEach { point ->
        val x = point.x
        val y = point.y
        val series = chart.addSeries(UUID.randomUUID().toString().take(10), listOf(x, x), listOf(0.0, y))
        series.lineColor = Color.BLACK
        series.markerColor = Color.BLACK
        series.marker = None()
        series.lineStyle = BasicStroke(0.1f)
    }
    // Show it
    logToConsole("Finished calculations; visualizing...")
    SwingWrapper(chart).displayChart()
    logToConsole("Finished visualization")

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}

private fun addLine(name: String, line: Line, chart: XYChart) {
    chart.addSeries(name, listOf(line.from.x, line.to.x), listOf(line.from.y, line.to.y)).marker = SeriesMarkers.NONE
}

private fun addLine(name: String, xData: List<Point>, line: DoubleArray, chart: XYChart) {
    chart.addSeries(name, xData.map { it.coordinates[0] }.toDoubleArray(), line).marker = SeriesMarkers.NONE
}