package ru.ifmo.ctddev.isaev

import org.ejml.simple.SimpleMatrix
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
import java.util.*
import kotlin.collections.ArrayList


/**
 * @author iisaev
 */
val random = Random()
val colorMaps = listOf(
        ColorMapRainbow(),
        ColorMapWhiteGreen(),
        ColorMapGrayscale(),
        ColorMapHotCold(),
        ColorMapRBG(),
        ColorMapRedAndGreen(),
        ColorMapWhiteBlue(),
        ColorMapWhiteRed()
)

fun randomColor() = colorMaps[random.nextInt(colorMaps.size)]

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    val measures = listOf(VDM::class, SpearmanRankCorrelation::class, SymmetricUncertainty::class)
    val featuresToProcess = 0.rangeTo(50).toSet()//setOf(0, 5, 9)
    val xyData = listOf(Point(1.0, 0.0, 0.0), Point(0.0, 1.0, 0.0), Point(0.0, 0.0, 1.0))
    val data = getEvaluatedData(xyData, dataSet, measures, featuresToProcess)
    val planes = calculatePlanes(data)
    val allIntersections = 0.until(planes.size).flatMap { i ->
        0.until(i).mapNotNull { j -> planes[i].intersect(planes[j]) }
    }
    println("Found ${allIntersections.size} intersections")
    val intersections = allIntersections
    val pointsToTry = intersections.flatMap { calculatePoints(it) }
    println("Found ${pointsToTry.size} points to try from ${intersections.size} intersections")
    //val n = 100
    AnalysisLauncher.open(PlanesAndIntersectionsScene(planes, intersections))
    AnalysisLauncher.open(PointsScene(pointsToTry))
}

fun calculatePlanes(data: List<List<Double>>): List<Plane> {
    return 0.until(data[0].size)
            .map {
                Plane(
                        Point3d(1.0, 0.0, data[0][it]),
                        Point3d(0.0, 1.0, data[1][it]),
                        Point3d(0.0, 0.0, data[2][it])
                )
            }
}

class PlanesAndIntersectionsScene(private val planes: List<Plane>,
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

class PointsScene(private val pointsToTry: List<Point3d>) : AbstractAnalysis() {

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

private fun calculatePoints(it: Intersection3d): List<Point3d> {
    val p1 = it.line.p1
    val p2 = it.line.p2
    val xMin = Math.min(p1.x, p2.x)
    val xMax = Math.max(p1.x, p2.x)
    val yMin = Math.min(p1.y, p2.y)
    val yMax = Math.max(p1.y, p2.y)
    val zMin = Math.min(p1.z, p2.z)
    val zMax = Math.max(p1.z, p2.z)
    val beta = yMax - yMin
    val gamma = zMax - zMin
    var x = xMin
    val result = ArrayList<Point3d>()
    val len = xMax - xMin
    while (x <= xMax) {
        val t = (x - xMin) / len
        val y = yMin + t * beta
        val z = zMin + t * gamma
        // result.add(Point(x, y, 1 - x - y))
        result.add(Point3d(x, y, z))
        x += 0.01
    }
    return result
}

class Point3d(val x: Double, val y: Double, val z: Double)

class Line3d(val p1: Point3d, val p2: Point3d)

class Intersection3d(val plane1: Plane, val plane2: Plane, val line: Line3d)

class Plane {
    private val a: Double
    private val b: Double
    private val c: Double
    private val d: Double

    constructor(a: Double, b: Double, c: Double, d: Double) {
        this.a = a
        this.b = b
        this.c = c
        this.d = d
    }

    constructor(p1: Point3d, p2: Point3d, p3: Point3d) {
        val d1 = SimpleMatrix(arrayOf(
                doubleArrayOf(p2.y - p1.y, p2.z - p1.z),
                doubleArrayOf(p3.y - p1.y, p3.z - p1.z)
        )).determinant()
        val d2 = SimpleMatrix(arrayOf(
                doubleArrayOf(p2.x - p1.x, p2.z - p1.z),
                doubleArrayOf(p3.x - p1.x, p3.z - p1.z)
        )).determinant()
        val d3 = SimpleMatrix(arrayOf(
                doubleArrayOf(p2.x - p1.x, p2.y - p1.y),
                doubleArrayOf(p3.x - p1.x, p3.y - p1.y)
        )).determinant()
        this.a = d1
        this.b = -d2
        this.c = d3
        this.d = d2 * p1.y - d1 * p1.x - d3 * p1.z
    }

    fun substitute(x: Double, y: Double) = (a * x + b * y + d) / (-c)

    fun contains(p: Point3d): Boolean {
        val ans = a * p.x + b * p.y + c * p.z + d
        return ans roughlyEqualTo 0.0
    }

    fun intersect(second: Plane): Intersection3d? {
        if (a / second.a roughlyEqualTo b / second.b && b / second.b roughlyEqualTo c / second.c) {
            return null
        }
        val validRange = 0.0.rangeTo(1.0)
        val intersections = listOf(
                getIntersectionWithFixedX(0.0, second),
                getIntersectionWithFixedX(1.0, second),
                getIntersectionWithFixedY(0.0, second),
                getIntersectionWithFixedY(1.0, second),
                getIntersectionWithFixedZ(0.0, second),
                getIntersectionWithFixedZ(1.0, second)
        )
                .onEach { assert(contains(it)) }
                .filter { validRange.contains(it.x) && validRange.contains(it.y) && validRange.contains(it.z) }
        return if (intersections.size < 2) {
            null
        } else {
            Intersection3d(this, second, Line3d(intersections[0], intersections[1]))
        }
    }

    private fun getIntersectionWithFixedY(y: Double, second: Plane): Point3d {
        // z == kx + l
        val k = a / (-c)
        val l = (b * y + d) / (-c)
        val x = (second.c * l + second.b * y + second.d) / -(second.a + second.c * k)
        val z = (a * x + b * y + d) / (-c)
        return Point3d(x, y, z)
    }

    private fun getIntersectionWithFixedX(x: Double, second: Plane): Point3d {
        // z == ky + l
        val k = b / (-c)
        val l = (a * x + d) / (-c)
        val y = (second.c * l + second.a * x + second.d) / -(second.b + second.c * k)
        val z = (b * y + a * x + d) / (-c)
        return Point3d(x, y, z)
    }

    private fun getIntersectionWithFixedZ(z: Double, second: Plane): Point3d {
        // y == kx+l
        val k = a / (-b)
        val l = (c * z + d) / (-b)
        val x = (second.b * l + second.c * z + second.d) / -(second.a + second.b * k)
        val y = (a * x + c * z + d) / (-b)
        return Point3d(x, y, z)
    }

}

private infix fun Double.roughlyEqualTo(d: Double): Boolean = Math.abs(this - d) < 0.001
