package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.None
import org.locationtech.jts.geom.*
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex
import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.calculateAllPointsWithEnrichment2d
import ru.ifmo.ctddev.isaev.space.evaluateDataSet
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
private val evaluatedDs = evaluateDataSet(dataSet, measures)

/* val geomFactory = GeometryFactory()
  val trgBuilder = DelaunayTriangulationBuilder()
  trgBuilder.setSites(arrayListOf(
          Coordinate(0.0, 0.0),
          Coordinate(0.0, 1.0),
          Coordinate(1.0, 0.0),
          Coordinate(1.0, 1.0)
  ))
  val triangles = trgBuilder.getTriangles(geomFactory)*/
fun main(args: Array<String>) {
    val envelope = Envelope(Coordinate(-1.0, -1.0), Coordinate(2.0, 2.0))
    val subDiv = QuadEdgeSubdivision(envelope, 0.000001)
    val incDelaunay = IncrementalDelaunayTriangulator(subDiv)
    incDelaunay.insertSite(Vertex(0.0, 0.0))
    incDelaunay.insertSite(Vertex(0.0, 1.0))
    incDelaunay.insertSite(Vertex(1.0, 0.0))
    incDelaunay.insertSite(Vertex(1.0, 1.0))
    val cache = HashMap<Point, RoaringBitmap>()
    do {
        val smthChanged = performEnrichment(incDelaunay)
    } while (smthChanged)
    val geomFact = GeometryFactory()
    val trianglesGeom = subDiv.getTriangles(geomFact) as GeometryCollection
    val allTriangles = 0.until(trianglesGeom.numGeometries)
            .map { trianglesGeom.getGeometryN(it) }
            .map { Triangle(it.coordinates[0], it.coordinates[1], it.coordinates[2]) }
    val f = true

    val (evaluatedData, cutsForAllPoints, cutChangePositions, pointsToTry, angles, lastFeatureInAllCuts) =
            calculateAllPointsWithEnrichment2d(100, evaluatedDs, cutSize, 2)
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
    //TODO restore cutting line data from other sources (value of last cut feature measure in each point)
    val cuttingLineDataToDraw = lastFeatureInAllCuts.mapIndexed { i, f ->
        val angle = angles[i]
        val d = evaluatedData[i][f]
        sin(angle) * d // y coord by definition of sinus
    }
    chart.addSeries("Bottom front", xDataForFeatures, cuttingLineDataToDraw).apply {
        this.marker = None()
        this.lineColor = Color.BLACK
        this.lineStyle = BasicStroke(3.0f)

    }
    drawChart(chart)
}

fun performEnrichment(delaunay: IncrementalDelaunayTriangulator): Boolean {
    return false
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