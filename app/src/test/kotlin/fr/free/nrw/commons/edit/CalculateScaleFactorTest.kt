package fr.free.nrw.commons.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateScaleFactorTest {
    @Test
    fun bitmapWithinMaxSizeIsNotScaled() {
        assertEquals(1, calculateScaleFactor(1000, 1500, MAX_SIZE))
    }

    @Test
    fun bitmapEqualToMaxSizeIsNotScaled() {
        assertEquals(1, calculateScaleFactor(MAX_SIZE, MAX_SIZE, MAX_SIZE))
    }

    @Test
    fun widthOverMaxSizeUsesWidthRatio() {
        assertEquals(2, calculateScaleFactor(4000, 1000, MAX_SIZE))
    }

    @Test
    fun heightOverMaxSizeRoundsRatioUp() {
        assertEquals(3, calculateScaleFactor(1000, 5000, MAX_SIZE))
    }

    @Test
    fun sizeJustOverMaxSizeRoundsUpToTwo() {
        assertEquals(2, calculateScaleFactor(MAX_SIZE + 1, 100, MAX_SIZE))
    }

    @Test
    fun bothSidesOverMaxSizeUseLargerRatio() {
        assertEquals(3, calculateScaleFactor(6000, 3000, MAX_SIZE))
    }

    private companion object {
        const val MAX_SIZE = 2000
    }
}
