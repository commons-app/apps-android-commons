package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.graphics.Matrix
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimationStateUnitTest {

    private val zeroValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

    @Test
    fun calculateInterpolationReturnsStartValuesAtFractionZero() {
        val state = AnimationState()
        state.getStartValues().fill(0f)
        state.getStopValues().fill(2f)

        val outMatrix = Matrix()
        state.calculateInterpolation(outMatrix, 0f)

        val values = zeroValues.copyOf()
        outMatrix.getValues(values)
        assertArrayEquals(zeroValues, values, 0f)
    }

    @Test
    fun calculateInterpolationReturnsStopValuesAtFractionOne() {
        val state = AnimationState()
        state.getStartValues().fill(0f)
        state.getStopValues().fill(2f)

        val outMatrix = Matrix()
        state.calculateInterpolation(outMatrix, 1f)

        val values = zeroValues.copyOf()
        outMatrix.getValues(values)
        assertArrayEquals(floatArrayOf(2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f), values, 0f)
    }

    @Test
    fun calculateInterpolationInterpolatesLinearlyAtFractionHalf() {
        val state = AnimationState()
        state.getStartValues().fill(2f)
        state.getStopValues().fill(0f)

        val outMatrix = Matrix()
        state.calculateInterpolation(outMatrix, 1f / 2f)

        val values = zeroValues.copyOf()
        outMatrix.getValues(values)
        assertArrayEquals(floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), values, 0f)
    }

    @Test
    fun isAnimatingDefaultsToFalseAndCanBeToggled() {
        val state = AnimationState()
        assertFalse(state.getIsAnimating())

        state.setAnimating(true)
        assertTrue(state.getIsAnimating())

        state.setAnimating(false)
        assertFalse(state.getIsAnimating())
    }
}
