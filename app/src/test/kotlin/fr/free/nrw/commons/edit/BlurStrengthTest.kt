package fr.free.nrw.commons.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurStrengthTest {
    @Test
    fun maxStrengthUsesSolidAverage() {
        assertNull(BlurStrength.radiusPx(100, regionWidth = 200, regionHeight = 100))
        assertEquals(BlurStrength.MAX_PROGRESS, BlurStrength.DEFAULT_PROGRESS)
    }

    @Test
    fun midStrengthKeepsAModerateRadius() {
        val radius = BlurStrength.radiusPx(50, regionWidth = 200, regionHeight = 200)!!
        assertTrue("mid strength should blur, was $radius", radius in 10..50)
    }

    @Test
    fun lightStrengthUsesASmallRadius() {
        val light = BlurStrength.radiusPx(20, regionWidth = 200, regionHeight = 200)!!
        val mid = BlurStrength.radiusPx(50, regionWidth = 200, regionHeight = 200)!!
        assertTrue(light in 1 until mid)
    }

    @Test
    fun higherStrengthIncreasesRadius() {
        val low = BlurStrength.radiusPx(10, regionWidth = 400, regionHeight = 400)!!
        val high = BlurStrength.radiusPx(90, regionWidth = 400, regionHeight = 400)!!
        assertTrue(high > low)
    }

    @Test
    fun zeroStrengthStillAppliesALightBlur() {
        assertEquals(1, BlurStrength.radiusPx(0, regionWidth = 100, regionHeight = 100))
    }
}
