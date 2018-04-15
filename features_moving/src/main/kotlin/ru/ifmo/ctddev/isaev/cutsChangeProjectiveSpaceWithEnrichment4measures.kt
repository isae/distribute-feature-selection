package ru.ifmo.ctddev.isaev

import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class, SymmetricUncertainty::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()

private data class PointProcessingResult(
        val evaluatedData: Map<SpacePoint, DoubleArray>,
        val cutsForAllPoints: Map<SpacePoint, Set<Int>>,
        val cutChangePositions: Set<SpacePoint>
)

private data class PointProcessingFinalResult(
        val evaluatedData: Map<SpacePoint, DoubleArray>,
        val cutsForAllPoints: Map<SpacePoint, Set<Int>>,
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
    result.removeAt(0) //[0,0,0,0] is not a valid point
    return result
}

fun main(args: Array<String>) {
    val (evaluatedData, cutsForAllPoints, pointsToTry) = processAllPointsWithEnrichment(10)
    println("Found ${pointsToTry.size} points to try with enrichment")
    println(pointsToTry)
    if (!pointsToTry.all { it.coordinates.size == measures.size }) {
        throw IllegalStateException("Incorrect result")
    }
    val space = getBasicSpace(3, 100)
    println("Space size is ${space.size}  points")
}

private fun processAllPointsWithEnrichment(startingEpsilon: Int): PointProcessingFinalResult {
    var prevEpsilon = startingEpsilon //TODO: recalculate only changes
    var startingSpace = getBasicSpace(measures.size - 1, prevEpsilon)
    var (evaluatedData, cutsForAllPoints, currCutChangePositions) = processAllPoints(startingSpace, startingEpsilon)
    var prevCutChangePositions: MutableSet<SpacePoint>

    // after enrichment
    do {

        val newEpsilon = prevEpsilon * 10
        currCutChangePositions.onEach { it *= 10 }
        prevCutChangePositions = currCutChangePositions.toMutableSet() // TODO ??
        prevCutChangePositions.addAll(calculateEnrichment(prevCutChangePositions))
        val newResult = processAllPoints(prevCutChangePositions, newEpsilon)

        evaluatedData = newResult.evaluatedData
        cutsForAllPoints = newResult.cutsForAllPoints
        currCutChangePositions = newResult.cutChangePositions
        prevEpsilon = newEpsilon
    } while (currCutChangePositions.size != prevCutChangePositions.size)

    val pointsToTry = currCutChangePositions
            .map {
                val angle = getAngle(prevEpsilon, it)
                getPointOnUnitSphere(angle)
            }

    return PointProcessingFinalResult(
            evaluatedData,
            cutsForAllPoints,
            pointsToTry
    )
}

fun getFirstDifferentBelow(coord: Int, validate: (SpacePoint) -> Boolean, point: SpacePoint): SpacePoint? {
    val newPoint = point.clone()
    val prevCoordValue = newPoint[coord] - 1
    prevCoordValue.downTo(0)
            .forEach { coordValue ->
                newPoint[coord] = coordValue
                if (validate(newPoint)) {
                    return newPoint
                }
            }
    return null
}

fun calculateCutChanges(currentResult: Map<SpacePoint, Set<Int>>): Set<SpacePoint> {
    val results = HashSet<SpacePoint>()
    currentResult
            .forEach { point, cut ->
                0.until(point.size).forEach { coord ->
                    val firstBelow = getFirstDifferentBelow(coord, {
                        val newPointCut = currentResult[it]
                        newPointCut != null && newPointCut != cut
                    }, point)
                    if (firstBelow != null) {
                        results.add(firstBelow)
                    }
                }
            }
    return results
}

fun calculateEnrichment(positions: Set<SpacePoint>): Collection<SpacePoint> {
    val result = HashSet<SpacePoint>()
    positions.forEach { point ->
        0.until(point.size).forEach { coord ->
            val firstBelow = getFirstDifferentBelow(coord, { positions.contains(it) }, point)
            if (firstBelow != null) {
                (firstBelow[coord] + 1).until(point[coord]).forEach { newCoordValue ->
                    val newPoint = point.clone()
                    newPoint[coord] = newCoordValue
                    result.add(newPoint)
                }
            }
        }
    }
    return result
}

private operator fun SpacePoint.timesAssign(multiplier: Int) {
    this.forEachIndexed { i, value ->
        this[i] = value * multiplier
    }
}

private fun processAllPoints(intPoints: Collection<SpacePoint>, epsilon: Int): PointProcessingResult {
    // begin first stage (before enrichment)
    logToConsole("Started the processing")
    logToConsole("${intPoints.size} points to calculate measures on")
    val (evaluatedData, cutsForAllPoints) =
            processAllPointsHd(intPoints.toList(), dataSet, measures, epsilon, cutSize)
    logToConsole("Evaluated data, calculated cutting line and cuts for all points")
    val cutChangePositions = calculateCutChanges(cutsForAllPoints)
    logToConsole("Found ${cutChangePositions.size} points to try")
    // end first stage (before enrichment)
    return PointProcessingResult(evaluatedData, cutsForAllPoints, cutChangePositions)
}

private fun getFilteredDataSet(cutsForAllPoints: List<Set<Int>>, evaluatedData: List<DoubleArray>): List<DoubleArray> {
    val sometimesInCut = cutsForAllPoints
            .flatMap { it }
            .toSet()
    logToConsole("Sometimes in cut: ${sometimesInCut.size} features")
    val alwaysInCut = sometimesInCut.filter { featureNum ->
        cutsForAllPoints.all { it.contains(featureNum) }
    }
    logToConsole("Always in cut: ${alwaysInCut.size} features: $alwaysInCut")
    val needToProcess = sometimesInCut - alwaysInCut
    logToConsole("Need to process: ${needToProcess.size} features")

    fun feature(i: Int) = getFeaturePositions(i, evaluatedData)

    return needToProcess.map { feature(it) }
}