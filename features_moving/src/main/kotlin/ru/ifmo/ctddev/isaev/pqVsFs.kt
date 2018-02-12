package ru.ifmo.ctddev.isaev

import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.melif.impl.PriorityQueueMeLiF
import ru.ifmo.ctddev.isaev.space.FullSpaceScanner

/**
 * @author iisaev
 */


private val LOGGER = LoggerFactory.getLogger("pqVsFs")

fun main(args: Array<String>) {
    val dataSet = DataSetReader().readCsv(args[0])
    val order = 0.until(dataSet.getInstanceCount()).shuffled()
    val algorithmConfig = AlgorithmConfig(
            0.1,
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
    LOGGER.info("PQ: processed ${pqStats.visitedPoints} points in ${pqTime / 1000} seconds, best result is ${pqStats.bestResult.score}")
    LOGGER.info("FS: processed ${fullSpaceStats.visitedPoints} points in ${fullSpaceTime / 1000} seconds, best result is ${fullSpaceStats.bestResult.score}")

}

fun <T> calculateTime(block: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}