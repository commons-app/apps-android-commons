package fr.free.nrw.commons.nearby.presenter

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * A pause shared by all Nearby detail requests to the Wikidata Query Service.
 *
 * When the service refuses a request for a while (429, 503) or a request fails on the way, every
 * request waits until the same time before the next one is sent, instead of each request finding
 * out on its own. The wait is the one the service asks for with Retry-After, or else starts at
 * [firstWaitMillis] and doubles with each refusal in a row, up to [MAX_WAIT_MILLIS], plus a random
 * part so that requests don't all start again at the same moment.
 *
 * See https://www.mediawiki.org/wiki/Wikimedia_APIs/Rate_limits#Errors
 */
internal class QueryServiceThrottle(
    private val firstWaitMillis: Long = TimeUnit.SECONDS.toMillis(FIRST_WAIT_SECONDS),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val random: Random = Random.Default,
) {
    @Volatile
    private var resumeAtMillis = 0L
    private val refusalsInARow = AtomicInteger()

    /**
     * How long requests still have to wait, 0 if they can be sent now
     */
    fun waitMillis(): Long = (resumeAtMillis - currentTimeMillis()).coerceAtLeast(0)

    /**
     * Pauses all requests after a temporary failure and returns how long the pause is
     *
     * @param retryAfterMillis the wait asked for by the service, or null if it didn't say
     */
    fun onTemporaryFailure(retryAfterMillis: Long?): Long {
        val inARow = refusalsInARow.incrementAndGet()
        val wait =
            retryAfterMillis ?: (
                minOf(firstWaitMillis shl (inARow - 1).coerceAtMost(MAX_DOUBLINGS), MAX_WAIT_MILLIS) +
                    random.nextLong(firstWaitMillis / JITTER_DIVISOR + 1)
            )
        synchronized(this) {
            // A later pause asked for by another request wins
            resumeAtMillis = maxOf(resumeAtMillis, currentTimeMillis() + wait)
        }
        return wait
    }

    /**
     * Called when a request got an answer, so the next refusal starts from the first wait again
     */
    fun onSuccess() {
        refusalsInARow.set(0)
    }

    companion object {
        private const val FIRST_WAIT_SECONDS = 5L
        private val MAX_WAIT_MILLIS = TimeUnit.MINUTES.toMillis(1)
        private const val MAX_DOUBLINGS = 4
        private const val JITTER_DIVISOR = 5
    }
}
