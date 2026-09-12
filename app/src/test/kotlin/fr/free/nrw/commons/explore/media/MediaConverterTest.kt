package fr.free.nrw.commons.explore.media

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaType
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.gallery.ExtMetadata
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.lang.IllegalArgumentException

class MediaConverterTest {
    @Mock
    lateinit var page: MwQueryPage

    @Mock
    lateinit var entity: Entities.Entity

    @Mock
    lateinit var imageInfo: ImageInfo

    @Mock
    lateinit var metadata: ExtMetadata

    lateinit var mediaConverter: MediaConverter
    lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mediaConverter = MediaConverter()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConvertNoMetadata() {
        mediaConverter.convert(page, entity, imageInfo)
    }

    @Test
    fun testConvertIfThumbUrlBlank() {
        `when`(imageInfo.getMetadata()).thenReturn(metadata)
        `when`(imageInfo.getThumbUrl()).thenReturn("")
        `when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        `when`(metadata.licenseUrl()).thenReturn("licenseUrl")
        `when`(metadata.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        `when`(metadata.artist()).thenReturn("Foo Bar")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals(media.thumbUrl, media.imageUrl, "originalUrl")
    }

    @Test
    fun testConvertIfThumbUrlNotBlank() {
        `when`(imageInfo.getMetadata()).thenReturn(metadata)
        `when`(imageInfo.getThumbUrl()).thenReturn("thumbUrl")
        `when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        `when`(metadata.licenseUrl()).thenReturn("licenseUrl")
        `when`(metadata.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        `when`(metadata.artist()).thenReturn("Foo Bar")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals(media.thumbUrl, "thumbUrl")
    }

    @Test
    fun `test converting artist value (author) with html links`() {
        `when`(imageInfo.getMetadata()).thenReturn(metadata)
        `when`(imageInfo.getThumbUrl()).thenReturn("thumbUrl")
        `when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        `when`(metadata.licenseUrl()).thenReturn("licenseUrl")
        `when`(metadata.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        `when`(metadata.artist()).thenReturn("<a href=\"//commons.wikimedia.org/wiki/User:Foo_Bar\" title=\"Foo Bar\">Foo Bar</a>")
        // Artist values like above is very common, found in file pages created via UploadWizard
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals("Foo Bar", media.author)
    }

    @Test
    fun `test convert artist value (author) in plain text`() {
        `when`(imageInfo.getMetadata()).thenReturn(metadata)
        `when`(imageInfo.getThumbUrl()).thenReturn("thumbUrl")
        `when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        `when`(metadata.licenseUrl()).thenReturn("licenseUrl")
        `when`(metadata.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        `when`(metadata.artist()).thenReturn("Foo Bar")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals("Foo Bar", media.author)
    }
    @Test
    fun `test convert artist value (author) containing red link`() {
        `when`(imageInfo.getMetadata()).thenReturn(metadata)
        `when`(imageInfo.getThumbUrl()).thenReturn("thumbUrl")
        `when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        `when`(metadata.licenseUrl()).thenReturn("licenseUrl")
        `when`(metadata.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        `when`(metadata.artist()).thenReturn("<a href=\"/w/index.php?title=User:Foo&action=edit&redlink=1\" class=\"new\" title=\"User:Foo (page does not exist)\">Foo</a>")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals("Foo", media.author)
    }

    @Test
    fun `test convert carries the api media type`() {
        whenever(imageInfo.getMetadata()).thenReturn(metadata)
        whenever(imageInfo.getThumbUrl()).thenReturn(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/" +
                "Big_Buck_Bunny_medium.ogv/500px--Big_Buck_Bunny_medium.ogv.jpg",
        )
        whenever(imageInfo.getOriginalUrl()).thenReturn(
            "https://upload.wikimedia.org/wikipedia/commons/4/41/Big_Buck_Bunny_medium.ogv",
        )
        whenever(metadata.licenseUrl()).thenReturn("https://creativecommons.org/licenses/by/3.0")
        whenever(metadata.dateTime()).thenReturn("2010-01-16 17:34:35")
        whenever(metadata.artist()).thenReturn("(c) copyright Blender Foundation\\www.bigbuckbunny.org")
        whenever(page.title()).thenReturn("File:Big Buck Bunny medium.ogv")
        whenever(imageInfo.getMimeType()).thenReturn("application/ogg")
        whenever(imageInfo.getMediaType()).thenReturn("VIDEO")

        media = mediaConverter.convert(page, entity, imageInfo)

        assertEquals("application/ogg", media.mimeType)
        assertEquals(MediaType.VIDEO, media.mediaType)
    }
}
