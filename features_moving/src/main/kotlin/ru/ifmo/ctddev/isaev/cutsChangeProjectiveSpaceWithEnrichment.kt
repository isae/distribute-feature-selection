package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.None
import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.space.calculateAllPointsWithEnrichment2d
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import ru.ifmo.ctddev.isaev.space.logToConsole
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

fun main(args: Array<String>) {
    val (evaluatedData, cuttingLineY, cutsForAllPoints, cutChangePositions, pointsToTry, angles) =
            calculateAllPointsWithEnrichment2d(100, dataSet, measures, cutSize, 2)
    println("Found ${pointsToTry.size} points to try with enrichment")
    println(cutChangePositions)
    pointsToTry.forEach { println(it) }

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

private fun getFeaturesToDraw(cutsForAllPoints: List<RoaringBitmap>, evaluatedData: List<DoubleArray>): List<DoubleArray> {
    val sometimesInCut = cutsForAllPoints
            .flatMap { it }
            .toSet()
    logToConsole { "Sometimes in cut: ${sometimesInCut.size} features" }
    val alwaysInCut = sometimesInCut.filter { featureNum ->
        cutsForAllPoints.all { it.contains(featureNum) }
    }
    logToConsole { "Always in cut: ${alwaysInCut.size} features: $alwaysInCut" }
    val needToProcess = sometimesInCut - alwaysInCut
    logToConsole { "Need to process: ${needToProcess.size} features" }

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    return needToProcess.map { feature(it) }
}

private fun drawChart(chart: XYChart) {
    logToConsole { "Finished calculations; visualizing..." }
    SwingWrapper(chart).displayChart()
    logToConsole { "Finished visualization" }

    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}