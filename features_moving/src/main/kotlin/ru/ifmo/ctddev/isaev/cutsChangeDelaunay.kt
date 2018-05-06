package ru.ifmo.ctddev.isaev

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex
import ru.ifmo.ctddev.isaev.feature.measure.SymmetricUncertainty
import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.space.MapCache
import ru.ifmo.ctddev.isaev.space.evaluateDataSet
import ru.ifmo.ctddev.isaev.space.performDelaunayEnrichment


/**
 * @author iisaev
 */

private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, SymmetricUncertainty::class)
private const val cutSize = 50
private val dataSet = KnownDatasets.ARIZONA5.read()
private val evaluatedDs = evaluateDataSet(dataSet, measures)
val range = Array(evaluatedDs[0].size, { it })
const val MAX = 700.0

fun main(args: Array<String>) {
    val tolerance = 1E-6
    val envelope = Envelope(Coordinate(0.0, 0.0), Coordinate(1.0 / tolerance, 1.0 / tolerance))
    val subDiv = QuadEdgeSubdivision(envelope, tolerance)
    val delaunay = IncrementalDelaunayTriangulator(subDiv)
    delaunay.insertSite(Vertex(1.0 / tolerance, 1.0))
    delaunay.insertSite(Vertex(tolerance, 1.0 / tolerance))
    delaunay.insertSite(Vertex(tolerance, tolerance))
    val pointCache = MapCache()
    val geomFact = GeometryFactory()
    do {
        val isChanged = performDelaunayEnrichment(evaluatedDs, delaunay, pointCache, geomFact, range, subDiv, cutSize)
    } while (isChanged)

    val pointsToTry = pointCache.getAll()
    println("Found ${pointsToTry.size} points to try with enrichment")
    pointsToTry.forEach { println(it) }
    visualizeDelaunay(envelope, subDiv, geomFact, delaunay)
}