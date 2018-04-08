package ru.ifmo.ctddev.isaev

import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.getAngle
import ru.ifmo.ctddev.isaev.space.getFeaturePositions
import ru.ifmo.ctddev.isaev.space.getPointOnUnitSphere
import ru.ifmo.ctddev.isaev.space.processAllPointsHd


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class, SymmetricUncertainty::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()

typealias SpacePoint = IntArray

private data class PointProcessingResult(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<Set<Int>>,
        val cutChangePositions: List<SpacePoint>
)

private data class PointProcessingFinalResult(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<Set<Int>>,
        val pointsToTry: List<Point>
)

fun main(args: Array<String>) {
    val (evaluatedData, cuttingLineY, cutsForAllPoints, pointsToTry) = processAllPointsWithEnrichment(10)
    println("Found ${pointsToTry.size} points to try with enrichment")
    println(pointsToTry)
    if (!pointsToTry.all { it.coordinates.size == measures.size }) {
        throw IllegalStateException("Incorrect result")
    }
    val space = Space(3, 100)
    println("Space size is ${space.size()} points")

    //val features = getFilteredDataSet(cutsForAllPoints, evaluatedData)
    //features.forEach { println(Arrays.toString(it)) }
}

private class Space : Iterable<SpacePoint> {

    private val dimensions = HashMap<Int, MutableSet<SpacePoint>>()

    override fun iterator(): Iterator<SpacePoint> {
        val dim0 = dimensions[0]!!
        return dim0.iterator()
    }

    constructor(dimensionality: Int, epsilon: Int) {
        if (dimensionality < 1) {
            throw IllegalStateException("Incorrect dimensionality: $dimensionality")
        }
        0.until(dimensionality).forEach { dim ->
            // dimensions[dim] = TreeSet<SpacePoint>(compareBy { arr -> arr[dim] })
            dimensions[dim] = HashSet()
        }
        0.rangeTo(epsilon).forEach {
            val result = IntArray(dimensionality)
            result[0] = it
            dimensions[0]?.add(result)
        }
        val zeroDim = dimensions[0]!!

        1.until(dimensionality).forEach { dim ->
            val results = 1.rangeTo(epsilon)
                    .flatMap { value ->
                        zeroDim.map { point ->
                            val result = point.clone()
                            result[dim] = value
                            result
                        }
                    }
            dimensions[0]?.addAll(results)
        }
    }

    fun size(): Int {
        return dimensions[0]?.size ?: 0
    }
}

private fun processAllPointsWithEnrichment(startingEpsilon: Int): PointProcessingFinalResult {
    var prevEpsilon = startingEpsilon //TODO: recalculate only changes
    var prevPositions = Space(measures.size - 1, prevEpsilon)
    var prevAngles = prevPositions.toList()
    var (evaluatedData, cuttingLineY, cutsForAllPoints, currCutChangePositions) = processAllPoints(prevAngles, startingEpsilon)
    var prevCutChangePositions: List<Int>

    /*// after enrichment
    do {
        prevCutChangePositions = currCutChangePositions

        val newEpsilon = prevEpsilon * 10
        val newPositions = prevPositions.map { it * 10 }.toSortedSet()
        prevCutChangePositions.forEach {
            val prev = it - 1
            newPositions.addAll((prev * 10)..(it * 10))
        }
        val newAngles = newPositions.map { getAngle(newEpsilon, it) }
        val newResult = processAllPoints(newAngles)

        evaluatedData = newResult.evaluatedData
        cuttingLineY = newResult.cuttingLineY
        cutsForAllPoints = newResult.cutsForAllPoints
        currCutChangePositions = newResult.cutChangePositions
        prevEpsilon = newEpsilon
        prevPositions = newPositions
        prevAngles = newAngles
    } while (currCutChangePositions.size != prevCutChangePositions.size)*/

    val pointsToTry = currCutChangePositions
            .map {
                val angle = getAngle(prevEpsilon, it)
                getPointOnUnitSphere(angle)
            }

    return PointProcessingFinalResult(
            evaluatedData,
            cuttingLineY,
            cutsForAllPoints,
            pointsToTry
    )
}

private fun processAllPoints(intPoints: List<SpacePoint>, epsilon: Int): PointProcessingResult {
    // begin first stage (before enrichment)
    logToConsole("Started the processing")
    logToConsole("${intPoints.size} points to calculate measures on")
    val (evaluatedData, cutsForAllPoints) =
            processAllPointsHd(intPoints, dataSet, measures, epsilon, cutSize)
    logToConsole("Evaluated data, calculated cutting line and cuts for all points")

    /* val cutChangePositions = cutsForAllPoints
             .mapIndexed { index, cut -> Pair(index, cut) }
             .filter { it.first != 0 && it.second != cutsForAllPoints[it.first - 1] }
             .map { it.first }*/
    val cutChangePositions = emptyList<SpacePoint>()

    logToConsole("Found ${cutChangePositions.size} points to try")
    TODO("Not implemented")
    // end first stage (before enrichment)
    //return PointProcessingResult(evaluatedData, cuttingLineY, cutsForAllPoints, cutChangePositions)
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