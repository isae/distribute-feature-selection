package ru.ifmo.ctddev.isaev

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRainbow
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.rendering.canvas.Quality
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.space.getEvaluatedData
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import kotlin.reflect.KClass


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    val measures: Array<KClass<out RelevanceMeasure>> = arrayOf(VDM::class, SpearmanRankCorrelation::class, SymmetricUncertainty::class)
    //val n = 100
    //val xyData = 0.rangeTo(n).map { (it.toDouble()) / n }
    val xyData = 0.rangeTo(100)
            .flatMap { x ->
                0.rangeTo(100 - x)
                        .map { y ->
                            listOf(
                                    x.toDouble() / 100,
                                    y.toDouble() / 100,
                                    (100 - x - y).toDouble() / 100)
                        }
            }
    val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(dataSet, measures.toList())
    val evaluatedData = getEvaluatedData(xyData, dataSet, valuesForEachMeasure)

    val feature: (Int) -> List<Number> = { getFeaturePositions(it, evaluatedData) }
    AnalysisLauncher.open(FeatureMoving3d(feature))
}

class FeatureMoving3d(val feature: (Int) -> List<Number>) : AbstractAnalysis() {

    override fun init() {
        // Define a function to plot

        // Define range and precision for the function to plot
        val range = Range(-3f, 3f)
        val steps = 80

        // Create the object to represent the function over the given range.
        val surface = Builder.buildOrthonormal(OrthonormalGrid(range, steps, range, steps), object : Mapper() {
            override fun f(x: Double, y: Double): Double {
                return x + y
            }
        })
                .apply {
                    colorMapper = ColorMapper(ColorMapRainbow(), bounds.zmin.toDouble(), bounds.zmax.toDouble(), org.jzy3d.colors.Color(1f, 1f, 1f, .5f))
                    faceDisplayed = true
                    wireframeDisplayed = false
                }

        // Create a chart
        chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType())
        chart.scene.graph.add(surface)
    }
}