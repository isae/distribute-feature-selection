package ru.ifmo.ktddev.isaev

import org.slf4j.LoggerFactory
import ru.ifmo.ctddev.isaev.result.Point
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.lang.Double.compare

/**
 * @author iisaev
 */
class AlgorithmConfig(val delta: Double,
                      val foldsEvaluator: FoldsEvaluator,
                      val measures: Array<RelevanceMeasure>)

class EvaluatedFeature(feature: Feature, val measure: Double)
    : Feature(feature.name, feature.values)

class SelectionResult(val selectedFeatures: List<Feature>,
                      val point: Point,
                      val score: Double) : Comparable<SelectionResult> {

    override fun compareTo(other: SelectionResult): Int {
        return compare(score, other.score)
    }

    fun betterThan(bestScore: SelectionResult): Boolean {
        return this.compareTo(bestScore) == 1
    }
}

class DataSetReader {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun readCsv(path: String): FeatureDataSet {
        return readDataSet(File(path), ",")
    }

    fun readCsv(file: File): FeatureDataSet {
        return readDataSet(file, ",")
    }

    private fun readDataSet(file: File, delimiter: String): FeatureDataSet {
        fun parseRow(line: String): List<Int> {
            return line.split(delimiter.toRegex())
                    .map({ Integer.valueOf(it) })
        }
        try {
            val reader = BufferedReader(FileReader(file))
            val counter = intArrayOf(0)
            val goodLines = Sequence { reader.lines().iterator() }
                    .filter { l -> !l.contains("NaN") }
            val classes = parseRow(goodLines.first())
            val features = goodLines
                    .map(::parseRow)
                    .map { it -> Feature("feature ${++counter[0]}", it) }
            val featuresList = features.toList()
            logger.debug("Read dataset by path {}; {} objects; {} features", arrayOf(file.absoluteFile, classes.size, featuresList.size))
            return FeatureDataSet(featuresList, classes, file.name)
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException("File not found: " + file.name, e)
        }

    }
}
