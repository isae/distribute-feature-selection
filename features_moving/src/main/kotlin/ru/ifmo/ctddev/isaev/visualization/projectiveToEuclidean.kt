package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapWhiteGreen
import org.jzy3d.maths.Coord3d
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.primitives.LineStrip
import org.jzy3d.plot3d.primitives.Point
import org.jzy3d.plot3d.rendering.canvas.Quality
import kotlin.math.sqrt


/**
 * @author iisaev
 */

// Define range and precision for the function to plot
val origin = Point(Coord3d(0.0, 0.0, 0.0))


fun main(args: Array<String>) {
    AnalysisLauncher.open(ProjectiveToEuclidean())
}

fun m(mapper: (Double, Double) -> Double): Mapper {
    return object : Mapper() {
        override fun f(x: Double, y: Double): Double {
            return mapper(x, y)
        }
    }
}

private class ProjectiveToEuclidean : AbstractAnalysis() {

    override fun init() {

        // Create the object to represent the function over the given range.
        /* val surface1 = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), m { x, y -> x + y })
                 .apply {
                     colorMapper = ColorMapper(ColorMapRainbow(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                     faceDisplayed = true
                     wireframeDisplayed = false
                 }
                 */

        val semiSphere = Builder.buildOrthonormal(m { x, y -> sqrt(1 - x * x - y * y) }, Range(-1f, 1f), 50)
                .apply {
                    colorMapper = ColorMapper(ColorMapWhiteGreen(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        val xLine = LineStrip(origin, Point(Coord3d(1.1, 0.0, 0.0))).apply {
            wireframeColor = Color.BLACK
        }
        xLine.setWidth(4.0f)
        val yLine = LineStrip(origin, Point(Coord3d(0.0, 1.1, 0.0))).apply {
            wireframeColor = Color.BLACK
        }
        yLine.setWidth(4.0f)
        val zLine = LineStrip(origin, Point(Coord3d(0.0, 0.0, 1.1))).apply {
            wireframeColor = Color.BLACK
        }
        zLine.setWidth(4.0f)


        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType())
        //chart.scene.graph.add(surface1)
        //chart.scene.graph.add(semiSphere)
        chart.scene.graph.add(xLine)
        chart.scene.graph.add(yLine)
        chart.scene.graph.add(zLine)
        chart.scene.graph.add(semiSphere)
    }
}

