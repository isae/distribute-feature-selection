package ru.ifmo.ctddev.isaev

import ru.ifmo.ctddev.isaev.feature.measure.VDM
import ru.ifmo.ctddev.isaev.point.Point
import ru.ifmo.ctddev.isaev.space.*
import java.util.*


/**
 * @author iisaev
 */

private val measures = arrayOf(VDM(), FitCriterion())

private const val cutSize = 50
private val dataSet = KnownDatasets.DLBCL.read()
private val evaluatedDataSet = evaluateDataSet(dataSet, measures)

fun main(args: Array<String>) {
    var lRaw = SpacePoint(intArrayOf(19), 64)
    var rRaw = SpacePoint(intArrayOf(39), 128)
    do {
        println("Left: $lRaw")
        println("Right: $rRaw")
        val mRaw = inBetween(lRaw, rRaw)
        val left = Point.fromRawCoords(lRaw.point[0].toDouble() / lRaw.delta, 1.0)
        val right = Point.fromRawCoords(rRaw.point[0].toDouble() / rRaw.delta, 1.0)
        val middle = Point.fromRawCoords(mRaw.point[0].toDouble() / mRaw.delta, 1.0)
        println("Left angles: ${Arrays.toString(left.coordinates)}")
        println("Riht angles: ${Arrays.toString(right.coordinates)}")
        val lCut = processPointGetWholeCut(left, evaluatedDataSet, cutSize)
        val rCut = processPointGetWholeCut(right, evaluatedDataSet, cutSize)
        val mCut = processPointGetWholeCut(middle, evaluatedDataSet, cutSize)
        println("Lcut: $lCut")
        println("Mcut: $mCut")
        println("Rcut: $rCut")
        println("Diff between left and right: ${getDiff(lCut, rCut)}")
        //println("Diff between left and right, indices: ${getDiff(lCut, rCut).map { f -> lCut.indexOf(f) }}")
        println("Diff between right and left: ${getDiff(rCut, lCut)}")
        //println("Diff between right and left, indices: ${getDiff(rCut, lCut).map { f -> rCut.indexOf(f) }}")
        val lDiff = getDiff(lCut, mCut)
        println("Diff between left and middle: $lDiff")
        val rDiff = getDiff(mCut, rCut)
        println("Diff between middle and right: $rDiff")
        val firstDiffPos = lCut.indices.first {
            lCut[it] != rCut[it]
        }
        println("Diff since $firstDiffPos; l: ${lCut.drop(firstDiffPos)}")
        println("Diff since $firstDiffPos; r: ${rCut.drop(firstDiffPos)}")
        println("For left diff: ")
        val targetDiff: List<Int>
        if (lDiff.size < 2) {
            println("Going to the right")
            targetDiff = rDiff
            lRaw = mRaw
        } else {
            println("Going to the left")
            targetDiff = lDiff
            rRaw = mRaw
        }
    } while (lDiff.size > 1 || rDiff.size > 1)
}