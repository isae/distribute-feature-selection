package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapWhiteBlue
import org.jzy3d.maths.BoundingBox3d
import org.jzy3d.maths.Coord3d
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.primitives.LineStrip
import org.jzy3d.plot3d.primitives.Point
import org.jzy3d.plot3d.rendering.canvas.Quality
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


/**
 * @author iisaev
 */

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

        val semiSphere = Builder.buildOrthonormal(m { x, y -> sqrt(1 - x * x - y * y) }, Range(-1f, 1f), 200)
                .apply {
                    colorMapper = ColorMapper(ColorMapWhiteBlue(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .2f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        val xLine = getLineFromOrigin(Coord3d(1.1, 0.0, 0.0), Color.BLACK, 4.0)
        val yLine = getLineFromOrigin(Coord3d(0.0, 1.1, 0.0), Color.BLACK, 4.0)
        val zLine = getLineFromOrigin(Coord3d(0.0, 0.0, 1.1), Color.BLACK, 4.0)

        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType())
        val graph = chart.scene.graph
        //chart.scene.graph.add(surface1)
        //chart.scene.graph.add(semiSphere)
        graph.add(xLine)
        graph.add(yLine)
        graph.add(zLine)
        graph.add(semiSphere)

        val theta = Math.PI / 4

        val phi = Math.PI / 3

        val pointOnSphere = getPointOnSphere(phi, theta)
        val pointOnX = Coord3d(pointOnSphere.x, 0.0f, 0.0f)
        val pointOnY = Coord3d(0.0f, pointOnSphere.y, 0.0f)
        val pointOnZ = Coord3d(0.0f, 0.0f, pointOnSphere.z)
        val lineFromOrigin = getLineFromOrigin(pointOnSphere, Color.RED, 3.0)
        graph.add(lineFromOrigin)
        graph.add(getLine(pointOnSphere, pointOnX, Color.RED, 1.0))
        graph.add(getLine(pointOnSphere, pointOnY, Color.RED, 1.0))
        graph.add(getLine(pointOnSphere, pointOnZ, Color.RED, 1.0))

        chart.view.setBoundManual(BoundingBox3d(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f))

    }

    private fun getPointOnSphere(phi: Double, theta: Double): Coord3d {
        return Coord3d(
                cos(theta) * sin(phi),
                sin(theta) * sin(phi),
                cos(phi)
        )
    }


    private fun getLineFromOrigin(coord: Coord3d, color: Color, width: Double): LineStrip {
        return getLine(Coord3d.ORIGIN, coord, color, width)
    }

    private fun getLine(from: Coord3d, to: Coord3d, color: Color, width: Double): LineStrip {
        val result = LineStrip(Point(from), Point(to))
                .apply {
                    wireframeColor = color
                }
        result.setWidth(width.toFloat())
        return result
    }

}

