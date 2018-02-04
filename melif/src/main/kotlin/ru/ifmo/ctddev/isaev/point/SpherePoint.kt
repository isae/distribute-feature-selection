package ru.ifmo.ctddev.isaev.point

/**
 * @author iisaev
 */
fun fromSpherical(sphericalPoint: Point): Point {
    return Point(*fromSpherical(sphericalPoint.coordinates))
}

fun fromSpherical(spherical: DoubleArray): DoubleArray {
    return spherical.plus(1.0)
}

fun toSpherical(euclideanPoint: Point): Point {
    return Point(*toSpherical(euclideanPoint.coordinates))
}

fun toSpherical(euclidean: DoubleArray): DoubleArray {
    val n = euclidean.size
    return (0 until n - 1)
            .map { euclidean[it] / euclidean[n - 1] }
            .toDoubleArray()
}