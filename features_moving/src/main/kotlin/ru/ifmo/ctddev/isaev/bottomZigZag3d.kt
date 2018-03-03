package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.*
import org.jzy3d.maths.Coord3d
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.primitives.LineStrip
import org.jzy3d.plot3d.rendering.canvas.Quality
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.getEvaluatedData
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import java.util.*


/**
 * @author iisaev
 */
private val random = Random()
private val colorMaps = listOf(
        ColorMapRainbow(),
        ColorMapWhiteGreen(),
        ColorMapGrayscale(),
        ColorMapHotCold(),
        ColorMapRBG(),
        ColorMapRedAndGreen(),
        ColorMapWhiteBlue(),
        ColorMapWhiteRed()
)

private fun randomColor() = colorMaps[random.nextInt(colorMaps.size)]

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    val measures = listOf(VDM::class, SpearmanRankCorrelation::class, SymmetricUncertainty::class)
    val intValues = 0.rangeTo(100)
            .flatMap { x ->
                0.rangeTo(100 - x)
                        .map { y -> Pair(x, y) }
            }
    val xyData = intValues.map {
        val c1 = it.first.toDouble() / 100
        val c2 = it.second.toDouble() / 100
        Point(c1, c2, 1 - c1 - c2)
    }
    logToConsole("Found ${xyData.size} points")
    val evaluatedData = getEvaluatedData(xyData, dataSet, measures)
    logToConsole("Evaluated all data")
    val range = Array(evaluatedData[0].size, { it })
    val evaluatedDataWithNumbers = evaluatedData.map { Pair(it, range.clone()) }
    logToConsole("Indexed all data")
    val cutSize = 50
    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map {
                val featureNumbers = it.second
                val featureMeasures = it.first
                val comparator = kotlin.Comparator<Int> { o1, o2 -> compareValuesBy(o1, o2, { -featureMeasures[it] }) }
                Arrays.sort(featureNumbers, comparator)
                return@map Pair(featureMeasures, featureNumbers)
            } //max first
            .map { it.second.take(cutSize) }
    logToConsole("Calculated cuts")
    //val cuttingLineY = rawCutsForAllPoints.map { it.first.last() }
    val cutsForAllPoints = rawCutsForAllPoints
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

    val features = needToProcess.take(10).map { feature(it) }
    val planes = calculatePlanes(features)
    /* val allIntersections = 0.until(planes.size).flatMap { i ->
         0.until(i).mapNotNull { j -> planes[i].intersect(planes[j]) }
     }*/
    val allIntersections = emptyList<Intersection3d>()
    println("Found ${allIntersections.size} intersections")
    val intersections = allIntersections
    val pointsToTry = intersections.flatMap { calculatePoints(it) }
    println("Found ${pointsToTry.size} points to try from ${intersections.size} intersections")
    //val n = 100
    AnalysisLauncher.open(Planes(planes, intersections))
    AnalysisLauncher.open(Points(pointsToTry))
}

private class Planes(private val planes: List<Plane>,
                     private val intersections: List<Intersection3d>) : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(0f, 1f)
        val steps = 100

        // Create the object to represent the function over the given range.

        chart = AWTChartComponentFactory.chart(Quality.Fastest, getCanvasType())
        planes.forEach {
            val surface = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
                override fun f(x: Double, y: Double): Double {
                    return it.substitute(x, y)
                }
            })
                    .apply {
                        colorMapper = ColorMapper(randomColor(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                        faceDisplayed = true
                        wireframeDisplayed = false
                    }

            // Create a chart
            chart.scene.graph.add(surface)
        }

        intersections.forEach({
            val p1 = it.line.p1
            val p2 = it.line.p2
            val lineStrip = LineStrip(
                    org.jzy3d.plot3d.primitives.Point(Coord3d(p1.x, p1.y, p1.z)),
                    org.jzy3d.plot3d.primitives.Point(Coord3d(p2.x, p2.y, p2.z))
            )
            lineStrip.setWidth(8f)
            lineStrip.wireframeColor = Color.BLACK
            chart.scene.graph.add(lineStrip)
        })
        chart.axeLayout.xAxeLabel = "VDM weight"
        chart.axeLayout.yAxeLabel = "SpearmanRankCorrelation weight"
        chart.axeLayout.zAxeLabel = "Ensemble measure"
    }
}

private class Points(private val pointsToTry: List<Point3d>) : AbstractAnalysis() {

    override fun init() {
        chart = AWTChartComponentFactory.chart(Quality.Fastest, getCanvasType())
        pointsToTry.forEach {
            val pointToDisplay = org.jzy3d.plot3d.primitives.Point(Coord3d(it.x, it.y, it.z))
            pointToDisplay.setWidth(5f)
            pointToDisplay.color = Color.GREEN
            pointToDisplay.boundingBoxColor = Color.BLUE
            chart.scene.graph.add(pointToDisplay)
        }
        chart.axeLayout.xAxeLabel = "VDM weight"
        chart.axeLayout.yAxeLabel = "SpearmanRankCorrelation weight"
        chart.axeLayout.zAxeLabel = "Ensemble measure"
    }
}