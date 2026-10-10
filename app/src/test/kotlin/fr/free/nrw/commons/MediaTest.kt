package fr.free.nrw.commons

import media
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MediaTest {
    @Test
    fun displayTitleShouldStripExtension() {
        val m = media(filename = "File:Example.jpg")
        assertEquals("Example", m.displayTitle)
    }

    @Test
    fun displayTitleShouldUseSpaceForUnderscore() {
        val m = media(filename = "File:Example 1_2.jpg")
        assertEquals("Example 1 2", m.displayTitle)
    }

    @Test
    fun apiMediaTypeShouldOverrideTheExtensionFallback() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom("VIDEO", "application/ogg", "File:Example.ogg"))
    }

    @Test
    fun apiMediaTypesShouldMapToTheMatchingMediaType() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom("VIDEO", "video/webm", "File:Example.webm"))
        assertEquals(MediaType.AUDIO, mediaTypeFrom("AUDIO", "application/ogg", "File:Example.oga"))
        assertEquals(MediaType.IMAGE, mediaTypeFrom("BITMAP", "image/jpeg", "File:Example.jpg"))
        assertEquals(MediaType.IMAGE, mediaTypeFrom("DRAWING", "image/svg+xml", "File:Example.svg"))
    }

    @Test
    fun unrecognisedApiMediaTypeShouldNotFallBackToTheMimeType() {
        // DjVu files report OFFICE with an image/* mime type. OFFICE isn't handled, so it's OTHER.
        assertEquals(MediaType.OTHER, mediaTypeFrom("OFFICE", "image/vnd.djvu", "File:Example.djvu"))
    }

    @Test
    fun missingApiMediaTypeShouldFallBackToTheMimeType() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom(null, "video/webm", "File:Example.webm"))
        assertEquals(MediaType.VIDEO, mediaTypeFrom(null, "application/ogg", "File:Example.ogv"))
    }

    @Test
    fun oggContainersShouldBeSplitByExtension() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom("application/ogg", "File:Example.ogv"))
        assertEquals(MediaType.AUDIO, mediaTypeFrom("application/ogg", "File:Example.ogg"))
        assertEquals(MediaType.AUDIO, mediaTypeFrom("application/ogg", "File:Example.oga"))
        assertEquals(MediaType.AUDIO, mediaTypeFrom("application/ogg", "File:Example.opus"))
    }

    @Test
    fun mimeTypeShouldDecideTheMediaTypeForNonOggFiles() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom("video/webm", "File:Example.webm"))
        assertEquals(MediaType.AUDIO, mediaTypeFrom("audio/mpeg", "File:Example.mp3"))
        assertEquals(MediaType.IMAGE, mediaTypeFrom("image/jpeg", "File:Example.jpg"))
        assertEquals(MediaType.OTHER, mediaTypeFrom("application/pdf", "File:Example.pdf"))
    }

    @Test
    fun uppercaseFileExtensionsShouldStillResolve() {
        assertEquals(MediaType.VIDEO, mediaTypeFrom("application/ogg", "File:Example.OGV"))
    }
}
