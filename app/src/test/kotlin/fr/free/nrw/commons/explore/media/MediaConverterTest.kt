package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.gallery.ExtMetadata
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
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
        Mockito.`when`(imageInfo.getMetadata()).thenReturn(metadata)
        Mockito.`when`(imageInfo.getThumbUrl()).thenReturn("")
        Mockito.`when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        Mockito.`when`(imageInfo.getMetadata()?.licenseUrl()).thenReturn("licenseUrl")
        Mockito.`when`(imageInfo.getMetadata()?.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals(media.thumbUrl, media.imageUrl, "originalUrl")
    }

    @Test
    fun testConvertIfThumbUrlNotBlank() {
        Mockito.`when`(imageInfo.getMetadata()).thenReturn(metadata)
        Mockito.`when`(imageInfo.getThumbUrl()).thenReturn("thumbUrl")
        Mockito.`when`(imageInfo.getOriginalUrl()).thenReturn("originalUrl")
        Mockito.`when`(imageInfo.getMetadata()?.licenseUrl()).thenReturn("licenseUrl")
        Mockito.`when`(imageInfo.getMetadata()?.dateTime()).thenReturn("yyyy-MM-dd HH:mm:ss")
        media = mediaConverter.convert(page, entity, imageInfo)
        assertEquals(media.thumbUrl, "thumbUrl")
    }
}
