package ru.ifmo.ctddev.isaev

import org.jzy3d.chart.AWTChart
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRainbow
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.rendering.canvas.Quality

/**
 * @author iisaev
 */
fun main(args: Array<String>) {
    // Define a function to plot
    val mapper = object : Mapper() {
        override fun f(x: Double, y: Double): Double {
            return 10.0 * Math.sin(x / 10) * Math.cos(y / 20)
        }
    }

// Define range and precision for the function to plot
    val range = Range(-150f, 150f)
    val steps = 50

// Create a surface drawing that function
    val surface = Builder.buildOrthonormal(OrthonormalGrid(range, steps), mapper)
    surface.colorMapper = ColorMapper(ColorMapRainbow(), range)
    surface.faceDisplayed = true
    surface.wireframeDisplayed = false
    surface.wireframeColor = Color.BLACK

// Create a chart and add the surface
    val chart = AWTChart(Quality.Advanced)
    chart.add(surface)
    chart.open("Jzy3d Demo", 600, 600)
}