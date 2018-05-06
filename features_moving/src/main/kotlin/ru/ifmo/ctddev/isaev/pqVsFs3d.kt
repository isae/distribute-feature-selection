package ru.ifmo.ctddev.isaev

import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF
import ru.ifmo.ctddev.isaev.space.FullSpaceScanner
import ru.ifmo.ctddev.isaev.space.calculateTime
import java.io.File
import java.util.*

/**
 * @author iisaev
 */

private val LOGGER = LoggerFactory.getLogger("pqVsFs3d")

fun main(args: Array<String>) {
    File("results.txt").printWriter().use { out ->
        EnumSet.allOf(KnownDatasets::class.java).forEach {
            try {
                val dataSet = it.read()
                val res = processDataSet(dataSet)
                out.println("$it, ${res.pqVisited}, ${res.pqTime}, ${res.pqScore}, ${res.pqPoint}, ${res.fsVisited}, ${res.fsTime}, ${res.fsScore}, ${res.fsPoint}")
                out.flush()
            } catch (e: Exception) {
                LOGGER.error("Some error!", e)
            }
        }
    }
}

private fun processDataSet(dataSet: FeatureDataSet): ComparisonResult {
    val order = 0.until(dataSet.getInstanceCount()).shuffled()
    val algorithmConfig = AlgorithmConfig(
            0.001,
            SequentalEvaluator(
                    Classifiers.SVM,
                    PreferredSizeFilter(50),
                    OrderSplitter(10, order),
                    F1Score()
            ),
            listOf(VDM::class, SpearmanRankCorrelation::class, SymmetricUncertainty::class)
    )

    val (fullSpaceTime, fullSpaceStats) = calculateTime {
        FullSpaceScanner(algorithmConfig, dataSet, 4)
                .run()
    }
    val (pqTime, pqStats) = calculateTime {
        PriorityQueueMeLiF(algorithmConfig, dataSet, 4)
                .run("PqMeLif", 100)
    }
    LOGGER.info("""
          PQ: processed ${pqStats.visitedPoints} points in ${pqTime / 1000} seconds, 
          best result is ${pqStats.bestResult.score} in point ${pqStats.bestResult.point}
          """)
    LOGGER.info("""
        FS: processed ${fullSpaceStats.visitedPoints} points in ${fullSpaceTime / 1000} seconds, 
        best result is ${fullSpaceStats.bestResult.score} in point ${fullSpaceStats.bestResult.point}
        """)
    return ComparisonResult(
            pqStats.visitedPoints,
            pqTime / 1000,
            pqStats.bestResult.score,
            pqStats.bestResult.point,
            fullSpaceStats.visitedPoints,
            fullSpaceTime / 1000,
            fullSpaceStats.bestResult.score,
            fullSpaceStats.bestResult.point
    )
}