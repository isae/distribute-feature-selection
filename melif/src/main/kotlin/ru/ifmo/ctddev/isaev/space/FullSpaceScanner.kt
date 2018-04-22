package ru.ifmo.ctddev.isaev.space

import org.roaringbitmap.RoaringBitmap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.*
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * @author iisaev
 */
class FullSpaceScanner(val config: AlgorithmConfig, val dataSet: DataSet, threads: Int) {

    private val logger: Logger = LoggerFactory.getLogger(FullSpaceScanner::class.java)

    private val foldsEvaluator = config.foldsEvaluator

    private val executorService = Executors.newFixedThreadPool(threads)

    private val featureDataSet = dataSet.toFeatureSet()

    fun run(): RunStats {
        val runStats = RunStats(config, dataSet, javaClass.simpleName)
        logger.info("Started {} at {}", javaClass.simpleName, runStats.startTime)
        val (_, _, _, _, points, _) =
                calculateAllPointsWithEnrichment2d(1000, featureDataSet, config.measureClasses, 50, 10)
        logger.info("Found ${points.size} points to process")
        val scoreFutures = points.map { p ->
            executorService.submit(Callable<SelectionResult> { foldsEvaluator.getSelectionResult(dataSet, p, runStats) })
        }
        val scores: List<SelectionResult> = scoreFutures.map { it.get() }

        logger.info("Total scores: ")
        scores.forEach { println("Score ${it.score} at point ${it.point}") }
        logger.info("Max score: {} at point {}",
                runStats.bestResult.score,
                runStats.bestResult.point
        )
        runStats.finishTime = LocalDateTime.now()
        logger.info("Finished {} at {}", javaClass.simpleName, runStats.finishTime)
        logger.info("Working time: {} seconds", runStats.workTime)
        executorService.shutdown()
        return runStats
    }
}

private data class PointProcessingResult2d(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<RoaringBitmap>,
        val cutChangePositions: List<Int>
)

public data class PointProcessingFinalResult2d(
        val evaluatedData: List<DoubleArray>,
        val cuttingLineY: List<Double>,
        val cutsForAllPoints: List<RoaringBitmap>,
        val cutChangePositions: List<Int>,
        val pointsToTry: List<Point>,
        val angles: List<Double>
)

val twoDecimalPlaces = DecimalFormat(".##")
fun calculateAllPointsWithEnrichment2d(startingEpsilon: Int,
                                       dataSet: FeatureDataSet,
                                       measures: List<KClass<out RelevanceMeasure>>,
                                       cutSize: Int,
                                       zoom: Int): PointProcessingFinalResult2d {
    var prevEpsilon = startingEpsilon
    var prevPositions = calculateBitMap(0..prevEpsilon)
    var (evaluatedData, cuttingLineY, cutsForAllPoints, currCutChangePositions) = processAllPoints(prevPositions, prevEpsilon, dataSet, measures, cutSize)
    var prevCutChangePositions: List<Int>
    logToConsole({ "Cut change positions: $currCutChangePositions" })

    // after enrichment
    do {
        prevCutChangePositions = currCutChangePositions

        val (newEpsilon, newPositions) = getNewPositions(prevEpsilon, prevPositions, prevCutChangePositions, zoom)
        val newResult = processAllPoints(newPositions, newEpsilon, dataSet, measures, cutSize)

        evaluatedData = newResult.evaluatedData
        cuttingLineY = newResult.cuttingLineY
        cutsForAllPoints = newResult.cutsForAllPoints
        currCutChangePositions = newResult.cutChangePositions
        logToConsole({ "Cut change positions: ${currCutChangePositions.map { twoDecimalPlaces.format(it.toDouble() / (newEpsilon / startingEpsilon)) }}" })
        prevEpsilon = newEpsilon
        prevPositions = newPositions
    } while (currCutChangePositions.size != prevCutChangePositions.size)
    val anglesToTry = currCutChangePositions.map { getAngle(prevEpsilon, it) }
    val pointsToTry = anglesToTry.map { getPointOnUnitSphere(it) }

    return PointProcessingFinalResult2d(
            evaluatedData,
            cuttingLineY,
            cutsForAllPoints,
            currCutChangePositions,
            pointsToTry,
            prevPositions.map { getAngle(prevEpsilon, it) }
    )
}

