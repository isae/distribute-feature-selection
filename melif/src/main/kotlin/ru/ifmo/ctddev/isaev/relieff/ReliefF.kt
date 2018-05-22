package ru.ifmo.ctddev.isaev.relieff

import ru.ifmo.ctddev.isaev.*
import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.results.RunStats
import weka.attributeSelection.ReliefFAttributeEval

/**
 * @author iisaev
 */
class ReliefF(config: AlgorithmConfig, dataSet: DataSet) : FeatureSelectionAlgorithm(config, dataSet) {

    fun run(): RunStats {
        val runStats = RunStats(config, dataSet, javaClass.simpleName)
        val reliefF = ReliefFAttributeEval()
        val instanceDataSet = dataSet.toInstanceSet()
        val featureDataSet = dataSet.toFeatureSet()
        reliefF.buildEvaluator(toInstances(instanceDataSet))

        val featuresSortedByReliefFValue = featureDataSet.features.indices
                .map { it -> Pair(featureDataSet.features[it], reliefF.evaluateAttribute(it)) }
                .sortedBy { (_, score) -> -score }
        val selectedFeatures = featuresSortedByReliefFValue
                .map { (f, _) -> f }
                .take(50)
        val filteredDs = FeatureDataSet(
                selectedFeatures,
                featureDataSet.classes,
                "${featureDataSet.name} filtered by ReliefF"
        )


        val foldsEvaluator = config.foldsEvaluator
        val splits = foldsEvaluator.dataSetSplitter.split(filteredDs)
        val f1Score = splits
                .map { foldsEvaluator.getScore(it) }
                .average()

        val result = SelectionResult(selectedFeatures, Point.fromRawCoords(1.0), f1Score)
        runStats.updateBestResult(result)
        return runStats
    }
}