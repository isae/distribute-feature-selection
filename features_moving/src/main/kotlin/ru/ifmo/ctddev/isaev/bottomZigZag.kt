package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
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


/**
 * @author iisaev
 */

val measures = listOf(SpearmanRankCorrelation::class, VDM::class)
const val epsilon = 1000
const val cutSize = 50
val dataSet = KnownDatasets.DLBCL.read()

fun main(args: Array<String>) {
    logToConsole("Started the processing")
    val xData = 0.rangeTo(epsilon).map { x -> Point(x.toDouble() / epsilon, (epsilon - x).toDouble() / epsilon) }
    logToConsole("${xData.size} points to calculate measures on")
    val (evaluatedData, cuttingLineY, cutsForAllPoints) = processAllPointsFast(xData)
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

    // Create Chart

    val features = needToProcess.map { feature(it) }
    val rawLines = features
            .mapIndexed { index, coords -> Line("Feature $index", doubleArrayOf(coords.first(), coords.last())) }
    //val cuttingLineY = rawLines.sortedBy { it.from.y }[cutSize - 1].from.y
    val lines = rawLines
    //lines.forEachIndexed({ index, line -> addLine("Feature $index", line, chart) })
    val intersections = lines.flatMap { first -> lines.map { setOf(first, it) } }
            .filter { it.size > 1 }
            .distinct()
            .map { ArrayList(it) }
            .map { it[0].intersect(it[1]) }
            .filterNotNull()
            .sortedBy { it.point.x }
    logToConsole("Found ${intersections.size} intersections")
    //intersections.forEach { println("Intersection of ${it.line1.name} and ${it.line2.name} in point (%.2f, %.2f)".format(it.point.x, it.point.y)) }

    /* val pointsToTry = 0.rangeTo(intersections.size)
             .map { i ->
                 val left = if (i == 0) 0.0 else intersections[i - 1].point.x
                 val right = if (i == intersections.size) 1.0 else intersections[i].point.x
                 (left + right) / 2
             }
             .map { Point(it, 1 - it) }
 */
    val pointsToTry = lastFeatureInAllCuts
            .filterIndexed({ i, feature -> i == 0 || feature != lastFeatureInAllCuts[i - 1] })
            .mapIndexed({ i, _ -> Point(i.toDouble() / epsilon, (epsilon - i).toDouble() / epsilon) })
    logToConsole("Found ${pointsToTry.size} points to try")
    //pointsToTry.forEach { println("(%.3f, %.3f)".format(it.coordinates[0], it.coordinates[1])) }

    draw(lines, xData, intersections, cuttingLineY)
}

private fun processAllPoints(xData: List<Point>): Triple<List<DoubleArray>, List<Double>, List<List<Int>>> {
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val evaluatedDataWithNumbers = evaluatedData.map { it.mapIndexed { index, d -> Pair(index, d) } }

    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map { it.sortedBy { pair -> -pair.second } } //max first
    val cuttingLineY = rawCutsForAllPoints.map { it[cutSize - 1].second }
    val cutsForAllPoints = rawCutsForAllPoints
            .map { it.take(cutSize) }
            .map { it.map { pair -> pair.first } }
    return Triple(evaluatedData, cuttingLineY, cutsForAllPoints)
}

private fun processAllPointsFast(xData: List<Point>): Triple<List<DoubleArray>, List<Double>, List<List<Int>>> {
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val range = Array(evaluatedData[0].size, { it })
    val evaluatedDataWithNumbers = evaluatedData.map { Pair(it, range.clone()) }
    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map {
                val featureNumbers = it.second
                val featureMeasures = it.first
                val comparator = kotlin.Comparator<Int> { o1, o2 -> compareValuesBy(o1, o2, { -featureMeasures[it] }) }
                Arrays.sort(featureNumbers, comparator)
                return@map Pair(featureMeasures, featureNumbers)
            } //max first
    val cuttingLineY = rawCutsForAllPoints
            .map {
                val lastFeatureInCut = it.second[cutSize - 1]
                it.first[lastFeatureInCut]
            }
    val cutsForAllPoints = rawCutsForAllPoints
            .map { it.second.take(cutSize) }
    return Triple(evaluatedData, cuttingLineY, cutsForAllPoints)
}

private fun draw(lines: List<Line>,
                 xData: List<Point>,
                 intersections: List<Intersection>,
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
    /* intersections.forEach { point ->
         val x = point.point.x
         val y = point.point.y
         val series = chart.addSeries(UUID.randomUUID().toString().take(10), listOf(x, x), listOf(0.0, y))
         series.lineColor = Color.BLACK
         series.markerColor = Color.BLACK
         series.marker = None()
         series.lineStyle = BasicStroke(0.1f)
     }
 */
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

internal fun addLine(name: String, xData: List<Point>, line: DoubleArray, chart: XYChart) {
    chart.addSeries(name, xData.map { it.coordinates[0] }.toDoubleArray(), line).marker = SeriesMarkers.NONE
}