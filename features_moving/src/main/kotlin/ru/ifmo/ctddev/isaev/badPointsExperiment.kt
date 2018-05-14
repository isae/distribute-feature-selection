package ru.ifmo.ctddev.isaev

import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.space.*
import kotlin.math.max


/**
 * @author iisaev
 */

//private val measures = listOf(VDM::class, FitCriterion::class)
private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class)
//private val measures = listOf(SpearmanRankCorrelation::class, VDM::class, FitCriterion::class, SymmetricUncertainty::class)

private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDataSet = evaluateDataSet(dataSet, measures)

fun main(args: Array<String>) {
    /* var lRaw = SpacePoint(intArrayOf(545), 2048)
     var rRaw = SpacePoint(intArrayOf( 1091), 4096)*/
    var lRaw = SpacePoint(intArrayOf(2048, 545), 2048)
    var rRaw = SpacePoint(intArrayOf(4096, 1091), 4096)
    do {
        val mRaw = inBetween(lRaw, rRaw)
        val left = getPointOnUnitSphere(getAngle(lRaw))
        val angles = getAngle(rRaw)
        val right = getPointOnUnitSphere(angles)
        val middle = getPointOnUnitSphere(getAngle(mRaw))
        val maxes = left.coordinates.zip(right.coordinates).map { (o1, o2) -> max(o1, o2) }
        val projMaxes = lRaw.point.zip(rRaw.point).map { (o1, o2) -> max(o1, o2) }
        println("Left: ${lRaw.point.mapIndexed { i, f -> f - projMaxes[i] }} -> " +
                "${left.coordinates.mapIndexed { i, c -> (c - maxes[i]) * 100000 }}")
        println("Right: ${rRaw.point.mapIndexed { i, f -> f - projMaxes[i] }} -> " +
                "${right.coordinates.mapIndexed { i, c -> (c - maxes[i]) * 100000 }}}")
        val lCut = processPointGetWholeCut(left, evaluatedDataSet, cutSize)
        val rCut = processPointGetWholeCut(right, evaluatedDataSet, cutSize)
        val mCut = processPointGetWholeCut(middle, evaluatedDataSet, cutSize)
        //println("Lcut: $lCut")
        //println("Mcut: $mCut")
        //println("Rcut: $rCut")
        //println("Diff between left and right: ${getDiff(lCut, rCut)}")
        //println("Diff between right and left: ${getDiff(rCut, lCut)}")
        val lDiff = getDiff(lCut, mCut)
        //println("Diff between left and middle: $lDiff")
        val rDiff = getDiff(mCut, rCut)
        //println("Diff between middle and right: $rDiff")
        val firstDiffPos = lCut.indices.first {
            lCut[it] != rCut[it]
        }
        //println("Diff since $firstDiffPos; l: ${lCut.drop(firstDiffPos)}")
        //println("Diff since $firstDiffPos; r: ${rCut.drop(firstDiffPos)}")
        /*lCut.forEachIndexed { i, f ->
            if (rCut[i] != f) {
                val placeInR = rCut.indexOfFirst { it == f }
                if (placeInR == -1) {
                    println("Feature $f: removed")
                } else {
                    println("Feature $f: moved from $i to $placeInR")
                }
            }
        }*/
        val targetDiff: List<Int>
        if (lDiff.isEmpty()) {
            //println("Going to the right")
            targetDiff = rDiff
            lRaw = mRaw
        } else {
            //println("Going to the left")
            targetDiff = lDiff
            rRaw = mRaw
        }
        println("\n___________________________\n")
    } while (targetDiff.size > 1)
}