private fun getNewPositions(prevEpsilon: Int, prevPositions: RoaringBitmap, prevCutChangePositions: List<Int>, zoom: Int): Pair<Int, RoaringBitmap> {
    val newEpsilon = prevEpsilon * zoom
    val newPositions = calculateBitMap(prevPositions.map { it * zoom })
    prevCutChangePositions.forEach {
        val prev = it - 1
        newPositions.add(((prev * zoom) + 1).toLong(), it * zoom.toLong())
    }
    return Pair(newEpsilon, newPositions)
}

private fun processAllPoints(positions: RoaringBitmap,
                             epsilon: Int,
                             dataSet: FeatureDataSet,
                             measures: List<KClass<out RelevanceMeasure>>,
                             cutSize: Int
): PointProcessingResult2d {
    // begin first stage (before enrichment)
    logToConsole({ "Started the processing" })
    logToConsole({ "${positions.cardinality} points to calculate measures on" })
    val angles = positions.map { getAngle(epsilon, it) }
    val chunkSize = getChunkSize(cutSize, dataSet, measures)
    val cutsForAllPoints = ArrayList<RoaringBitmap>(angles.size)
    val evaluatedData = ArrayList<DoubleArray>(angles.size)
    val cuttingLineY = ArrayList<Double>(angles.size)
    angles.chunked(chunkSize)
            .forEach { cut ->
                logToConsole({ "Processing chunk of ${cut.size} points" })
                val pointsInProjectiveCoords = cut.map { getPointOnUnitSphere(it) }
                //logToConsole("Points to process: ")
                //pointsInProjectiveCoords.forEach { println(it) }
                val (evaluatedDataChunk, cuttingLineYChunk, cutsForAllPointsRaw) = processAllPointsFast(pointsInProjectiveCoords, dataSet, measures, cutSize)
                val cutsForAllPointsChunk = cutsForAllPointsRaw.map { calculateBitMap(it) }
                cutsForAllPoints.addAll(cutsForAllPointsChunk)
                evaluatedData.addAll(evaluatedDataChunk)
                cuttingLineY.addAll(cuttingLineYChunk)
            }
    logToConsole({ "Evaluated data, calculated cutting line and cuts for all points" })

    val cutChangePositions = positions
            .filterIndexed { i, pos -> i != 0 && cutsForAllPoints[i] != cutsForAllPoints[i - 1] }
    logToConsole({ "Found ${cutChangePositions.size} points to try" })

    // end first stage (before enrichment)
    return PointProcessingResult2d(evaluatedData, cuttingLineY, cutsForAllPoints, cutChangePositions)
}

fun calculateBitMap(bitsToSet: Iterable<Int>): RoaringBitmap {
    val result = RoaringBitmap()
    bitsToSet.forEach {
        result.add(it)
    }
    result.runOptimize()
    return result
}

fun processAllPoints(xData: List<Point>,
                     dataSet: FeatureDataSet,
                     measures: List<KClass<out RelevanceMeasure>>,
                     cutSize: Int): Triple<List<DoubleArray>, List<Double>, List<List<Int>>> {
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val evaluatedDataWithNumbers = evaluatedData.map { it.mapIndexed { index, d -> Pair(index, d) } }

    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map { it.sortedBy { pair -> -pair.second } } //max first
    val cuttingLineY = rawCutsForAllPoints.map { it[cutSize - 1].second }
    val cutsForAllPoints = rawCutsForAllPoints
            .map { it.take(cutSize) }
            .map { it.map { pair -> pair.first } }
    return Triple(evaluatedData, cuttingLineY, cutsForAllPoints)
}

fun processAllPointsFast(xData: List<Point>,
                         dataSet: FeatureDataSet,
                         measures: List<KClass<out RelevanceMeasure>>,
                         cutSize: Int)
        : Triple<List<DoubleArray>, List<Double>, List<List<Int>>> {
    if (xData[0].coordinates.size != 2) {
        throw IllegalArgumentException("Only two-dimensioned points are supported")
    }
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val range = Array(evaluatedData[0].size, { it })
    val evaluatedDataWithNumbers = evaluatedData.map { Pair(it, range.clone()) }
    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map {
                val featureNumbers = it.second
                val featureMeasures = it.first
                val comparator = kotlin.Comparator<Int> { o1, o2 -> compareValuesBy(o1, o2, { -featureMeasures[it] }) }
                Arrays.sort(featureNumbers, comparator)
                return@map Pair(featureMeasures, featureNumbers)
            } //max first
    val cuttingLineY = rawCutsForAllPoints
            .map {
                val lastFeatureInCut = it.second[cutSize - 1]
                it.first[lastFeatureInCut]
            }
    val cutsForAllPoints = rawCutsForAllPoints
            .map { it.second.take(cutSize) }
    return Triple(evaluatedData, cuttingLineY, cutsForAllPoints)
}

