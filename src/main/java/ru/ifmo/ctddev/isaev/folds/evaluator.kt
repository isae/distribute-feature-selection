package ru.ifmo.ctddev.isaev.folds

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.ScoreCalculator
import ru.ifmo.ctddev.isaev.classifier.Classifiers
import ru.ifmo.ctddev.isaev.dataset.DataSet
import ru.ifmo.ctddev.isaev.dataset.DataSetPair
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet
import ru.ifmo.ctddev.isaev.feature.RelevanceMeasure
import ru.ifmo.ctddev.isaev.filter.DataSetFilter
import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm
import ru.ifmo.ctddev.isaev.result.Point
import ru.ifmo.ctddev.isaev.result.RunStats
import ru.ifmo.ctddev.isaev.result.SelectionResult
import ru.ifmo.ctddev.isaev.splitter.DataSetSplitter
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
                              private val scoreCalculator: ScoreCalculator) {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    abstract fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult
    abstract fun getSelectionResultPar(dataSet: DataSet, point: Point, stats: RunStats, executorService: ExecutorService): SelectionResult

    protected fun getF1Score(dsPair: DataSetPair): Double {
        val classifier = classifiers.newClassifier()
        classifier.train(dsPair.trainSet)
        val actual = classifier.test(dsPair.testSet)
                .map { Math.round(it).toInt() }
        val expectedValues = dsPair.testSet.toInstanceSet().instances
                .map { it.clazz }
        val result = scoreCalculator.calculateF1Score(expectedValues, actual)
        if (logger.isTraceEnabled) {
            logger.trace("Expected values: {}", Arrays.toString(expectedValues.toTypedArray()))
            logger.trace("Actual values: {}", Arrays.toString(actual.toTypedArray()))
        }
        return result
    }

    protected fun getF1Score(filteredDs: FeatureDataSet): Double {
        val instanceDataSet = filteredDs.toInstanceSet()
        val f1Scores = dataSetSplitter.split(instanceDataSet)
                .map { this.getF1Score(it) }
        return f1Scores.average()
    }

    protected fun getF1Score(dataSet: DataSet, point: Point, measures: Array<RelevanceMeasure>): Double {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, measures)
        val instanceDataSet = filteredDs.toInstanceSet()
        val f1Scores = dataSetSplitter.split(instanceDataSet)
                .map { this.getF1Score(it) }
        return f1Scores.average()
    }
}

class SequentalEvaluator(classifiers: Classifiers, dataSetFilter: DataSetFilter, dataSetSplitter: DataSetSplitter, scoreCalculator: ScoreCalculator)
    : FoldsEvaluator("Seq", classifiers, dataSetSplitter, dataSetFilter, scoreCalculator) {

    override fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.measures)
        val f1Score = getF1Score(dataSet, point, stats.measures)
        logger.debug("Point {}; F1 score: {}", point, FeatureSelectionAlgorithm.FORMAT.format(f1Score))
        val result = SelectionResult(filteredDs.features, point, f1Score)
        stats.updateBestResult(result)
        return result
    }

    override fun getSelectionResultPar(dataSet: DataSet, point: Point, stats: RunStats, executorService: ExecutorService): SelectionResult {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.measures)
        val instanceDataSet = filteredDs.toInstanceSet()
        val dataSetPairs = dataSetSplitter.split(instanceDataSet)
        val latch = CountDownLatch(dataSetPairs.size)
        val f1Scores = Collections.synchronizedList(ArrayList<Double>(dataSetPairs.size))
        val futures = dataSetPairs.map { ds ->
            executorService.submit {
                val score = getF1Score(ds)
                f1Scores.add(score)
                latch.countDown()
            }
        }
        try {
            latch.await()
            futures.forEach { f -> assert(f.isDone) }
            val f1Score = f1Scores.average()
            logger.debug("Point {}; F1 score: {}", point, f1Score)
            val result = SelectionResult(filteredDs.features, point, f1Score)
            stats.updateBestResult(result)
            return result
        } catch (e: InterruptedException) {
            throw IllegalStateException("Waiting on latch interrupted! ", e)
        }

    }
}

class ParallelEvaluator(classifiers: Classifiers, dataSetFilter: DataSetFilter, dataSetSplitter: DataSetSplitter,
                        private val executorService: ExecutorService, scoreCalculator: ScoreCalculator)
    : FoldsEvaluator("Par", classifiers, dataSetSplitter, dataSetFilter, scoreCalculator) {

    constructor(classifiers: Classifiers, dataSetFilter: DataSetFilter, datasetSplitter: DataSetSplitter, scoreCalculator: ScoreCalculator, threads: Int)
            : this(classifiers, dataSetFilter, datasetSplitter, Executors.newFixedThreadPool(threads), scoreCalculator)

    override fun getSelectionResult(dataSet: DataSet, point: Point, stats: RunStats): SelectionResult {
        val filteredDs = dataSetFilter.filterDataSet(dataSet.toFeatureSet(), point, stats.measures)
        val instanceDataSet = filteredDs.toInstanceSet()
        val dataSetPairs = dataSetSplitter.split(instanceDataSet)
        val latch = CountDownLatch(dataSetPairs.size)
        val f1Scores = Collections.synchronizedList(ArrayList<Double>(dataSetPairs.size))
        val futures = dataSetPairs
                .map { ds ->
                    executorService.submit {
                        val score = getF1Score(ds)
                        f1Scores.add(score)
                        latch.countDown()
                    }
                }
        try {
            latch.await()
            futures.forEach { f -> assert(f.isDone) }
            val f1Score = f1Scores.average()
            logger.debug("Point $point; F1 score: $f1Score")
            val result = SelectionResult(filteredDs.features, point, f1Score)
            stats.updateBestResult(result)
            return result
        } catch (e: InterruptedException) {
            throw IllegalStateException("Waiting on latch interrupted! ", e)
        }

    }

    override fun getSelectionResultPar(dataSet: DataSet, point: Point, stats: RunStats, executorService: ExecutorService): SelectionResult {
        throw UnsupportedOperationException("Method is not implemented")
    }
}
