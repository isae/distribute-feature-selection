package ru.ifmo.ctddev.isaev.space

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.AlgorithmConfig
import ru.ifmo.ctddev.isaev.DataSet
import ru.ifmo.ctddev.isaev.DataSetEvaluator
import ru.ifmo.ctddev.isaev.SelectionResult
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * @author iisaev
 */
class FullSpaceScanner(val config: AlgorithmConfig, val dataSet: DataSet, threads: Int) {

    private val logger: Logger = LoggerFactory.getLogger(FullSpaceScanner::class.java)

    private val foldsEvaluator = config.foldsEvaluator

    private val name = "FullSpaceScanner"

    private val executorService = Executors.newFixedThreadPool(threads)

    private val featureDataSet = dataSet.toFeatureSet()

    fun run(): RunStats {
        val points = calculatePoints();
        val runStats = RunStats(config, dataSet, name)
        logger.info("Started {} at {}", name, runStats.startTime)
        logger.info("Started {} at {}", javaClass.simpleName, runStats.startTime)
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
        val xData = listOf(listOf(0.0, 1.0), listOf(1.0, 0.0))
        val valuesForEachMeasure = DataSetEvaluator().evaluateMeasures(featureDataSet, config.measureClasses)
        val evaluatedData = getEvaluatedData(xData, featureDataSet, valuesForEachMeasure)
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
}