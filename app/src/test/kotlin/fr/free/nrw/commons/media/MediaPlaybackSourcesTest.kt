package fr.free.nrw.commons.media

import android.content.Context
import android.util.DisplayMetrics
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.wikidata.model.gallery.MediaDerivative
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ORIGINAL_SRC =
    "https://upload.wikimedia.org/wikipedia/commons/4/41/Big_Buck_Bunny_medium.ogv"

private const val TRANSCODE_SRC_240 =
    "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/" +
        "Big_Buck_Bunny_medium.ogv/Big_Buck_Bunny_medium.ogv.240p.vp9.webm"

private const val TRANSCODE_SRC_480 =
    "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/" +
        "Big_Buck_Bunny_medium.ogv/Big_Buck_Bunny_medium.ogv.480p.vp9.webm"

private const val TRANSCODE_SRC_720 =
    "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/" +
        "Big_Buck_Bunny_medium.ogv/Big_Buck_Bunny_medium.ogv.720p.vp9.webm"

private const val AUDIO_ORIGINAL_SRC =
    "https://upload.wikimedia.org/wikipedia/commons/a/a9/Tromboon-sample.ogg"

private const val AUDIO_TRANSCODE_SRC_MP3 =
    "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a9/" +
        "Tromboon-sample.ogg/Tromboon-sample.ogg.mp3"

