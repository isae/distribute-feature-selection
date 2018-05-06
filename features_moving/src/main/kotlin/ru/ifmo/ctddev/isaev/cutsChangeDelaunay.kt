package ru.ifmo.ctddev.isaev

import org.locationtech.jts.geom.*
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex
import org.roaringbitmap.RoaringBitmap
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.evaluateDataSet
import ru.ifmo.ctddev.isaev.space.logToConsole
import ru.ifmo.ctddev.isaev.space.processPoint
import java.util.*


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, SymmetricUncertainty::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDs = evaluateDataSet(dataSet, measures)
val range = Array(evaluatedDs[0].size, { it })

interface CutCache {

    fun compute(point: Point, function: (Point) -> RoaringBitmap): RoaringBitmap

    fun getAll(): Collection<Point>
}

class MapCache : CutCache {
    private val cache = TreeMap<Point, RoaringBitmap>()

    override fun getAll(): Collection<Point> = cache.keys
    override fun compute(point: Point, function: (Point) -> RoaringBitmap): RoaringBitmap {
        return cache.computeIfAbsent(point, function)
    }
}

class NopCache : CutCache {
    override fun getAll(): Collection<Point> = emptyList()

    override fun compute(point: Point, function: (Point) -> RoaringBitmap): RoaringBitmap {
        return function(point)
    }
}

fun main(args: Array<String>) {
    val max = 700.0
    val envelope = Envelope(Coordinate(0.0, 0.0), Coordinate(max, max))
    val subDiv = QuadEdgeSubdivision(envelope, 1E-6)
    val delaunay = IncrementalDelaunayTriangulator(subDiv)
    delaunay.insertSite(Vertex(0.0, 0.0))
    delaunay.insertSite(Vertex(0.0, max))
    delaunay.insertSite(Vertex(max, 0.0))
    delaunay.insertSite(Vertex(max, max))
    val pointCache = MapCache()
    val geomFact = GeometryFactory()
    val range = Array(evaluatedDs[0].size, { it })
    do {
        val isChanged = performEnrichment(delaunay, pointCache, geomFact, subDiv)
    } while (isChanged)

    val pointsToTry = pointCache.getAll()
    println("Found ${pointsToTry.size} points to try with enrichment")
    visualizeDelaunay(envelope, subDiv, geomFact, delaunay)
    //pointsToTry.forEach { println(it) }
}

fun process(coord: Coordinate, cache: CutCache): RoaringBitmap {
    val point = Point(coord.x, coord.y, 1.0) //conversion from homogeneous to euclidean
    return cache.compute(point, { processPoint(it, evaluatedDs, cutSize, range) })
}

fun performEnrichment(delaunay: IncrementalDelaunayTriangulator,
                      cache: CutCache,
                      geomFact: GeometryFactory,
                      subDiv: QuadEdgeSubdivision): Boolean {
    val trianglesGeom = subDiv.getTriangles(geomFact) as GeometryCollection
    val allTriangles = 0.until(trianglesGeom.numGeometries)
            .map { trianglesGeom.getGeometryN(it) }
            .map { Triangle(it.coordinates[0], it.coordinates[1], it.coordinates[2]) }
    var isChanged = false
    allTriangles.onEach {
        val p0 = it.p0
        val cut0 = process(p0, cache)
        val p1 = it.p1
        val cut1 = process(p1, cache)
        val p2 = it.p2
        val cut2 = process(p2, cache)

        val center = getCenter(p0, p1, p2)
        val centerCut = process(center, cache)
        if (centerCut != cut0 && centerCut != cut1 && centerCut != cut2) {
            /*if (allTriangles.size > 20000) {
                val centerCut2 = process(center, cache)
                val cut01 = cut0.clone()
                cut01.andNot(cut1)
                val cut12 = cut1.clone()
                cut12.andNot(cut2)
                val cut02 = cut0.clone()
                cut02.andNot(cut2)
                logToConsole { "Cut 0 equals to cut 1: ${cut0 == cut1} $cut01" }
                logToConsole { "Cut 1 equals to cut 2: ${cut1 == cut2} $cut12" }
                logToConsole { "Cut 0 equals to cut 2: ${cut0 == cut2} $cut02" }
            }*/
            delaunay.insertSite(Vertex(center.x, center.y))
            isChanged = true
        } else {
            //logToConsole { "Something is equal to something" }
        }
    }
    logToConsole { "Finished iteration: found ${allTriangles.size} polygons" }
    return isChanged
}

fun getCenter(p0: Coordinate, p1: Coordinate, p2: Coordinate): Coordinate {
    return Coordinate(
            (p0.x + p1.x + p2.x) / 3,
            (p0.y + p1.y + p2.y) / 3
    )
}