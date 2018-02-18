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
    val featuresToProcess = setOf(0, 5, 9)
    val xyData = listOf(Point(1.0, 0.0, 0.0), Point(0.0, 1.0, 0.0), Point(0.0, 0.0, 1.0))
    val data = getEvaluatedData(xyData, dataSet, measures, featuresToProcess)
    val planes = calculatePlanes(data)
    //val n = 100
    AnalysisLauncher.open(FeatureMoving3d2(planes))
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

class FeatureMoving3d2(val planes: List<Plane>) : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(0f, 1f)
        val steps = 100

        // Create the object to represent the function over the given range.

        chart = AWTChartComponentFactory.chart(Quality.Intermediate, getCanvasType())
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
        val intersections = 0.until(planes.size).flatMap { i ->
            0.until(i).mapNotNull { j -> planes[i].intersect(planes[j]) }
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
        val point1 = getIntersectionWithZ(0.0, second)
        assert(contains(point1))
        assert(second.contains(point1))
        val point2 = getIntersectionWithZ(1.0, second)
        assert(contains(point2))
        assert(second.contains(point1))
        return Intersection3d(this, second, Line3d(point1, point2))
    }

    private fun getIntersectionWithZ(z: Double, second: Plane): Point3d {
        val (k, l) = getYCoeffWithZ(z) // considering z = 0
        val x = (second.b * l + second.c * z + second.d) / -(second.a + second.b * k)
        val y = substituteXAndC(x, z)
        return Point3d(x, y, z)
    }

    private fun substituteXAndC(x: Double, z: Double): Double {
        return (a * x + c * z + d) / (-b)
    }

    private fun getYCoeffWithZ(z: Double): Pair<Double, Double> {
        return Pair(a / (-b), (c * z + d) / (-b))
    }
}

private infix fun Double.roughlyEqualTo(d: Double): Boolean = Math.abs(this - d) < 0.001
