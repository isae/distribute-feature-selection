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
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDs = evaluateDataSet(dataSet, measures)
val range = Array(evaluatedDs[0].size, { it })
const val MAX = 700.0

fun main(args: Array<String>) {
    val envelope = Envelope(Coordinate(0.0, 0.0), Coordinate(MAX, MAX))
    val subDiv = QuadEdgeSubdivision(envelope, 1E-6)
    val delaunay = IncrementalDelaunayTriangulator(subDiv)
    delaunay.insertSite(Vertex(0.0, MAX))
    delaunay.insertSite(Vertex(MAX, 0.0))
    delaunay.insertSite(Vertex(MAX, MAX))
    val pointCache = MapCache()
    val geomFact = GeometryFactory()
    do {
        val isChanged = performDelaunayEnrichment(evaluatedDs, delaunay, pointCache, geomFact, MAX, range, subDiv, cutSize)
    } while (isChanged)

    val pointsToTry = pointCache.getAll()
    println("Found ${pointsToTry.size} points to try with enrichment")
    pointsToTry.forEach { println(it) }
    visualizeDelaunay(envelope, subDiv, geomFact, delaunay)
}