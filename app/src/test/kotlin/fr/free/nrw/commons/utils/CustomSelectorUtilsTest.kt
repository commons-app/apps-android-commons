package fr.free.nrw.commons.utils

import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.IOException
import java.net.UnknownHostException

/**
 * Test class for [CustomSelectorUtils]
 */
class CustomSelectorUtilsTest {
    private val mediaClient: MediaClient = mock(MediaClient::class.java)

    private fun checkWhetherFileExists(): ImageLoader.Result =
        runBlocking {
            CustomSelectorUtils.checkWhetherFileExistsOnCommonsUsingSHA1("sha1", Dispatchers.Unconfined, mediaClient)
        }

    @Test
    fun testFileExistsOnCommons() {
        `when`(mediaClient.checkFileExistsUsingSha("sha1")).thenReturn(Single.just(true))

        assertEquals(ImageLoader.Result.TRUE, checkWhetherFileExists())
    }

    @Test
    fun testFileDoesNotExistOnCommons() {
        `when`(mediaClient.checkFileExistsUsingSha("sha1")).thenReturn(Single.just(false))

        assertEquals(ImageLoader.Result.FALSE, checkWhetherFileExists())
    }

    @Test
    fun testNoNetworkConnectionReturnsError() {
        `when`(mediaClient.checkFileExistsUsingSha("sha1")).thenReturn(Single.error(UnknownHostException()))

        assertEquals(ImageLoader.Result.ERROR, checkWhetherFileExists())
    }

    @Test
    fun testOtherFailureReturnsError() {
        `when`(mediaClient.checkFileExistsUsingSha("sha1")).thenReturn(Single.error(IOException()))

        assertEquals(ImageLoader.Result.ERROR, checkWhetherFileExists())
    }
}
