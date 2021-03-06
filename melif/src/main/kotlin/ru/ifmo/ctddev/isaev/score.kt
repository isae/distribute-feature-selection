package ru.ifmo.ctddev.isaev

import java.lang.Double.MIN_VALUE
import java.lang.Double.isNaN

/**
 * @author iisaev
 */
interface Score {
    fun calculate(expected: List<Int>, actual: List<Int>): Double
}

class F1Score : Score {
    override fun calculate(expected: List<Int>, actual: List<Int>): Double {
        if (expected.size != actual.size) {
            throw IllegalArgumentException("Expected and  actual lists must have same size")
        }
        var truePositive = 0
        var trueNegative = 0
        var falsePositive = 0
        var falseNegative = 0
        expected.forEachIndexed { i, ex ->
            val act = actual[i]
            if (ex == 1 && act == 1) {
                ++truePositive
            }
            if (ex == 0 && act == 0) {
                ++trueNegative
            }
            if (ex == 0 && act == 1) {
                ++falsePositive
            }
            if (ex == 1 && act == 0) {
                ++falseNegative
            }
        }
        val precision = truePositive.toDouble() / (truePositive + falsePositive)
        val recall = truePositive.toDouble() / (truePositive + falseNegative)
        var result = 2.0 * precision * recall / (precision + recall)
        result = if (isNaN(result)) MIN_VALUE else result
        return result
    }
}
