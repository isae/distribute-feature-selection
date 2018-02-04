package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import java.time.LocalDateTime


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val measures = arrayOf(VDM(), SpearmanRankCorrelation())
    val featuresToTake = 50
    val rawDataSet = DataSetReader().readCsv(args[0])
    val dataSet = rawDataSet.take(featuresToTake)
    val n = 100
    val xData = 0.rangeTo(n)
            .map { (it.toDouble()) / n }

    fun feature(i: Int) = getFeaturePositions(i, xData, dataSet, measures)

    // Create Chart
    val chartBuilder = XYChartBuilder()
            .width(640)
            .height(480)
            .xAxisTitle("Measure Proportion (VDM to Spearman)")
            .yAxisTitle("Number of feature in order")
    val chart = XYChart(chartBuilder)
    for (i in 0..20){
        chart.addSeries("Feature $i", xData, feature(i)).marker = SeriesMarkers.NONE
    }

    // Show it
    SwingWrapper(chart).displayChart()

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 400);
}

private fun getFeaturePositions(pos: Int,
                                xData: List<Double>,
                                dataSet: FeatureDataSet,
                                measures: Array<RelevanceMeasure>): List<Int> {
    return xData
            .map {
                val sortedFeatures = DataSetEvaluator().evaluateFeatures(dataSet, Point(it, 1 - it), measures)
                        .zip(generateSequence(0, { it + 1 }))
                        .sortedBy { it.first.name }
                        .toList()
                val point = sortedFeatures
                        .map { it.second }
                point[pos]
            }
}