package ru.ifmo.ctddev.isaev

import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.Styler
import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * @author iisaev
 */

//private val measures = listOf(SpearmanRankCorrelation::class, VDM::class)
private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class)
//private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class, SymmetricUncertainty::class)

val dimensionality = measures.size - 1

private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDataSet = evaluateDataSet(dataSet, measures)

private data class PointProcessingResult(
        val cutsForAllPoints: Map<SpacePoint, RoaringBitmap>,
        val cutChangePositions: Set<SpacePoint>
)

private data class PointProcessingFinalResult(
        val cutsForAllPoints: Map<SpacePoint, RoaringBitmap>,
        val pointsToTry: List<Point>
)

typealias Dimension = Int

typealias RowOfPoints = TreeSet<SpacePoint>

private val fullSpace = HashMap<Dimension, TreeMap<Coord, RowOfPoints>>()
        .apply {
            0.until(dimensionality).forEach { dim ->
                putIfAbsent(dim, TreeMap())
            }
        }


private val cutsForAllPoints = TreeMap<SpacePoint, RoaringBitmap>() //TODO:hashmap is ok?


private fun calculateBasicPoints(): List<SpacePoint> {
    if (dimensionality < 1) {
        throw IllegalStateException("Incorrect dimensionality: $dimensionality")
    }
    val basicPoints = ArrayList<SpacePoint>()
    basicPoints.add(SpacePoint(dimensionality))
    0.until(dimensionality).forEach {
        val zeros = LongArray(dimensionality)
        zeros[it] = 1
        basicPoints.add(SpacePoint(zeros, 1))
    }
    val ones = LongArray(dimensionality, { 1 })
    basicPoints.add(SpacePoint(ones, 1))
    return basicPoints
}

fun main(args: Array<String>) {
    val chart = Chart()
    val (cutsForAllPoints, pointsToTry) = processAllPointsWithEnrichment(chart)
    println("Found ${pointsToTry.size} points to try with enrichment")
    pointsToTry
            .sorted()
            .forEach { println(it) }
    if (!pointsToTry.all { it.coordinates.size == measures.size }) {
        throw IllegalStateException("Incorrect result")
    }
}

private fun processAllPointsWithEnrichment(chart: Chart?): PointProcessingFinalResult {
    var enrichmentDim = -1
    var currentEnrichment = calculateBasicPoints()
    do {
        val cutsForEnrichment = processAllPointsHd(currentEnrichment, evaluatedDataSet, cutSize)
        addEnrichmentToSpace(currentEnrichment)
        val beforeEnrichment = cutsForAllPoints.size
        currentEnrichment.forEachIndexed { i, point -> cutsForAllPoints.putIfAbsent(point, cutsForEnrichment[i]) }
        val afterEnrichment = cutsForAllPoints.size
        chart?.update()
        enrichmentDim = (enrichmentDim + 1) % dimensionality
        currentEnrichment = calculateEnrichment(0)
        //logToConsole { "Current enrichment: ${currentEnrichment.toList()}" }
    } while (currentEnrichment.isNotEmpty())

    val anglesToTry = cutsForAllPoints.keys
            .filter { it.eqToPrev.all { !it } }
            .map { getAngle(it) }
    val pointsToTry = anglesToTry.map { getPointOnUnitSphere(it) }

    return PointProcessingFinalResult(
            cutsForAllPoints,
            pointsToTry
    )
}

fun addEnrichmentToSpace(currentEnrichment: Collection<SpacePoint>) {
    currentEnrichment.forEach { point ->
        0.until(dimensionality).forEach { dim ->
            val coord = Coord.from(point.point[dim], point.delta)
            fullSpace[dim]?.putIfAbsent(coord, TreeSet())
            fullSpace[dim]?.get(coord)?.add(point)
        }
    }
}

val tempRoaringBitmap = RoaringBitmap()

fun calculateEnrichment(dim: Int): List<SpacePoint> {
    val dimension = fullSpace[dim]!!
    val otherDim = 1 - dim
    val result = ArrayList<SpacePoint>()
    var notChanged = 0
    dimension.forEach { coord, points ->
        points.forEach { point ->
            if (!point.eqToPrev[dim]) {
                val prev = points.lower(point)
                if (prev != null) {
                    val prevCut = cutsForAllPoints[prev]!!
                    val currCut = cutsForAllPoints[point]!!
                    val diff = getDiff(prevCut, currCut, tempRoaringBitmap)
                    if (diff.cardinality == 0) {
                        point.eqToPrev[dim] = true
                    }
                    if (diff.cardinality > 1) {
                        if (prev.point[otherDim] == 0L && point.delta > (2.shl(8))) {
                        } else {
                            val newPoint = prev.inBetween(point)
                            logToConsole { "Found diff $diff between $point and $prev at point $newPoint; reverse diff is ${getDiff(currCut, prevCut, tempRoaringBitmap)}" }
                            result.add(newPoint)
                        }
                    } else {
                        ++notChanged
                    }
                }
            }
        }
    }
    println("##### Calculated enrichment of size ${result.size} ##### for dimension $dim; Found not changed: $notChanged; ratio: ${result.size / notChanged.toDouble()}")
    return result
}

private fun getCurrentPoints(): List<SpacePoint> {
    val points = cutsForAllPoints.keys
            .filter { it.eqToPrev.all { !it } }
    return points
}

private class Chart {
    // Create Chart
    val chart = XYChartBuilder().width(800).height(600).build().apply {
        addSeries("data", doubleArrayOf(0.0), doubleArrayOf(0.0))

        // Customize Chart
        styler.defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Scatter
        styler.isChartTitleVisible = false
        styler.isLegendVisible = false
        styler.legendPosition = Styler.LegendPosition.InsideSW
        styler.markerSize = 5
    }!!


    // Show it
    val sw = SwingWrapper(chart).apply {
        displayChart()
    }

    fun update() {
        Thread.sleep(1000)
        val points = getCurrentPoints()
        val xData = points.map { it.point[0].toDouble() / it.delta }
        val yData = points.map { it.point[1].toDouble() / it.delta }
        logToConsole { "Current points: ${points.size}" }
        chart.updateXYSeries("data", xData, yData, null)
        sw.repaintChart()
    }

}