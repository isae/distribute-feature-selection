package ru.ifmo.ctddev.isaev

import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class)
//private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class, SymmetricUncertainty::class)

private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()

private data class PointProcessingResult(
        val cutsForAllPoints: Map<SpacePoint, RoaringBitmap>,
        val cutChangePositions: Set<SpacePoint>
)

private data class PointProcessingFinalResult(
        val cutsForAllPoints: Map<SpacePoint, RoaringBitmap>,
        val pointsToTry: List<Point>
)

private fun getBasicSpace(dimensionality: Int, epsilon: Int): MutableList<SpacePoint> {
    val result = ArrayList<SpacePoint>()
    if (dimensionality < 1) {
        throw IllegalStateException("Incorrect dimensionality: $dimensionality")
    }
    result.addAll(
            0.rangeTo(epsilon).map {
                val basicPoint = SpacePoint(dimensionality)
                basicPoint[0] = it
                return@map basicPoint
            }
    )

    1.until(dimensionality).forEach { dim ->
        val newDimension = 1.rangeTo(epsilon)
                .flatMap { value ->
                    result.map { point ->
                        val newPoint = point.clone()
                        newPoint[dim] = value
                        newPoint
                    }
                }
        result.addAll(newDimension)
    }
    return result
}

fun main(args: Array<String>) {
    val (cutsForAllPoints, pointsToTry) = processAllPointsWithEnrichment(100, 2)
    println("Found ${pointsToTry.size} points to try with enrichment")
    pointsToTry
            .sorted()
            .forEach { println(it) }
    if (!pointsToTry.all { it.coordinates.size == measures.size }) {
        throw IllegalStateException("Incorrect result")
    }
}

private fun processAllPointsWithEnrichment(startingEpsilon: Int,
                                           zoom: Int): PointProcessingFinalResult {
    var prevEpsilon = startingEpsilon //TODO: recalculate only changes
    var space = getBasicSpace(measures.size - 1, prevEpsilon).toSet()
    var prevIterChangePositions: Set<SpacePoint>
    var (cuts, currIterChangePositions) = processAllPoints(space, startingEpsilon)
    logToConsole({ "Cut change positions: ${currIterChangePositions.sorted()}" })

    // after enrichment
    do {
        prevIterChangePositions = currIterChangePositions
        logToConsole({ "Points to try before enrichment: ${space.size}" })
        val (newSpace, newEpsilon) = calculateEnrichment(space, prevIterChangePositions, prevEpsilon, zoom)
        space = newSpace
        logToConsole({ "Points to try after enrichment: ${space.size}" })
        val newResult = processAllPoints(space, newEpsilon)
        logToConsole({
            "Cut change positions: ${newResult.cutChangePositions.sorted()
                    .map { it.point }.map { it.map { i -> twoDecimalPlaces.format(i.toDouble() / (newEpsilon / startingEpsilon)) } }}}"
        })
        // TODO: holy fuck, this may significantly increase the time of processing

        cuts = newResult.cutsForAllPoints
        currIterChangePositions = newResult.cutChangePositions
        prevEpsilon = newEpsilon
    } while (prevIterChangePositions.size != currIterChangePositions.size)

    val anglesToTry = currIterChangePositions.map { getAngle(prevEpsilon, it) }
    val pointsToTry = anglesToTry.map { getPointOnUnitSphere(it) }

    return PointProcessingFinalResult(
            cuts,
            pointsToTry
    )
}

private fun rehash(set: HashSet<SpacePoint>) {
    val rehashedPoints = HashSet(set)
    set.clear()
    set.addAll(rehashedPoints)
}

fun getFirstDifferentBelowInIndex(index: Int, space: Set<SpacePoint>, pointToClone: SpacePoint): SpacePoint? {
    val point = pointToClone.clone()
    val firstBelow = point[index] - 1
    firstBelow.downTo(0)
            .forEach { coordBelow ->
                point[index] = coordBelow
                if (space.contains(point)) {
                    return point
                }
            }
    return null
}

fun calculateCutChanges(cutsForAllPoints: Map<SpacePoint, RoaringBitmap>): Set<SpacePoint> {
    val results = HashSet<SpacePoint>()
    cutsForAllPoints
            .forEach { point, cut ->
                0.until(point.size).forEach { coord ->
                    val firstBelow = getFirstDifferentBelowInIndex(coord, cutsForAllPoints.keys, point)
                    if (firstBelow != null) {
                        val firstBelowCut = cutsForAllPoints[firstBelow]
                        if (firstBelowCut != null && firstBelowCut != cut) {
                            results.add(point)
                        }
                    }
                }
            }
    return results
}

fun calculateEnrichment(
        oldSpace: Set<SpacePoint>,
        changePositions: Set<SpacePoint>,
        prevEpsilon: Int,
        zoom: Int): Pair<Set<SpacePoint>, Int> {
    logToConsole({ "Started calculating the enrichment of set with ${changePositions.size} points" })
    val newEpsilon = prevEpsilon * zoom
    val result = HashSet<SpacePoint>()
    oldSpace.forEach {
        //TODO: oldSpace is never used, so we may modify it
        val newPoint = it.clone()
        newPoint *= zoom
        result.add(newPoint)
    }
    val enrichment = HashSet<SpacePoint>()
    changePositions.forEach { point ->
        point.indices().forEach { index ->
            val firstBelow = getFirstDifferentBelowInIndex(index, oldSpace, point)
            if (firstBelow != null) {
                val prev = firstBelow[index] * zoom + 1
                val curr = point[index] * zoom
                prev.until(curr).forEach { newCoordValue ->
                    val newPoint = point.clone()
                    newPoint *= zoom
                    newPoint[index] = newCoordValue
                    enrichment.add(newPoint)
                }
            }
        }
    }
    result.addAll(enrichment)
    logToConsole({ "Finished enrichment calculation" })
    return Pair(result, newEpsilon)
}

private operator fun SpacePoint.timesAssign(multiplier: Int) {
    this.forEachIndexed { i, value ->
        this[i] = value * multiplier
    }
}

private fun processAllPoints(intPoints: Collection<SpacePoint>, epsilon: Int): PointProcessingResult {
    // begin first stage (before enrichment)
    logToConsole({ "Started the processing" })
    logToConsole({ "${intPoints.size} points to calculate measures on" })
    val cutsForAllPoints = processAllPointsHd(intPoints.toList(), dataSet, measures, epsilon, cutSize)
    logToConsole({ "Evaluated data, calculated cutting line and cuts for all points" })
    val cutChangePositions = calculateCutChanges(cutsForAllPoints)
    logToConsole({ "Found ${cutChangePositions.size} cut change positions (aka points to try)" })
    // end first stage (before enrichment)
    return PointProcessingResult(cutsForAllPoints, cutChangePositions)
}

private fun getFilteredDataSet(cutsForAllPoints: List<RoaringBitmap>, evaluatedData: List<DoubleArray>): List<DoubleArray> {
    val sometimesInCut = cutsForAllPoints
            .flatMap { it }
            .toSet()
    logToConsole({ "Sometimes in cut: ${sometimesInCut.size} features" })
    val alwaysInCut = sometimesInCut.filter { featureNum ->
        cutsForAllPoints.all { it.contains(featureNum) }
    }
    logToConsole({ "Always in cut: ${alwaysInCut.size} features: $alwaysInCut" })
    val needToProcess = sometimesInCut - alwaysInCut
    logToConsole({ "Need to process: ${needToProcess.size} features" })

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    return needToProcess.map { feature(it) }
}