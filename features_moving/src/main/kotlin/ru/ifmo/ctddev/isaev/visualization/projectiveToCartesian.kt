package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.Chart
import org.jzy3d.chart.controllers.keyboard.camera.AWTCameraKeyController
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapWhiteBlue
import org.jzy3d.maths.*
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.primitives.AbstractDrawable
import org.jzy3d.plot3d.primitives.LineStrip
import org.jzy3d.plot3d.primitives.Point
import org.jzy3d.plot3d.primitives.textured.DrawableTexture
import org.jzy3d.plot3d.rendering.canvas.Quality
import org.jzy3d.plot3d.rendering.scene.Graph
import org.jzy3d.plot3d.rendering.textures.BufferedImageTexture
import org.jzy3d.plot3d.text.drawable.DrawableTextTexture
import org.jzy3d.plot3d.text.drawable.TextImageRenderer
import ru.ifmo.ctddev.isaev.space.getPointOnUnitSphere
import java.awt.Font
import java.awt.event.KeyEvent
import kotlin.math.sqrt


/**
 * @author iisaev
 */

private var theta = Math.PI / 4

private var phi = Math.PI / 3

private var drawablesByPoint = emptyList<AbstractDrawable>()

fun main(args: Array<String>) {
    val scene = ProjectiveToEuclidean()
    AnalysisLauncher.open(scene, Rectangle(0, 0, 1024, 768))
    val chart = scene.chart
    chart.addController(object : AWTCameraKeyController() {
        override fun keyPressed(e: KeyEvent) {
            if (e.isShiftDown) {
                when (e.keyCode) {
                    KeyEvent.VK_LEFT -> theta -= 0.01
                    KeyEvent.VK_RIGHT -> theta += 0.01
                    KeyEvent.VK_UP -> phi -= 0.01
                    KeyEvent.VK_DOWN -> phi += 0.01
                }
                updateDrawables(chart)
            }
        }
    })
}

private fun updateDrawables(chart: Chart) {
    drawablesByPoint.forEach { chart.removeDrawable(it, false) }
    drawablesByPoint = getDrawablesByPoint(theta, phi)
    chart.addDrawable(drawablesByPoint, false)
    chart.view.setBoundManual(BoundingBox3d(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f))
}

private fun m(mapper: (Double, Double) -> Double): Mapper {
    return object : Mapper() {
        override fun f(x: Double, y: Double): Double {
            return mapper(x, y)
        }
    }
}

private class ProjectiveToEuclidean : AbstractAnalysis() {

    override fun hasOwnChartControllers(): Boolean {
        return true
    }

    override fun init() {

        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType())
        val graph = chart.scene.graph
        drawBasics(graph)
        updateDrawables(chart)
    }

    private fun drawBasics(graph: Graph) {
        val semiSphere = Builder.buildOrthonormal(m { x, y -> sqrt(1 - x * x - y * y) }, Range(-1f, 1f), 200)
                .apply {
                    colorMapper = ColorMapper(ColorMapWhiteBlue(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .2f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        val xLine = getLineFromOrigin(Coord3d(1.1, 0.0, 0.0), Color.BLACK, 4.0)
        val yLine = getLineFromOrigin(Coord3d(0.0, 1.1, 0.0), Color.BLACK, 4.0)
        val zLine = getLineFromOrigin(Coord3d(0.0, 0.0, 1.1), Color.BLACK, 4.0)
        graph.add(xLine)
        graph.add(yLine)
        graph.add(zLine)
        graph.add(semiSphere)
    }

}


private fun getDrawablesByPoint(theta: Double, phi: Double): List<AbstractDrawable> {
    val cartesian = getPointOnUnitSphere(doubleArrayOf(theta, phi))
    val pointOnSphere = Coord3d(cartesian.coordinates[0], cartesian.coordinates[1], cartesian.coordinates[2])
    println("Current coordinates: phi = $phi, theta = $theta conversion from [$theta, $phi] to $cartesian")
    val pointOnX = Coord3d(pointOnSphere.x, 0.0f, 0.0f)
    val pointOnY = Coord3d(0.0f, pointOnSphere.y, 0.0f)
    val pointOnZ = Coord3d(0.0f, 0.0f, pointOnSphere.z)
    val lineFromOrigin = getLineFromOrigin(pointOnSphere, Color.RED, 3.0)
    
    fun makeMapping(dim: Coord2d): List<Coord2d> {
        return DrawableTexture.getManualTextureMapping(dim.x, dim.y, dim.x / 2.0f, dim.y / 2.0f)
    }

    val xDescr = DrawableTextTexture(makeImage(pointOnSphere.x), PlaneAxis.X, pointOnSphere.x, makeMapping(Coord2d(0.1f, 0.1f)), Color.BLACK).apply{
        
    }

    val yDescr = DrawableTextTexture(makeImage(pointOnSphere.y), PlaneAxis.Y, pointOnSphere.y, makeMapping(Coord2d(0.1f, 0.1f)), Color.BLACK).apply{

    }

    val zDescr = DrawableTextTexture(makeImage(pointOnSphere.z), PlaneAxis.Z, pointOnSphere.z, makeMapping(Coord2d(0.1f, 0.1f)), Color.BLACK).apply{

    }

    return listOf(
            lineFromOrigin,
            getLine(pointOnSphere, pointOnX, Color.RED, 1.0),
            xDescr,
            getLine(pointOnSphere, pointOnY, Color.RED, 1.0),
            yDescr,
            getLine(pointOnSphere, pointOnZ, Color.RED, 1.0),
            zDescr
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

fun makeImage(text: Number): BufferedImageTexture {
    val font = Font("Serif", Font.BOLD, 12)
    return TextImageRenderer(text.toString(), font).image
}