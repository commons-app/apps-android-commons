package fr.free.nrw.commons.media

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonHeaderRequestInterceptor
import fr.free.nrw.commons.di.NetworkingModule
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaPlaybackHttpClientTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val module = NetworkingModule()
    private val sharedLoggingInterceptor = module.provideHttpLoggingInterceptor()
    private val playbackLoggingInterceptor = module.provideMediaPlaybackHttpLoggingInterceptor()
    private val context: Context = mock()

    @Before
    fun setUp() {
        whenever(context.cacheDir).thenReturn(temporaryFolder.newFolder())
    }

    @Test
    fun playbackClientShouldSwapTheLoggingInterceptorInPlace() {
        val shared = module.provideOkHttpClient(context, sharedLoggingInterceptor)
        val playback = module.provideMediaPlaybackHttpClient(shared, playbackLoggingInterceptor)

        assertSame(playbackLoggingInterceptor, playback.interceptors.first())
        assertTrue(playback.interceptors.last() is CommonHeaderRequestInterceptor)
        assertEquals(shared.interceptors.size, playback.interceptors.size)
    }

    @Test
    fun playbackClientShouldDropTheCacheAndLeaveTheSharedClientAlone() {
        val shared = module.provideOkHttpClient(context, sharedLoggingInterceptor)
        val playback = module.provideMediaPlaybackHttpClient(shared, playbackLoggingInterceptor)

        assertNull(playback.cache)
        assertNotNull(shared.cache)
        assertSame(sharedLoggingInterceptor, shared.interceptors.first())
    }

    @Test
    fun playbackLoggingShouldStopAtHeaders() {
        assertEquals(Level.HEADERS, playbackLoggingInterceptor.level)
    }
}
