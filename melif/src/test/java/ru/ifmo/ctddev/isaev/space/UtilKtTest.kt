package ru.ifmo.ctddev.isaev.space

import org.junit.Test
import java.lang.Math.PI
import java.lang.Math.abs
import java.util.*


/**
 * @author iisaev
 */
class UtilKtTest {
    private val EPSILON = 1E-08

    @Test
    fun testGetPointOnUnitSphere() {
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 1.0), getPointOnUnitSphere(doubleArrayOf(0.0, 0.0)).coordinates)
        assertArrayEquals(doubleArrayOf(1.0, 0.0, 0.0), getPointOnUnitSphere(doubleArrayOf(0.0, PI / 2)).coordinates)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 1.0), getPointOnUnitSphere(doubleArrayOf(PI / 2, 0.0)).coordinates)
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 0.0), getPointOnUnitSphere(doubleArrayOf(PI / 2, PI / 2)).coordinates)
    }

    private fun assertArrayEquals(expected: DoubleArray, actual: DoubleArray) {
        if (!expected.indices.all { abs(expected[it] - actual[it]) < EPSILON }) {
            throw AssertionError("\nExpected: ${Arrays.toString(expected)}\nActual: $${Arrays.toString(actual)}")
        }
    }
}