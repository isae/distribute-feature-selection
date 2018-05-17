package ru.ifmo.ctddev.isaev.space

import org.junit.Assert
import org.junit.Test


/**
 * @author iisaev
 */
class FullSpaceScannerKtTest {

    @Test
    fun testInBetween() {
        Assert.assertEquals(SpacePoint(longArrayOf(0, 3), 4),
                SpacePoint(longArrayOf(0, 1), 2).inBetween(SpacePoint(longArrayOf(0, 1), 1)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(3, 0), 4),
                SpacePoint(longArrayOf(1, 0), 2).inBetween(SpacePoint(longArrayOf(1, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(0, 1), 4),
                SpacePoint(longArrayOf(0, 1), 2).inBetween(SpacePoint(longArrayOf(0, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(1, 0), 4),
                SpacePoint(longArrayOf(1, 0), 2).inBetween(SpacePoint(longArrayOf(0, 0), 1)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(1, 3), 4),
                SpacePoint(longArrayOf(2, 2), 4).inBetween(SpacePoint(longArrayOf(0, 4), 4)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(1, 3), 4),
                SpacePoint(longArrayOf(2, 2), 4).inBetween(SpacePoint(longArrayOf(0, 8), 8)
                )
        )
        Assert.assertEquals(SpacePoint(longArrayOf(3, 1), 4),
                SpacePoint(longArrayOf(2, 2), 4).inBetween(SpacePoint(longArrayOf(8, 0), 8)
                )
        )
    }
}