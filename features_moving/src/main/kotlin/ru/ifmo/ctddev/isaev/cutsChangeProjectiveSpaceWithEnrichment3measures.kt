package ru.ifmo.ctddev.isaev

import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.max


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

private val fullSpace = HashMap<Dimension, HashMap<Coord, RowOfPoints>>()
        .apply {
            0.until(dimensionality).forEach { dim ->
                putIfAbsent(dim, HashMap())
            }
        }


private val cutsForAllPoints = HashMap<SpacePoint, RoaringBitmap>()


private fun calculateBasicPoints(): HashSet<SpacePoint> {
    if (dimensionality < 1) {
        throw IllegalStateException("Incorrect dimensionality: $dimensionality")
    }
    val basicPoints = HashSet<SpacePoint>()
    basicPoints.add(SpacePoint(dimensionality))
    0.until(dimensionality).forEach {
        val zeros = IntArray(dimensionality)
        zeros[it] = 1
        basicPoints.add(SpacePoint(zeros, 1))
    }
    val ones = IntArray(dimensionality, { 1 })
    basicPoints.add(SpacePoint(ones, 1))
    return basicPoints
}

fun main(args: Array<String>) {
    val (cutsForAllPoints, pointsToTry) = processAllPointsWithEnrichment()
    println("Found ${pointsToTry.size} points to try with enrichment")
    pointsToTry
            .sorted()
            .forEach { println(it) }
    if (!pointsToTry.all { it.coordinates.size == measures.size }) {
        throw IllegalStateException("Incorrect result")
    }
}

private fun processAllPointsWithEnrichment(): PointProcessingFinalResult {
    var enrichmentDim = -1
    var currentEnrichment = calculateBasicPoints()
    while (!currentEnrichment.isEmpty()) {
        val pointsToCalculate = currentEnrichment.toList()
        val cutsForEnrichment = processAllPointsHd(pointsToCalculate, evaluatedDataSet, cutSize)
        addEnrichmentToSpace(currentEnrichment)
        pointsToCalculate.forEachIndexed { i, point ->
            cutsForAllPoints[point] = cutsForEnrichment[i]
        }
        enrichmentDim = (enrichmentDim + 1) % dimensionality
        currentEnrichment = calculateEnrichment(enrichmentDim)
    }

    val anglesToTry = cutsForAllPoints.keys.map { getAngle(it) }
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

fun calculateEnrichment(dim: Int): HashSet<SpacePoint> {
    val dimension = fullSpace[dim]!!
    val otherDim = 1 - dim
    val result = HashSet<SpacePoint>()
    var notChanged = 0
    dimension.forEach { coord, points ->
        points.forEach { point ->
            val prev = points.lower(point)
            if (prev != null) {
                val prevCut = cutsForAllPoints[prev]!!
                val currCut = cutsForAllPoints[point]!!
                val diff = getDiff(prevCut, currCut, tempRoaringBitmap)
                if (diff.cardinality > 1) {
                    val newPoint = inBetween(point, prev, otherDim)
                    result.add(newPoint)
                } else {
                    ++notChanged
                }
            }
        }
    }

    logToConsole { "Calculated enrichment of size ${result.size} for dimension $dim; Found not changed: $notChanged; ratio: ${notChanged.toDouble() / result.size}" }
    return result
}

private fun inBetween(point: SpacePoint, prev: SpacePoint, dim: Int): SpacePoint {
    val curArr = point.point
    val curDelta = point.delta
    val prevArr = prev.point
    val prevDelta = prev.delta

    val newArr = curArr.clone()
    var newDelta: Int

    val commonDivisor = max(curDelta, prevDelta)
    if (curDelta != commonDivisor) {
        val multiplier = commonDivisor / curDelta // all deltas are powers of 2
        newDelta = commonDivisor
        newArr.indices.forEach { newArr[it] *= multiplier }
    } else {
        newDelta = curDelta
    }

    val sumForDim = newArr[dim] + prevArr[dim]
    if (sumForDim % 2 == 0) {
        newArr[dim] = sumForDim / 2
    } else {
        newArr.indices.forEach {
            newArr[it] *= 2
        }
        newArr[dim] = sumForDim
        newDelta *= 2
    }

    return SpacePoint(newArr, newDelta)
}