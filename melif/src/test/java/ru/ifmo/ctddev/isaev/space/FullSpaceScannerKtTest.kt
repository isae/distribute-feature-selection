package ru.ifmo.ctddev.isaev.space

import org.junit.Assert
import org.junit.Test


/**
 * @author iisaev
 */
class FullSpaceScannerKtTest {

    @Test
    fun testInBetween() {
        Assert.assertEquals(SpacePoint(intArrayOf(0, 3), 4),
                inBetween(
                        SpacePoint(intArrayOf(0, 1), 2),
                        SpacePoint(intArrayOf(0, 1), 1)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(3, 0), 4),
                inBetween(
                        SpacePoint(intArrayOf(1, 0), 2),
                        SpacePoint(intArrayOf(1, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(0, 1), 4),
                inBetween(
                        SpacePoint(intArrayOf(0, 1), 2),
                        SpacePoint(intArrayOf(0, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(1, 0), 4),
                inBetween(
                        SpacePoint(intArrayOf(1, 0), 2),
                        SpacePoint(intArrayOf(0, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(1, 3), 4),
                inBetween(
                        SpacePoint(intArrayOf(2, 2), 4),
                        SpacePoint(intArrayOf(0, 4), 4)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(1, 3), 4),
                inBetween(
                        SpacePoint(intArrayOf(2, 2), 4),
                        SpacePoint(intArrayOf(0, 8), 8)
                )
        )
        Assert.assertEquals(SpacePoint(intArrayOf(3, 1), 4),
                inBetween(
                        SpacePoint(intArrayOf(2, 2), 4),
                        SpacePoint(intArrayOf(8, 0), 8)
                )
        )
    }
}