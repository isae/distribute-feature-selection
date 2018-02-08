package ru.ifmo.ctddev.isaev

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


/**
 * @author iisaev
 */

fun main(args: Array<String>) {
    val measures: Array<KClass<out RelevanceMeasure>> = arrayOf(VDM::class, SpearmanRankCorrelation::class)
    val featuresToTake = 19993
    val rawDataSet = DataSetReader().readCsv(args[0])
    val dataSet = rawDataSet.take(featuresToTake)
    val n = 100
    val xData = 0.rangeTo(n)
            .map { (it.toDouble()) / n }
    val normalize = true
    println(xData.map { String.format("%.2f", it) })
    val valuesForEachMeasure = measures.map { m ->
        val measure = m.createInstance()
        val evaluated = dataSet.features
                .map { measure.evaluate(it, dataSet.classes) }
                .toList()
        val minValue = evaluated.min() ?: throw IllegalStateException()
        val maxValue = evaluated.max() ?: throw IllegalStateException()
        if (normalize) evaluated.normalize(minValue, maxValue) else evaluated
    }

    val evaluatedData = getEvaluatedData(xData, dataSet, valuesForEachMeasure)
    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    // Create Chart
    val chartBuilder = XYChartBuilder()
            .width(640)
            .height(480)
            .xAxisTitle("Measure Proportion (${measures[0].simpleName} to ${measures[1].simpleName})")
            .yAxisTitle("Number of feature in order")
    val chart = XYChart(chartBuilder)
    for (i in 0..50) {
        chart.addSeries("Feature $i", xData, feature(i)).marker = SeriesMarkers.NONE
    }
    //chart.addSeries("Feature 4", xData, feature(4)).marker = SeriesMarkers.NONE

    // Show it
    SwingWrapper(chart).displayChart()

    // or save it in high-res
    BitmapEncoder.saveBitmapWithDPI(chart, "./charts/Test_Chart_${LocalDateTime.now()}", BitmapEncoder.BitmapFormat.PNG, 200);
}

private fun getFeaturePositions(pos: Int,
                                evaluatedData: List<List<Double>>): List<out Number> {
    val result = evaluatedData.map { it[pos] }
    println(result.map { String.format("%.2f", it.toDouble()) })
    return result
}

private fun getEvaluatedData(xData: List<Double>, dataSet: FeatureDataSet, valuesForEachMeasure: List<List<Double>>): List<List<Double>> {
    return xData
            .map {
                val sortedFeatures = evaluateDataSet(dataSet, it, valuesForEachMeasure)
                        .zip(generateSequence(0, { it + 1 }))
                        .sortedBy { it.first.name }
                        .toList()
                val point = sortedFeatures
                        .map { it.first.measure }
                point
            }
}

private fun evaluateDataSet(dataSet: FeatureDataSet,
                            c1: Double,
                            valuesForEachMeasure: List<List<Double>>): Sequence<EvaluatedFeature> {
    val c2 = 1 - c1
    val measureCosts = Point(c1, c2)
    val ensembleMeasures = 0.until(dataSet.features.size)
            .map { i ->
                val measuresForParticularFeature = valuesForEachMeasure
                        .map { it[i] }
                measureCosts.coordinates
                        .zip(measuresForParticularFeature)
            }
            .map {
                val sumByDouble = it
                        .sumByDouble { (measureCost, measureValue) -> measureCost * measureValue }
                sumByDouble
            }
    val sortedBy = dataSet.features.zip(ensembleMeasures)
            .map { (f, m) -> EvaluatedFeature(f, m) }
            .sortedBy { it.measure }
    return sortedBy
            .asSequence()
}