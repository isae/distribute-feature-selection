package ru.ifmo.ctddev.isaev

import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF
import ru.ifmo.ctddev.isaev.space.FullSpaceScanner
import ru.ifmo.ctddev.isaev.space.calculateTime

/**
 * @author iisaev
 */


private val LOGGER = LoggerFactory.getLogger("pqVsFs")

fun main(args: Array<String>) {
    val dataSet = KnownDatasets.ARIZONA5.read()
    val order = 0.until(dataSet.getInstanceCount()).shuffled()
    val algorithmConfig = AlgorithmConfig(
            0.001,
            SequentalEvaluator(
                    Classifiers.SVM,
                    PreferredSizeFilter(50),
                    OrderSplitter(10, order),
                    F1Score()
            ),
            listOf(VDM::class, SpearmanRankCorrelation::class)
    )
    val (pqTime, pqStats) = calculateTime {
        PriorityQueueMeLiF(algorithmConfig, dataSet, 4)
                .run("PqMeLif", 100)
    }

    val (fullSpaceTime, fullSpaceStats) = calculateTime {
        FullSpaceScanner(algorithmConfig, dataSet, 4)
                .run()
    }
    LOGGER.info("""
          PQ: processed ${pqStats.visitedPoints} points in ${pqTime / 1000} seconds, 
          best result is ${pqStats.bestResult.score} in point ${pqStats.bestResult.point}
          """)
    LOGGER.info("""
        FS: processed ${fullSpaceStats.visitedPoints} points in ${fullSpaceTime / 1000} seconds, 
        best result is ${fullSpaceStats.bestResult.score} in point ${fullSpaceStats.bestResult.point}
        """)

}