package ru.ifmo.ctddev.isaev

import org.ejml.simple.SimpleMatrix
import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRainbow
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.rendering.canvas.Quality
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.getEvaluatedData


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    val measures = listOf(VDM::class, SpearmanRankCorrelation::class, SymmetricUncertainty::class)
    //val n = 100
    val intValues = 0.rangeTo(100)
            .flatMap { x ->
                0.rangeTo(100 - x)
                        .map { y -> Pair(x, y) }
            }
    /*val xyData = intValues.map {
        val c1 = it.first.toDouble() / 100
        val c2 = it.second.toDouble() / 100
        Point(c1, c2, 1 - c1 - c2)
    }*/
    val xyData = listOf(Point(1.0, 0.0, 0.0), Point(0.0, 1.0, 0.0), Point(0.0, 0.0, 1.0))
    println("Found ${xyData.size} points")
    val data = getEvaluatedData(xyData, dataSet, measures, 0)
    val forDraw = intValues.zip(data).toMap()
    val element = Plane(
            Point3d(0.5, 2.0, -1.0), 
            Point3d(3.0, 1.0, 2.0),
            Point3d(0.0, -1.0, 1.0)
    )
    val z1 = element.substitute(0.5, 2.0)
    val z2 = element.substitute(3.0, 1.0)
    val z3 = element.substitute(0.0, -1.0)
    AnalysisLauncher.open(FeatureMoving3d2(listOf(
            element
    )))
}

class FeatureMoving3d2(val planes: List<Plane>) : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(0f, 1f)
        val steps = 80

        // Create the object to represent the function over the given range.

        chart = AWTChartComponentFactory.chart(Quality.Fastest, getCanvasType())
        planes.forEach {
            val surface = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
                override fun f(x: Double, y: Double): Double {
                    return it.substitute(x, y)
                }
            })
                    .apply {
                        colorMapper = ColorMapper(ColorMapRainbow(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                        faceDisplayed = true
                        wireframeDisplayed = false
                    }

            // Create a chart
            chart.scene.graph.add(surface)
        }

    }
}

class Point3d(val x: Double, val y: Double, val z: Double)

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
}