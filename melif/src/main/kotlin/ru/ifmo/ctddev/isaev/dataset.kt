package ru.ifmo.ctddev.isaev

import java.util.*

/**
 * @author iisaev
 */
abstract class DataSet(val name: String) {

    abstract fun getFeatureCount(): Int

    abstract fun getInstanceCount(): Int

    abstract fun toFeatureSet(): FeatureDataSet

    abstract fun toInstanceSet(): InstanceDataSet
}


class DataInstance(val name: String,
                   val clazz: Int,
                   val values: List<Int>) {

    constructor(clazz: Int, values: List<Int>) : this("", clazz, values)

    override fun toString() = name + ": " + clazz
}

class DataSetPair(val trainSet: DataSet, val testSet: DataSet)

open class Feature(val name: String,
                   val values: List<Int>) {

    constructor(values: List<Int>) : this("", values)

    override fun toString() = name
}

class FeatureDataSet(val features: List<Feature>,
                     val classes: List<Int>,
                     name: String) : DataSet(name) {
    init {
        if (!classes.stream().allMatch { i -> i == 0 || i == 1 }) {
            throw IllegalArgumentException("All classes values should be 0 or 1")
        }
        features.forEach { f ->
            if (f.values.size != classes.size) {
                throw IllegalArgumentException("ru.ifmo.ctddev.isaev.Feature ${f.name} has wrong number of values")
            }
        }
    }

    override fun toFeatureSet() = this

    override fun toInstanceSet(): InstanceDataSet {
        val instances = (0 until classes.size)
                .map {
                    val values = ArrayList<Int>()
                    features.forEach { feature -> values.add(feature.values[it]) }
                    DataInstance("instance ${it + 1}", classes[it], values)
                }
        return InstanceDataSet(instances)
    }

    fun take(size: Int): FeatureDataSet {
        return FeatureDataSet(features.take(size), classes, name)
    }

    override fun getFeatureCount(): Int {
        return features.size
    }

    override fun getInstanceCount(): Int {
        return classes.size
    }
}

class InstanceDataSet(val instances: List<DataInstance>) : DataSet("") {
    override fun toFeatureSet(): FeatureDataSet {
        val classes = instances.map { it.clazz }
        val features = (0 until instances[0].values.size)
                .map { Feature(instances.map { inst -> inst.values[it] }) }
        return FeatureDataSet(features, classes, name)
    }

    override fun toInstanceSet(): InstanceDataSet {
        return this
    }

    override fun getFeatureCount(): Int {
        return instances[0].values.size
    }

    override fun getInstanceCount(): Int {
        return instances.size
    }
}