/** Exercises media playback source selection, resolution caps, and quality labels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MediaPlaybackSourcesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun displayMetrics(
        widthPixels: Int,
        heightPixels: Int,
    ): DisplayMetrics =
        DisplayMetrics().apply {
            this.widthPixels = widthPixels
            this.heightPixels = heightPixels
        }

    @Test
    @Throws(Exception::class)
    fun testAutoVideoQualityCapsAtTheShorterScreenSide() {
        Assert.assertEquals(1080, maxPlaybackResolution("auto", displayMetrics(1080, 2400)))
        Assert.assertEquals(1080, maxPlaybackResolution("auto", displayMetrics(2400, 1080)))
    }

    @Test
    @Throws(Exception::class)
    fun testHigherVideoQualityHasNoCap() {
        Assert.assertEquals(Int.MAX_VALUE, maxPlaybackResolution("higher", displayMetrics(1080, 2400)))
    }

    @Test
    @Throws(Exception::class)
    fun testDataSaverVideoQualityCapsAtZero() {
        Assert.assertEquals(0, maxPlaybackResolution("data_saver", displayMetrics(1080, 2400)))
    }

    @Test
    @Throws(Exception::class)
    fun testUnknownVideoQualityBehavesLikeAuto() {
        Assert.assertEquals(1080, maxPlaybackResolution("bogus", displayMetrics(1080, 2400)))
        Assert.assertEquals(1080, maxPlaybackResolution(null, displayMetrics(1080, 2400)))
    }

    private fun playbackSource(
        src: String,
        transcodeKey: String?,
        bandwidth: Int = 0,
        height: Int = 0,
        mediaType: MediaType = MediaType.VIDEO,
    ): MediaDerivative {
        val playbackSource = mock(MediaDerivative::class.java)
        whenever(playbackSource.src()).thenReturn(src)
        whenever(playbackSource.transcodeKey()).thenReturn(transcodeKey)
        whenever(playbackSource.bandwidth()).thenReturn(bandwidth)
        whenever(playbackSource.height()).thenReturn(height)
        whenever(playbackSource.mediaType()).thenReturn(mediaType)
        return playbackSource
    }

    @Test
    @Throws(Exception::class)
    fun testTranscodeIsPreferredOverTheOriginal() {
        val original = playbackSource(ORIGINAL_SRC, null)
        val transcode = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")

        val preferred = preferredMediaPlaybackSource(listOf(original, transcode), maxResolution = 1080)

        Assert.assertEquals(transcode.src(), preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testResumedSourceIsPreferredOverTheTranscode() {
        val original = playbackSource(ORIGINAL_SRC, null)
        val transcode = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")

        val preferred =
            preferredMediaPlaybackSource(listOf(original, transcode), maxResolution = 1080, resume = original)

        Assert.assertEquals(original.src(), preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testLargestTranscodeWithinTheCapIsPreferred() {
        val transcode240 = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")
        val transcode480 = playbackSource(TRANSCODE_SRC_480, "480p.vp9.webm")
        val transcode720 = playbackSource(TRANSCODE_SRC_720, "720p.vp9.webm")

        val preferred =
            preferredMediaPlaybackSource(listOf(transcode240, transcode480, transcode720), maxResolution = 480)

        Assert.assertEquals(TRANSCODE_SRC_480, preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testSmallestTranscodeIsPreferredWhenNoneFitsTheCap() {
        val transcode480 = playbackSource(TRANSCODE_SRC_480, "480p.vp9.webm")
        val transcode240 = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")

        val preferred = preferredMediaPlaybackSource(listOf(transcode480, transcode240), maxResolution = 0)

        Assert.assertEquals(TRANSCODE_SRC_240, preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testHighestTranscodeIsPreferredWithoutACap() {
        val original = playbackSource(ORIGINAL_SRC, null)
        val transcode240 = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")
        val transcode720 = playbackSource(TRANSCODE_SRC_720, "720p.vp9.webm")

        val preferred =
            preferredMediaPlaybackSource(listOf(original, transcode240, transcode720), maxResolution = Int.MAX_VALUE)

        Assert.assertEquals(TRANSCODE_SRC_720, preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testAudioTranscodeIsPreferredRegardlessOfTheCap() {
        val original = playbackSource(AUDIO_ORIGINAL_SRC, null, mediaType = MediaType.AUDIO)
        val transcode = playbackSource(AUDIO_TRANSCODE_SRC_MP3, "mp3", mediaType = MediaType.AUDIO)

        val preferred = preferredMediaPlaybackSource(listOf(original, transcode), maxResolution = 0)

        Assert.assertEquals(AUDIO_TRANSCODE_SRC_MP3, preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testTranscodeHeightIsUsedWhenTheKeyHasNoResolution() {
        val transcode240 = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")
        val transcode480 = playbackSource(TRANSCODE_SRC_480, "vp9.webm", height = 480)

        val preferred = preferredMediaPlaybackSource(listOf(transcode240, transcode480), maxResolution = 480)

        Assert.assertEquals(TRANSCODE_SRC_480, preferred?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testSaveResumeStateKeepsTheSourceBeingPlayed() {
        val player = MediaDetailPlayer(context, OkHttpClient()) {}
        val transcode240 = playbackSource(TRANSCODE_SRC_240, "240p.vp9.webm")
        val transcode480 = playbackSource(TRANSCODE_SRC_480, "480p.vp9.webm")
        Whitebox.setInternalState(player, "selected", transcode480)

        player.saveResumeState()
        player.release()
        player.setSources(listOf(transcode240, transcode480), emptyList())

        Assert.assertNull(player.selected)
        Assert.assertEquals(TRANSCODE_SRC_480, player.preferredSource(1080)?.src())
    }

    @Test
    @Throws(Exception::class)
    fun testVideoQualityLabelUsesTheTranscodeResolution() {
        val transcode =
            MediaDerivative(TRANSCODE_SRC_240, "video/webm; codecs=\"vp9, opus\"", transcodeKey = "240p.vp9.webm")

        Assert.assertEquals("240p", transcode.qualityLabel(context))
    }

    @Test
    @Throws(Exception::class)
    fun testVideoQualityLabelNamesTheOriginal() {
        val original = MediaDerivative(ORIGINAL_SRC, "video/webm; codecs=\"vp8, vorbis\"")

        Assert.assertEquals("Original", original.qualityLabel(context))
    }

    @Test
    @Throws(Exception::class)
    fun testAudioQualityLabelIsRoundedToKbps() {
        val transcode = MediaDerivative(AUDIO_TRANSCODE_SRC_MP3, "audio/mpeg", bandwidth = 199728, transcodeKey = "mp3")

        Assert.assertEquals("200 kbps", transcode.qualityLabel(context))
    }

    @Test
    @Throws(Exception::class)
    fun testAudioQualityLabelNamesTheOriginal() {
        val original = MediaDerivative(AUDIO_ORIGINAL_SRC, "audio/ogg; codecs=\"vorbis\"", bandwidth = 106695)

        Assert.assertEquals("107 kbps (Original)", original.qualityLabel(context))
    }
}
