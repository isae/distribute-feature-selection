package ru.ifmo.ctddev.isaev.comparison

import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.*
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.relieff.ReliefF
import ru.ifmo.ctddev.isaev.space.FullSpaceScanner
import ru.ifmo.ctddev.isaev.space.calculateTime
import java.io.File
import java.util.*

/**
 * @author iisaev
 */


private val LOGGER = LoggerFactory.getLogger("pqVsFs")

fun main(args: Array<String>) {
    File("results.txt").printWriter().use { out ->
        EnumSet.of(KnownDatasets.ARIZONA5).forEach {
            try {
                val dataSet = it.read()
                val res = processDataSet(dataSet)
                out.println("$it, " +
                        "${res.reliefFTime}, ${res.reliefFScore}, " +
                        "${res.pqVisited}, ${res.pqTime}, ${res.pqScore}, ${res.pqPoint}, " +
                        "${res.fsVisited}, ${res.fsTime}, ${res.fsScore}, ${res.fsPoint}" +
                        "")
                out.flush()
            } catch (e: Exception) {
                LOGGER.error("Some error!", e)
            }
        }
    }
}
/*fun main(args: Array<String>) {
    val dataSet = KnownDatasets.ARIZONA1.read()
    val order = 0.until(dataSet.getInstanceCount()).shuffled()
    val algorithmConfig = AlgorithmConfig(
            0.001,
            SequentalEvaluator(
                    Classifiers.SVM,
                    PreferredSizeFilter(50),
                    OrderSplitter(10, order),
                    F1Score()
            ),
            listOf(VDM(), SpearmanRankCorrelation())
    )
    val (fullSpaceTime, fullSpaceStats) = calculateTime {
        FullSpaceScanner(algorithmConfig, dataSet, 4)
                .run()
    }
    LOGGER.info("""
        FS: processed ${fullSpaceStats.visitedPoints} points in ${fullSpaceTime / 1000} seconds, 
        best result is ${fullSpaceStats.bestResult.score} in point ${fullSpaceStats.bestResult.point}
        """)
}*/

private data class ComparisonResult2d(
        val reliefFTime: Long,
        val reliefFScore: Double,
        val pqVisited: Long,
        val pqTime: Long,
        val pqScore: Double,
        val pqPoint: Point,
        val fsVisited: Long,
        val fsTime: Long,
        val fsScore: Double,
        val fsPoint: Point
)

private fun processDataSet(dataSet: FeatureDataSet): ComparisonResult2d {
    val order = 0.until(dataSet.getInstanceCount()).shuffled()
    val algorithmConfig = AlgorithmConfig(
            0.001,
            SequentalEvaluator(
                    Classifiers.SVM,
                    PreferredSizeFilter(50),
                    OrderSplitter(10, order),
                    F1Score()
            ),
            arrayOf(VDM(), SpearmanRankCorrelation())
    )

    val (reliefFTime, reliefFStats) = calculateTime {
        ReliefF(algorithmConfig, dataSet)
                .run()
    }

    val (fullSpaceTime, fullSpaceStats) = calculateTime {
        FullSpaceScanner(algorithmConfig, dataSet, 4)
                .run()
    }
    val pointsVisitedByFs = fullSpaceStats.visitedPoints.toInt()
    val (pqTime, pqStats) = calculateTime {
        PriorityQueueMeLiF(algorithmConfig, dataSet, 4)
                .run("PqMeLif", pointsVisitedByFs, false)
    }

    LOGGER.info("""
          ReliefF: processed ${reliefFStats.visitedPoints} points in ${reliefFTime / 1000} seconds, 
          best result is ${reliefFStats.bestResult.score} in point ${reliefFStats.bestResult.point}
          """)

    LOGGER.info("""
          PQ: processed ${pqStats.visitedPoints} points in ${pqTime / 1000} seconds, 
          best result is ${pqStats.bestResult.score} in point ${pqStats.bestResult.point}
          """)
    LOGGER.info("""
        FS: processed ${fullSpaceStats.visitedPoints} points in ${fullSpaceTime / 1000} seconds, 
        best result is ${fullSpaceStats.bestResult.score} in point ${fullSpaceStats.bestResult.point}
        """)
    return ComparisonResult2d(
            reliefFTime / 1000,
            reliefFStats.bestResult.score,
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