package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.None
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*
import java.awt.BasicStroke
import java.awt.Color
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()

private data class PointProcessingResult2d(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<Set<Int>>,
        val cutChangePositions: List<Int>
)

private data class PointProcessingFinalResult2d(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<Set<Int>>,
        val pointsToTry: List<Point>,
        val angles: List<Double>
)

fun main(args: Array<String>) {
    val (evaluatedData, cuttingLineY, cutsForAllPoints, pointsToTry, angles) = processAllPointsWithEnrichment(10)
    println("Found ${pointsToTry.size} points to try with enrichment")
    println(pointsToTry)

    val features = getFeaturesToDraw(cutsForAllPoints, evaluatedData)

    val chartBuilder = XYChartBuilder()
            .width(1024)
            .height(768)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Ensemble feature measure")
    val chart = XYChart(chartBuilder)
    drawSemiSphere(chart)
    drawAxisX(chart)
    drawAxisY(chart)
    val xDataForFeatures = angles.map { cos(it) }
    features.forEachIndexed { index, doubles ->
        val yDataForFeature = doubles.zip(angles)
                .map { (d, angle) ->
                    sin(angle) * d // y coord by definition of sinus
                }
        chart.addSeries("Feature $index", xDataForFeatures, yDataForFeature)
                .apply {
                    this.marker = None()
                    this.lineStyle = BasicStroke(1.0f)
                }
    }
    val cuttingLineDataToDraw = cuttingLineY.zip(angles)
            .map { (d, angle) ->
                sin(angle) * d // y coord by definition of sinus
            }
    chart.addSeries("Bottom front", xDataForFeatures, cuttingLineDataToDraw).apply {
        this.marker = None()
        this.lineColor = Color.BLACK
        this.lineStyle = BasicStroke(3.0f)

    }
    drawChart(chart)
}

private fun processAllPointsWithEnrichment(startingEpsilon: Int): PointProcessingFinalResult2d {
    var prevEpsilon = startingEpsilon
    var prevPositions = (0..prevEpsilon).toSortedSet()
    var prevAngles = prevPositions.map { getAngle(prevEpsilon, it) }
    var (evaluatedData, cuttingLineY, cutsForAllPoints, currCutChangePositions) = processAllPoints(prevAngles)
    var prevCutChangePositions: List<Int>

    // after enrichment
    do {
        prevCutChangePositions = currCutChangePositions

        val newEpsilon = prevEpsilon * 10
        val newPositions = prevPositions.map { it * 10 }.toSortedSet()
        prevCutChangePositions.forEach {
            val prev = it - 1
            newPositions.addAll((prev * 10)..(it * 10))
        }
        val newAngles = newPositions.map { getAngle(newEpsilon, it) }
        val newResult = processAllPoints(newAngles)

        evaluatedData = newResult.evaluatedData
        cuttingLineY = newResult.cuttingLineY
        cutsForAllPoints = newResult.cutsForAllPoints
        currCutChangePositions = newResult.cutChangePositions
        prevEpsilon = newEpsilon
        prevPositions = newPositions
        prevAngles = newAngles
    } while (currCutChangePositions.size != prevCutChangePositions.size)

    val pointsToTry = currCutChangePositions
            .map {
                val angle = getAngle(prevEpsilon, it)
                getPointOnUnitSphere(angle)
            }

    return PointProcessingFinalResult2d(
            evaluatedData,
            cuttingLineY,
            cutsForAllPoints,
            pointsToTry,
            prevAngles
    )
}

private fun processAllPoints(angles: List<Double>): PointProcessingResult2d {
    // begin first stage (before enrichment)
    logToConsole("Started the processing")
    val pointsInProjectiveCoords = angles.map { getPointOnUnitSphere(it) }
    logToConsole("${pointsInProjectiveCoords.size} points to calculate measures on")
    val (evaluatedData, cuttingLineY, cutsForAllPointsRaw) = processAllPointsFast(pointsInProjectiveCoords, dataSet, measures, cutSize)
    val cutsForAllPoints = cutsForAllPointsRaw.map { it.toSet() }
    logToConsole("Evaluated data, calculated cutting line and cuts for all points")

    val cutChangePositions = cutsForAllPoints
            .mapIndexed { index, cut -> Pair(index, cut) }
            .filter { it.first != 0 && it.second != cutsForAllPoints[it.first - 1] }
            .map { it.first }

    logToConsole("Found ${cutChangePositions.size} points to try")

    // end first stage (before enrichment)
    return PointProcessingResult2d(evaluatedData, cuttingLineY, cutsForAllPoints, cutChangePositions)
}

private fun drawAxisY(chart: XYChart) {
    chart.addSeries("Y", doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 1.05)).apply {
        this.lineColor = Color.BLACK
        this.marker = None()
        this.lineStyle = BasicStroke(2.0f)
    }
}

private fun drawAxisX(chart: XYChart) {
    chart.addSeries("X", doubleArrayOf(-1.05, 1.05), doubleArrayOf(0.0, 0.0)).apply {
        this.lineColor = Color.BLACK
        this.marker = None()
        this.lineStyle = BasicStroke(2.0f)
    }
}

private fun drawSemiSphere(chart: XYChart) {
    val epsilon = 1000
    val semiSphereXData = 0.rangeTo(epsilon).map { -1 + (2 * it).toDouble() / epsilon }
    chart.addSeries("Sphere",
            semiSphereXData,
            semiSphereXData.map { Math.sqrt(1 - it.pow(2)) }
    ).apply {
        this.lineColor = Color.BLACK
        this.marker = None()
        this.lineStyle = BasicStroke(3.0f)
    }
}

private fun getFeaturesToDraw(cutsForAllPoints: List<Set<Int>>, evaluatedData: List<DoubleArray>): List<DoubleArray> {
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

    return needToProcess.map { feature(it) }
}

private fun drawChart(chart: XYChart) {
    logToConsole("Finished calculations; visualizing...")
    SwingWrapper(chart).displayChart()
    logToConsole("Finished visualization")

    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}