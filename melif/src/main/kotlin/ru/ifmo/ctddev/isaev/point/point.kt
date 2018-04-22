package ru.ifmo.ctddev.isaev.point

import ru.ifmo.ctddev.isaev.melif.impl.FeatureSelectionAlgorithm
import java.util.*
import java.util.function.Consumer
import java.util.stream.DoubleStream
import java.util.stream.IntStream

/**
 * @author iisaev
 */
open class Point : Comparable<Point> {
    companion object {
        fun fromRawCoords(vararg coordinates: Double): Point {
            return Point(true, *coordinates)
        }
    }

    val coordinates: DoubleArray

    private val generation: Int

    constructor(vararg coordinates: Double) : this(0, *coordinates)

    constructor(generation: Int,
                vararg coordinates: Double) : this(generation, Consumer {}, *coordinates)

    constructor(generation: Int,
                modifyCoordinates: Consumer<DoubleArray>,
                vararg coordinates: Double) {
        this.coordinates = coordinates.clone()
        modifyCoordinates.accept(this.coordinates)
        val modulus = DoubleStream.of(*coordinates).sum()
        IntStream.range(0, coordinates.size).forEach { i -> this.coordinates[i] /= modulus } // normalization
        this.generation = generation
    }

    constructor(point: Point) : this(*point.coordinates.clone())

    private constructor(unused: Boolean, vararg coordinates: Double) {
        this.coordinates = coordinates
        this.generation = -1
    }

    constructor(point: Point,
                modifyCoordinates: (DoubleArray) -> Unit) {
        this.coordinates = point.coordinates.clone()
        modifyCoordinates(coordinates)
        //double modulus = Math.sqrt(DoubleStream.of(coordinates).map(d -> d * d).sum());
        val modulus = DoubleStream.of(*coordinates).sum()
        IntStream.range(0, coordinates.size).forEach { i -> this.coordinates[i] /= modulus } // normalization
        this.generation = point.generation + 1
    }

    override fun compareTo(other: Point): Int {
        for (i in coordinates.indices) {
            if (coordinates[i] - other.coordinates[i] > 0.00001) {
                return 1
            }
            if (coordinates[i] - other.coordinates[i] < -0.00001) {
                return -1
            }
        }
        return 0
    }

    override fun toString(): String {
        val doubles = coordinates.map { FeatureSelectionAlgorithm.FORMAT.format(it) }
        return "[" + doubles.joinToString(", ") + "]/" + if (generation > 0) generation else ""
    }

    fun getNeighbours(delta: Double): List<Point> {
        val points = ArrayList<Point>()
        IntStream.range(0, coordinates.size - 1).forEach { i ->
            val plusDelta = Point(this, { coords -> coords[i] += delta })
            val minusDelta = Point(this, { coords -> coords[i] -= delta })
            points.add(plusDelta)
            points.add(minusDelta)
        }
        return points
    }

    operator fun get(i: Int): Double = coordinates[i]
}

class PriorityPoint(var priority: Double, p: Point) : Point(p) {

    constructor(value: Point) : this(1.0, value)
}