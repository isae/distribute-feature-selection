package ru.ifmo.ctddev.isaev

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author iisaev
 */
abstract class FoldsEvaluator(val name: String,
                              val classifiers: Classifiers,
                              val dataSetSplitter: DataSetSplitter,
                              val dataSetFilter: DataSetFilter,
                              private val score: Score) {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    abstract fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult

    public fun getScore(dsPair: DataSetPair): Double {
        val classifier = classifiers.newClassifier()
        val trained = classifier.train(dsPair.trainSet)
        val actual = trained.test(dsPair.testSet)
                .map { Math.round(it).toInt() }
        val expectedValues = dsPair.testSet.toInstanceSet().instances
                .map { it.clazz }
        val result = score.calculate(expectedValues, actual)
        if (logger.isTraceEnabled) {
            logger.trace("Expected values: {}", Arrays.toString(expectedValues.toTypedArray()))
            logger.trace("Actual values: {}", Arrays.toString(actual.toTypedArray()))
        }
        return result
    }

    protected fun getScore(dataSet: DataSet, 
                           point: Point, measures: Array<RelevanceMeasure>): Double {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, measures)
        val instanceDataSet = filteredDs.toInstanceSet()
        val splits = dataSetSplitter.split(instanceDataSet)
        val f1Scores = splits
                .map { this.getScore(it) }
        return f1Scores.average()
    }
}

class SequentalEvaluator(classifiers: Classifiers, dataSetFilter: DataSetFilter, dataSetSplitter: DataSetSplitter, score: Score)
    : FoldsEvaluator("Seq", classifiers, dataSetSplitter, dataSetFilter, score) {

    override fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.measures)
        val f1Score = getScore(dataSet, point, stats.measures)
        logger.debug("Point {}; F1 score: {}", point, FeatureSelectionAlgorithm.FORMAT.format(f1Score))
        val result = SelectionResult(filteredDs.features, point, f1Score)
        stats.updateBestResult(result)
        return result
    }
}

class ParallelEvaluator(classifiers: Classifiers, dataSetFilter: DataSetFilter, dataSetSplitter: DataSetSplitter,
                        private val executorService: ExecutorService, score: Score)
    : FoldsEvaluator("Par", classifiers, dataSetSplitter, dataSetFilter, score) {

    constructor(classifiers: Classifiers, dataSetFilter: DataSetFilter, datasetSplitter: DataSetSplitter, score: Score, threads: Int)
            : this(classifiers, dataSetFilter, datasetSplitter, Executors.newFixedThreadPool(threads), score)

    override fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.measures)
        val instanceDataSet = filteredDs.toInstanceSet()
        val dataSetPairs = dataSetSplitter.split(instanceDataSet)
        val latch = CountDownLatch(dataSetPairs.size)
        val f1Scores = Collections.synchronizedList(ArrayList<Double>(dataSetPairs.size))
        val futures = dataSetPairs
                .map { ds ->
                    executorService.submit {
                        val score = getScore(ds)
                        f1Scores.add(score)
                        latch.countDown()
                    }
                }
        try {
            latch.await()
            futures.forEach { f ->
                if (!f.isDone) {
                    throw IllegalStateException("Task is not done after latch is released")
                }
            }
            val f1Score = f1Scores.average()
            logger.debug("Point $point; F1 score: $f1Score")
            val result = SelectionResult(filteredDs.features, point, f1Score)
            stats.updateBestResult(result)
            return result
        } catch (e: InterruptedException) {
            throw IllegalStateException("Waiting on latch interrupted! ", e)
        }

    }
}
