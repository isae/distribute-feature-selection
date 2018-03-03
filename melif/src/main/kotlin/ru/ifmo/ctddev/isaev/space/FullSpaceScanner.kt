package ru.ifmo.ctddev.isaev.space

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.*
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
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
        val points = calculateAllPoints(config, featureDataSet, 50)
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

    private fun calculatePoints(): List<Point> {
        val xData = listOf(Point(0.0, 1.0), Point(1.0, 0.0))
        val evaluatedData = getEvaluatedData(xData, featureDataSet, config.measureClasses)
        fun feature(i: Int) = getFeaturePositions(i, evaluatedData)
        val featuresToTake = 50
        val lines = 0.rangeTo(featuresToTake).map { feature(it) }
                .mapIndexed { index, coords -> Line("Feature $index", coords) }
        val intersections = lines.flatMap { first -> lines.map { setOf(first, it) } }
                .filter { it.size > 1 }
                .distinct()
                .map { ArrayList(it) }
                .mapNotNull { it[0].intersect(it[1]) }
                .sortedBy { it.point.x }
        return 0.rangeTo(intersections.size)
                .map { i ->
                    val left = if (i == 0) 0.0 else intersections[i - 1].point.x
                    val right = if (i == intersections.size) 1.0 else intersections[i].point.x
                    (left + right) / 2
                }
                .map { "%.3f".format(it) }
                .distinct()
                .map { it.toDouble() }
                .map { Point(it, 1 - it) }
    }

    fun calculateAllPoints(config: AlgorithmConfig,
                           dataSet: FeatureDataSet,
                           cutSize: Int): List<Point> {
        val discreteSpace = (1.0 / config.delta).toInt()
        fun getPoint(x: Int): Point {
            val first = x.toDouble() / discreteSpace
            val second = (discreteSpace - x).toDouble() / discreteSpace
            return Point(first, second)
        }

        val xData = 0.rangeTo(discreteSpace)
                .map { x -> getPoint(x) }
        val measures = config.measures.map { it.javaClass.kotlin }
        val (evaluatedData, cuttingLineY, cutsForAllPoints) = processAllPointsFast(xData, dataSet, measures, cutSize)
        val lastFeatureInAllCuts = cutsForAllPoints.map { it.last() }
        val lastFeatureInCutSwitchPositions = lastFeatureInAllCuts
                .mapIndexed { index, i -> Pair(index, i) }
                .filter { it.first == 0 || it.second != lastFeatureInAllCuts[it.first - 1] }
                .map { it.first }
        return lastFeatureInCutSwitchPositions
                .map { x -> getPoint(x) }
    }


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