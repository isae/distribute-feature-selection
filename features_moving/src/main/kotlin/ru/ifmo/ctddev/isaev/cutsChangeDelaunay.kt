package ru.ifmo.ctddev.isaev

import org.locationtech.jts.geom.*
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex
import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.evaluateDataSet
import ru.ifmo.ctddev.isaev.space.logToConsole
import ru.ifmo.ctddev.isaev.space.processPoint
import java.util.*


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDs = evaluateDataSet(dataSet, measures)
val range = Array(evaluatedDs[0].size, { it })

fun main(args: Array<String>) {
    val envelope = Envelope(Coordinate(-1.0, -1.0), Coordinate(2.0, 2.0))
    val subDiv = QuadEdgeSubdivision(envelope, 1E-6)
    val delaunay = IncrementalDelaunayTriangulator(subDiv)
    delaunay.insertSite(Vertex(0.0, 0.0))
    delaunay.insertSite(Vertex(0.0, 1.0))
    delaunay.insertSite(Vertex(1.0, 0.0))
    delaunay.insertSite(Vertex(1.0, 1.0))
    val pointCache = TreeMap<Point, RoaringBitmap>()
    val geomFact = GeometryFactory()
    val range = Array(evaluatedDs[0].size, { it })
    do {
        val isChanged = performEnrichment(delaunay, pointCache, geomFact, subDiv)
    } while (isChanged)

    val pointsToTry = pointCache.keys
    println("Found ${pointsToTry.size} points to try with enrichment")
    //pointsToTry.forEach { println(it) }
}

fun performEnrichment(delaunay: IncrementalDelaunayTriangulator,
                      cache: TreeMap<Point, RoaringBitmap>,
                      geomFact: GeometryFactory,
                      subDiv: QuadEdgeSubdivision): Boolean {
    val trianglesGeom = subDiv.getTriangles(geomFact) as GeometryCollection
    val allTriangles = 0.until(trianglesGeom.numGeometries)
            .map { trianglesGeom.getGeometryN(it) }
            .map { Triangle(it.coordinates[0], it.coordinates[1], it.coordinates[2]) }
    var isChanged = false
    allTriangles.onEach {
        val p0 = Point.fromRawCoords(it.p0.x, it.p0.y)
        val cut0 = cache.computeIfAbsent(p0, { processPoint(it, evaluatedDs, cutSize, range) })
        val p1 = Point.fromRawCoords(it.p1.x, it.p1.y)
        val cut1 = cache.computeIfAbsent(p1, { processPoint(it, evaluatedDs, cutSize, range) })
        val p2 = Point.fromRawCoords(it.p2.x, it.p2.y)
        val cut2 = cache.computeIfAbsent(p2, { processPoint(it, evaluatedDs, cutSize, range) })

        val cut01 = cut0.clone()
        cut01.andNot(cut1)
        val cut12 = cut1.clone()
        cut12.andNot(cut2)
        val cut02 = cut0.clone()
        cut02.andNot(cut2)

        val center = getCenter(p0, p1, p2)
        val centerCut = cache.computeIfAbsent(center, { processPoint(it, evaluatedDs, cutSize, range) })
        if (centerCut != cut0 && centerCut != cut1 && centerCut != cut2) {
            if (allTriangles.size > 20000) {
                logToConsole { "Cut 0 equals to cut 1: ${cut0 == cut1} $cut01" }
                logToConsole { "Cut 1 equals to cut 2: ${cut1 == cut2} $cut12" }
                logToConsole { "Cut 0 equals to cut 2: ${cut0 == cut2} $cut02" }
            }
            delaunay.insertSite(Vertex(center.coordinates[0], center.coordinates[1]))
            isChanged = true
        } else {
            //logToConsole { "Something is equal to something" }
        }
    }
    logToConsole { "Finished iteration: found ${allTriangles.size} polygons" }
    return isChanged
}

fun getCenter(vararg points: Point): Point {
    val coords = DoubleArray(points[0].coordinates.size)
    coords.indices.forEach { i ->
        coords[i] = points.map { it.coordinates[i] }.sum() / points.size
    }
    return Point.fromRawCoords(*coords)
}