class SpacePoint(val point: IntArray) : Iterable<Int>, Comparable<SpacePoint> {
    // wrapper for int array for proper hashcode and compareTo implementation
    override fun compareTo(other: SpacePoint): Int {
        val first = point
        val second = other.point
        if (first.size != second.size) {
            throw IllegalStateException("Trying to compare invalid points")
        }
        for (i in first.indices) {
            if (first[i] < second[i]) {
                return -1
            }
            if (first[i] > second[i]) {
                return 1
            }
        }
        return 0
    }


    override fun iterator(): Iterator<Int> {
        return point.iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpacePoint

        if (!Arrays.equals(point, other.point)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(point)
    }

    constructor(size: Int) : this(IntArray(size))

    operator fun set(index: Int, value: Int) {
        point[index] = value
    }

    fun clone(): SpacePoint = SpacePoint(point.clone())
    val size: Int = point.size
    operator fun get(coord: Int): Int = point[coord]
    override fun toString(): String = Arrays.toString(point)
    fun indices(): IntRange = point.indices
}

const val DOUBLE_SIZE = 8
private fun getChunkSize(cutSize: Int, dataSet: FeatureDataSet, measures: List<KClass<out RelevanceMeasure>>): Int {
    return 5000
    val numberOfFeatures = dataSet.features.size
    logToConsole({ "Number of features: $numberOfFeatures" })
    val allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory

    logToConsole({ "Available memory: ${presumableFreeMemory / 1024 / 1024} mb" })
    val calculatedChunkSize = (presumableFreeMemory / DOUBLE_SIZE / numberOfFeatures / measures.size).toInt()
    logToConsole({ "Calculated chunk size: $calculatedChunkSize" })
    val chunkSize = calculatedChunkSize
    logToConsole({ "Chunk size to use: $chunkSize" })
    return chunkSize
}


fun processAllPointsHd(xDataRaw: List<SpacePoint>,
                       dataSet: FeatureDataSet,
                       measures: List<KClass<out RelevanceMeasure>>,
                       epsilon: Int,
                       cutSize: Int)
        : Map<SpacePoint, RoaringBitmap> {
    val cutsForAllPoints = HashMap<SpacePoint, RoaringBitmap>(xDataRaw.size)
    val chunkSize = getChunkSize(cutSize, dataSet, measures) // to ensure computation is filled in memory
    xDataRaw.chunked(chunkSize)
            .forEach {
                logToConsole({ "Processing chunk of ${it.size} points" })
                processAllPointsHdChunk(it, epsilon, dataSet, measures, cutSize)
                        .forEachIndexed { index, set ->
                            cutsForAllPoints[it[index]] = set
                        }
            }
    return cutsForAllPoints
}

private fun processAllPointsHdChunk(xDataRaw: List<SpacePoint>,
                                    epsilon: Int,
                                    dataSet: FeatureDataSet,
                                    measures: List<KClass<out RelevanceMeasure>>,
                                    cutSize: Int)
        : List<RoaringBitmap> {
    val angles = xDataRaw.map { getAngle(epsilon, it) }
    val xData = angles.map { getPointOnUnitSphere(it) }
    val evaluatedData = getEvaluatedData(xData, dataSet, measures)
    val range = Array(evaluatedData[0].size, { it })
    val evaluatedDataWithNumbers = evaluatedData.map { Pair(it, range.clone()) }
    val rawCutsForAllPoints = evaluatedDataWithNumbers
            .map {
                val featureNumbers = it.second
                val featureMeasures = it.first
                val comparator = kotlin.Comparator<Int> { o1, o2 -> compareValuesBy(o1, o2, { -featureMeasures[it] }) }
                Arrays.sort(featureNumbers, comparator)
                return@map Pair(featureMeasures, featureNumbers)
            } //max first
    return rawCutsForAllPoints
            .map { calculateBitMap(it.second.take(cutSize)) }
}


const val MAX_ANGLE = Math.PI / 2

fun getAngle(epsilon: Int, x: Int): Double {
    val fractionOfPi = MAX_ANGLE / epsilon
    return MAX_ANGLE - (fractionOfPi * x)
}

fun getAngle(epsilon: Int, xs: SpacePoint): DoubleArray {
    val result = DoubleArray(xs.size)
    xs.forEachIndexed { i, x ->
        result[i] = getAngle(epsilon, x)
    }
    return result
}

private val startTime = LocalDateTime.now()


fun logToConsole(msgGetter: () -> String) {
    println(msgGetter())
}