package ru.ifmo.ctddev.isaev

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.IntStream

/**
 * @author iisaev
 */
abstract class DataSetSplitter(val testPercent: Int) {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    init {
        logger.info("Initialized dataset splitter with test percent $testPercent")
    }

    abstract fun split(original: DataSet): List<DataSetPair>
}

class OrderSplitter(testPercent: Int, val order: List<Int>) : DataSetSplitter(testPercent) {
    init {
        if (logger.isTraceEnabled) {
            logger.trace("Initialized dataset splitter with order $order")
        }
    }

    override fun split(original: DataSet): List<DataSetPair> {
        val folds = (100.0 / testPercent).toInt()
        val instancesBeforeShuffle = ArrayList(original.toInstanceSet().instances)
        val instances: MutableList<DataInstance> = ArrayList()
        order.forEach { i -> instances.add(instancesBeforeShuffle[i]) }
        val results = ArrayList<ArrayList<DataInstance>>()
        IntStream.range(0, folds).forEach { results.add(ArrayList()) }
        val pos = intArrayOf(0)
        instances.forEach { inst ->
            results[pos[0]].add(inst)
            pos[0] = (pos[0] + 1) % folds
        }
        val result = (0 until folds).map {
            val train = ArrayList<DataInstance>()
            val test = ArrayList<DataInstance>()
            for (j in 0 until folds) {
                if (it != j) {
                    train.addAll(results[j])
                } else {
                    test.addAll(results[j])
                }
            }
            DataSetPair(InstanceDataSet(train), InstanceDataSet(test))
        }
        if (result.size != folds) {
            throw IllegalStateException("Invalid split")
        }
        return result
    }
}

class RandomSplitter(private val testPercent: Int,
                     private val times: Int) {
    private val random = Random()

    fun split(original: DataSet): List<DataSetPair> {
        return (0 until times).map { splitRandomly(original) }
    }

    private fun splitRandomly(original: DataSet): DataSetPair {
        val testInstanceNumber = (original.getInstanceCount().toDouble() * testPercent).toInt() / 100
        val selectedInstances = HashSet<Int>()
        while (selectedInstances.size != testInstanceNumber) {
            selectedInstances.add(random.nextInt(original.getInstanceCount()))
        }
        val instanceSet = original.toInstanceSet()
        val trainInstances = ArrayList<DataInstance>(original.getInstanceCount() - testInstanceNumber)
        val testInstances = ArrayList<DataInstance>(testInstanceNumber)
        IntStream.range(0, original.getInstanceCount()).forEach { i ->
            if (selectedInstances.contains(i)) {
                testInstances.add(instanceSet.instances[i])
            } else {
                trainInstances.add(instanceSet.instances[i])
            }
        }
        return DataSetPair(InstanceDataSet(trainInstances), InstanceDataSet(testInstances))
    }
}
