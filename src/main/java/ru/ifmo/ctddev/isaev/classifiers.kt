package ru.ifmo.ctddev.isaev

import weka.classifiers.AbstractClassifier
import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.functions.LinearRegression
import weka.classifiers.functions.MultilayerPerceptron
import weka.classifiers.functions.SMO
import weka.classifiers.lazy.IBk
import weka.classifiers.rules.PART
import weka.classifiers.trees.HoeffdingTree
import weka.classifiers.trees.J48
import weka.classifiers.trees.LMT
import weka.classifiers.trees.RandomForest
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances
import java.math.BigInteger
import java.util.*

/**
 * @author iisaev
 */
private val random = Random()

fun nextAttrName(): String {
    return BigInteger(130, random).toString(32).substring(0, 8)
}

fun toInstances(ds: InstanceDataSet): Instances {
    val attrInfo = ArrayList<Attribute>()
    attrInfo.add(Attribute("Classes", Arrays.asList("0", "1")))
    ds.instances[0].values.forEach { attrInfo.add(Attribute(nextAttrName())) }
    val result = Instances(ds.name, attrInfo, ds.instances.size)
    result.setClassIndex(0)
    ds.instances.forEach { inst ->
        val instance = DenseInstance(inst.values.size + 1)
        instance.setDataset(result)
        instance.setValue(0, inst.clazz.toString())
        0.rangeTo(inst.values.size)
                .forEach { instance.setValue(it + 1, inst.values[it].toDouble()) }
        result.add(instance)
    }
    return result
}

fun toInstance(instance: DataInstance): Instance {
    val result = DenseInstance(instance.values.size + 1)
    result.setMissing(0)
    0.rangeTo(instance.values.size)
            .forEach { result.setValue(it + 1, instance.values[it].toDouble()) }
    return result
}

interface Classifier {
    fun train(trainDs: DataSet): TrainedClassifier
}

class TrainedClassifier(private val classifier: AbstractClassifier,
                        private val instances: Instances) {
    fun test(testDs: DataSet): List<Double> {
        return testDs.toInstanceSet().instances
                .map { toInstance(it) }
                .map { i ->
                    try {
                        i.setDataset(instances)
                        return testDs.toInstanceSet().instances
                                .map { toInstance(it) }
                                .map { classifier.classifyInstance(it) }
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Given dataset is not valid", e)
                    }
                }
    }
}

sealed class WekaClassifier : Classifier {

    protected abstract fun createClassifier(): AbstractClassifier

    override fun train(trainDs: DataSet): TrainedClassifier {
        try {
            val instances = toInstances(trainDs.toInstanceSet())
            val classifier = createClassifier()
            classifier.buildClassifier(instances)
            return TrainedClassifier(classifier, instances)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to train on given dataset", e)
        }
    }
}

class WekaHoeffdingTree : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = HoeffdingTree()
}

class WekaJ48 : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = J48()
}

class WekaKNN : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = IBk()
}

class WekaLinearRegression : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = LinearRegression()
}

class WekaLMT : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = LMT()
}

class WekaMultilayerPerceptron : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = MultilayerPerceptron()
}

class WekaPART : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = PART()
}

class WekaRandomForest : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = RandomForest()
}

class WekaSVM : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier {
        val classifier = SMO()
        classifier.buildLogisticModels = true
        return classifier
    }
}

class WekaNaiveBayes : WekaClassifier() {
    override fun createClassifier(): AbstractClassifier = NaiveBayes()
}

enum class Classifiers(private val typeToken: Class<out Classifier>) {
    HOEFD(WekaHoeffdingTree::class.java),
    J48(WekaJ48::class.java),
    KNN(WekaKNN::class.java),
    LMT(WekaLMT::class.java),
    PERCEPTRON(WekaMultilayerPerceptron::class.java),
    PART(WekaPART::class.java),
    RAND_FOREST(WekaRandomForest::class.java),
    SVM(WekaSVM::class.java),
    NAIVE_BAYES(WekaNaiveBayes::class.java);

    fun newClassifier(): Classifier {
        try {
            return typeToken.newInstance()
        } catch (e: InstantiationException) {
            throw IllegalStateException("Failed to instantiate Classifier")
        } catch (e: IllegalAccessException) {
            throw IllegalStateException("Failed to instantiate Classifier")
        }

    }
}