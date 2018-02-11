package ru.ifmo.ctddev.isaev.melif.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.AlgorithmConfig
import ru.ifmo.ctddev.isaev.DataSet
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors

/**
 * @author iisaev
 */
class SimplePointsRunner(config: AlgorithmConfig, dataSet: DataSet) : BasicMeLiF(config, dataSet) {

    private val LOGGER: Logger = LoggerFactory.getLogger(SimplePointsRunner::class.java)

    private val executorService = Executors.newFixedThreadPool(4);

    override fun run(points: Array<out Point>): RunStats {
        return run("SimpleRunner", points, -1)
    }

    override fun run(name: String, points: Array<out Point>, pointsToVisit: Int): RunStats {
        Arrays.asList(*points).forEach { p ->
            if (p.coordinates.size != config.measures.size) {
                throw IllegalArgumentException("Each point must have same coordinates number as number of measures")
            }
        }

        val runStats = RunStats(config, dataSet, name)
        logger.info("Started {} at {}", name, runStats.startTime)
        LOGGER.info("Started {} at {}", javaClass.simpleName, runStats.startTime)
        val scoreFutures = Arrays.asList(*points).stream()
                .map { p ->
                    executorService.submit({ performCoordinateDescend(p, runStats) })
                }
        val results = scoreFutures.map { it.get() }

        LOGGER.info("Total scores: ")
        /*scores.mapToDouble { it.getScore() }
                .forEach { println(it) }*/
        LOGGER.info("Max score: {} at point {}",
                runStats.bestResult.score,
                runStats.bestResult.point
        )
        runStats.finishTime = LocalDateTime.now()
        LOGGER.info("Finished {} at {}", javaClass.simpleName, runStats.finishTime)
        LOGGER.info("Working time: {} seconds", runStats.workTime)
        executorService.shutdown()
        return runStats
    }
}