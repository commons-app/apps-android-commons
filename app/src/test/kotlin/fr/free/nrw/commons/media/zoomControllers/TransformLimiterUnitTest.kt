package fr.free.nrw.commons.media.zoomControllers

import android.graphics.Matrix
import android.graphics.RectF
import fr.free.nrw.commons.media.zoomControllers.zoomable.TransformLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransformLimiterUnitTest {
    private lateinit var limiter: TransformLimiter

    @Before
    fun setUp() {
        limiter = TransformLimiter(minScale = 1.0f, maxScale = 3.0f)
    }

    @Test
    fun testMatrixScaleAndIdentity() {
        val matrix = Matrix()
        assertTrue(limiter.isMatrixIdentity(matrix))
        assertEquals(1.0f, limiter.getMatrixScaleFactor(matrix), 1e-4f)

        matrix.setScale(2.5f, 2.5f)
        assertFalse(limiter.isMatrixIdentity(matrix))
        assertEquals(2.5f, limiter.getMatrixScaleFactor(matrix), 1e-4f)
    }

    @Test
    fun testLimitScale() {
        val matrix = Matrix().apply { setScale(0.5f, 0.5f) }
        assertFalse(limiter.limitScale(matrix, 0f, 0f, TransformLimiter.LIMIT_NONE))
        assertTrue(limiter.limitScale(matrix, 0f, 0f, TransformLimiter.LIMIT_SCALE))
        assertEquals(1.0f, limiter.getMatrixScaleFactor(matrix), 1e-4f)

        matrix.setScale(4f, 4f)
        assertTrue(limiter.limitScale(matrix, 0f, 0f, TransformLimiter.LIMIT_SCALE))
        assertEquals(3.0f, limiter.getMatrixScaleFactor(matrix), 1e-4f)

        matrix.setScale(2f, 2f)
        assertFalse(limiter.limitScale(matrix, 0f, 0f, TransformLimiter.LIMIT_SCALE))
        assertEquals(2.0f, limiter.getMatrixScaleFactor(matrix), 1e-4f)
    }

    @Test
    fun testLimitTranslation() {
        val matrix = Matrix().apply { postTranslate(50f, 50f) }
        val img = RectF(0f, 0f, 200f, 200f)
        val view = RectF(0f, 0f, 100f, 100f)

        assertFalse(limiter.limitTranslation(matrix, TransformLimiter.LIMIT_NONE, img, view))
        assertTrue(limiter.limitTranslation(matrix, TransformLimiter.LIMIT_ALL, img, view))
    }

    @Test
    fun testCanScrollInAllDirection() {
        val view = RectF(0f, 0f, 100f, 100f)
        assertTrue(limiter.canScrollInAllDirection(RectF(-10f, -10f, 110f, 110f), view))
        assertFalse(limiter.canScrollInAllDirection(RectF(10f, 10f, 90f, 90f), view))
    }
}
