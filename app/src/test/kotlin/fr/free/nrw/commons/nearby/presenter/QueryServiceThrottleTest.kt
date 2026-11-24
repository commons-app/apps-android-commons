package fr.free.nrw.commons.nearby.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueryServiceThrottleTest {
    private var now = 1_000_000L
    private val throttle = QueryServiceThrottle(5000, { now }, Random(1))

    @Test
    fun `nothing to wait for before any failure`() {
        assertEquals(0, throttle.waitMillis())
    }

    @Test
    fun `waits at least 5 s and doubles up to a minute when the service gives no wait`() {
        val waits = (1..6).map { throttle.onTemporaryFailure(null) }

        listOf(5000L, 10_000L, 20_000L, 40_000L, 60_000L, 60_000L).zip(waits).forEach { (base, wait) ->
            assertTrue("$wait should be $base plus up to 1 s", wait in base..base + 1000)
        }
    }

    @Test
    fun `the wait asked for by the service is used as it is`() {
        assertEquals(42_000L, throttle.onTemporaryFailure(42_000L))
        assertEquals(42_000L, throttle.waitMillis())
    }

    @Test
    fun `all requests wait for the latest pause and it runs out`() {
        throttle.onTemporaryFailure(30_000L)
        throttle.onTemporaryFailure(1000L)
        assertEquals(30_000L, throttle.waitMillis())

        now += 30_000L
        assertEquals(0, throttle.waitMillis())
    }

    @Test
    fun `an answer makes the next refusal start from the first wait again`() {
        repeat(3) { throttle.onTemporaryFailure(null) }
        throttle.onSuccess()

        assertTrue(throttle.onTemporaryFailure(null) in 5000L..6000L)
    }
}
