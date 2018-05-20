package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.chart.Chart
import org.jzy3d.chart.ChartLauncher.configureControllers
import org.jzy3d.chart.Settings
import org.jzy3d.chart.controllers.keyboard.camera.AWTCameraKeyController
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapWhiteBlue
import org.jzy3d.maths.BoundingBox3d
import org.jzy3d.maths.Coord3d
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.primitives.AbstractDrawable
import org.jzy3d.plot3d.primitives.LineStrip
import org.jzy3d.plot3d.primitives.Point
import org.jzy3d.plot3d.rendering.canvas.Quality
import org.jzy3d.plot3d.rendering.scene.Graph
import org.jzy3d.ui.MultiChartPanel
import ru.ifmo.ctddev.isaev.space.getPointOnUnitSphere
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.KeyEvent
import java.text.DecimalFormat
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import kotlin.math.sqrt


/**
 * @author iisaev
 */

private var theta = Math.PI / 4

private var phi = Math.PI / 3

private var drawablesByPoint = emptyList<AbstractDrawable>()

private val decimal = DecimalFormat("0.0000")

val xTextArea = JTextArea("").apply {
    isEditable = false
}
val yTextArea = JTextArea("").apply {
    isEditable = false
}
val zTextArea = JTextArea("").apply {
    isEditable = false
}
val thetaTextArea = JTextArea("").apply {
    isEditable = false
}
val phiTextArea = JTextArea("").apply {
    isEditable = false
}

fun main(args: Array<String>) {
    Settings.getInstance().isHardwareAccelerated = true
    val scene = ProjectiveToEuclidean()
    //AnalysisLauncher.open(scene, Rectangle(0, 0, 1024, 768))
    scene.init()
    val chart = scene.chart
    val mouse = configureControllers(chart, "ProjectiveToEuclidean", true, false)
    chart.render()
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
    val multiChart = MultiChartPanel(listOf(chart), false, 1150, 800).apply {
        addPanel(JPanel(FlowLayout(FlowLayout.CENTER, 0, 0), true).apply {
            add(
                    JPanel(GridLayout(5, 1), true).apply {
                        add(JLabel("X:"))
                        add(JLabel("Y:"))
                        add(JLabel("Z:"))
                        add(JLabel("Theta:"))
                        add(JLabel("Phi:"))
                    }
            )
            add(
                    JPanel(GridLayout(5, 1), true).apply {
                        add(xTextArea)
                        add(yTextArea)
                        add(zTextArea)
                        add(thetaTextArea)
                        add(phiTextArea)
                    }
            )
        })
    }
    JFrame().apply {
        title = "ProjectiveToEuclidean"
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        add(multiChart)
        pack()
        isVisible = true
    }
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
    //println("Current coordinates: phi = $phi, theta = $theta conversion from [$theta, $phi] to $cartesian")
    val pointOnX = Coord3d(pointOnSphere.x, 0.0f, 0.0f)
    val pointOnY = Coord3d(0.0f, pointOnSphere.y, 0.0f)
    val pointOnZ = Coord3d(0.0f, 0.0f, pointOnSphere.z)
    val projectionOnXY = Coord3d(pointOnSphere.x, pointOnSphere.y, 0.0f)
    xTextArea.text = decimal.format(pointOnSphere.x)
    yTextArea.text = decimal.format(pointOnSphere.y)
    zTextArea.text = decimal.format(pointOnSphere.z)
    phiTextArea.text = decimal.format(phi)
    thetaTextArea.text = decimal.format(theta)
    return listOf(
            getLineFromOrigin(pointOnSphere, Color.RED, 4.0),
            getLine(pointOnSphere, pointOnX, Color.RED, 1.0),
            getLine(pointOnSphere, pointOnY, Color.RED, 1.0),
            getLine(pointOnSphere, pointOnZ, Color.RED, 1.0),
            getLineFromOrigin(projectionOnXY, Color.RED, 1.0),
            getLine(pointOnSphere, projectionOnXY, Color.RED, 1.0),
            getLine(pointOnX, projectionOnXY, Color.RED, 1.0),
            getLine(pointOnY, projectionOnXY, Color.RED, 1.0),
            getLine(pointOnX, pointOnZ, Color.RED, 1.0),
            getLine(pointOnY, pointOnZ, Color.RED, 1.0)
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