package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRainbow
import org.jzy3d.colors.colormaps.ColorMapWhiteGreen
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.rendering.canvas.Quality

/**
 * @author iisaev
 */
fun main(args: Array<String>) {
    AnalysisLauncher.open(KotlinSurfaceDemo())
}

private class KotlinSurfaceDemo : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(-3f, 3f)
        val steps = 80

        // Create the object to represent the function over the given range.
        val surface1 = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
            override fun f(x: Double, y: Double): Double {
                return x + y
            }
        })
                .apply {
                    colorMapper = ColorMapper(ColorMapRainbow(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        val surface2 = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
            override fun f(x: Double, y: Double): Double {
                return x * x + y * y
            }
        })
                .apply {
                    colorMapper = ColorMapper(ColorMapWhiteGreen(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }


        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType())
        chart.scene.graph.add(surface1)
        chart.scene.graph.add(surface2)
    }
}

