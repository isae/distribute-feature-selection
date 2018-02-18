package ru.ifmo.ctddev.isaev

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
    val xyData = intValues.map {
        val c1 = it.first.toDouble() / 100
        val c2 = it.second.toDouble() / 100
        Point(c1, c2, 1 - c1 - c2)
    }
    //val xyData = listOf(Point(1.0, 0.0, 0.0), Point(0.0, 1.0, 0.0), Point(0.0, 0.0, 1.0))
    println("Found ${xyData.size} points")
    val data = getEvaluatedData(xyData, dataSet, measures, 0)
    val forDraw = intValues.zip(data).toMap()
    AnalysisLauncher.open(FeatureMoving3d(forDraw))
}

class FeatureMoving3d(val forDraw: Map<Pair<Int, Int>, Double>) : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(0f, 1f)
        val steps = 80

        // Create the object to represent the function over the given range.
        val surface = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
            override fun f(xd: Double, yd: Double): Double {
                val x = (xd * 100).toInt()
                val y = (yd * 100).toInt()
                val res = forDraw[Pair(x, y)]
                val revRes = forDraw[Pair(100 - x, 100 - y)]
                if (res != null) {
                    return res
                }
                if (revRes != null) {
                    return 10 - revRes
                }
                println("null :( $x $y")
                return -1000.0
            }
        })
                .apply {
                    colorMapper = ColorMapper(ColorMapRainbow(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Fastest, getCanvasType())
        chart.scene.graph.add(surface)
    }
}