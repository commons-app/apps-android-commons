package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.CategoryApi
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File

class FileProcessorTest {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var gpsCategoryModel: GpsCategoryModel

    @Mock
    private lateinit var depictsModel: DepictModel

    @Mock
    private lateinit var apiCall: CategoryApi

    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private lateinit var dependencies: FileProcessorDependencies

    private lateinit var fileProcessor: FileProcessor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        dependencies =
            FileProcessorDependencies(
                gpsCategoryModel = gpsCategoryModel,
                depictsModel = depictsModel,
                apiCall = apiCall,
                okHttpJsonApiClient = okHttpJsonApiClient,
            )
        fileProcessor = FileProcessor(context, contentResolver, defaultKvStore, dependencies)
    }

    @Test
    fun testFileProcessorDependenciesNotNull() {
        assertNotNull(dependencies)
        assertNotNull(dependencies.gpsCategoryModel)
        assertNotNull(dependencies.depictsModel)
        assertNotNull(dependencies.apiCall)
        assertNotNull(dependencies.okHttpJsonApiClient)
    }

    @Test
    fun testFileProcessorInitialization() {
        assertNotNull(fileProcessor)
    }

    @Test
    fun redactExifTagsRemovesMotionPhotoVideo() {
        val photo = motionPhoto(tail = fakeVideo, videoLength = fakeVideo.size)
        val withoutVideo = photo.length() - fakeVideo.size

        fileProcessor.redactExifTags(null, emptySet(), photo.absolutePath)
        assertEquals(withoutVideo, photo.length())
        val text = String(photo.readBytes(), Charsets.ISO_8859_1)
        assertTrue(text.contains("GCamera:MotionPhoto=\"0\""))
        assertFalse(text.contains("ftyp"))

        // Running it again (e.g. for the custom selector's SHA1) must not cut the photo itself.
        val once = photo.readBytes()
        fileProcessor.redactExifTags(null, emptySet(), photo.absolutePath)
        assertArrayEquals(once, photo.readBytes())
    }

    @Test
    fun redactExifTagsKeepsPhotoWithoutVideo() {
        val photo = File.createTempFile("plain_", ".jpg").apply { deleteOnExit() }
        File(SAMPLE_JPEG).copyTo(photo, overwrite = true)
        val before = photo.readBytes()

        fileProcessor.redactExifTags(null, emptySet(), photo.absolutePath)
        assertArrayEquals(before, photo.readBytes())
    }

    @Test
    fun redactExifTagsKeepsPhotoWhenVideoIsNotWhereXmpSays() {
        val notVideo = ByteArray(fakeVideo.size)
        val photo = motionPhoto(tail = notVideo, videoLength = notVideo.size)
        val before = photo.readBytes()

        fileProcessor.redactExifTags(null, emptySet(), photo.absolutePath)
        assertArrayEquals(before, photo.readBytes())
    }

    private val fakeVideo = byteArrayOf(0, 0, 0, 0x18) + "ftypisom".toByteArray() + ByteArray(100)

    /**
     * Builds a JPEG laid out like a Google motion photo: XMP that points at a video
     * of [videoLength] bytes, then [tail] appended after the image.
     */
    private fun motionPhoto(
        tail: ByteArray,
        videoLength: Int,
    ): File {
        val jpeg = File(SAMPLE_JPEG).readBytes()
        val xmp =
            "http://ns.adobe.com/xap/1.0/\u0000" +
                "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF><rdf:Description " +
                "GCamera:MotionPhoto=\"1\" GCamera:MotionPhotoVersion=\"1\">" +
                "<Container:Directory><rdf:Seq>" +
                "<rdf:li><Container:Item Item:Semantic=\"Primary\" Item:Mime=\"image/jpeg\"/></rdf:li>" +
                "<rdf:li><Container:Item Item:Mime=\"video/mp4\" Item:Semantic=\"MotionPhoto\" " +
                "Item:Length=\"$videoLength\" Item:Padding=\"0\"/></rdf:li>" +
                "</rdf:Seq></Container:Directory></rdf:Description></rdf:RDF></x:xmpmeta>"
        val xmpBytes = xmp.toByteArray(Charsets.ISO_8859_1)
        val segmentLength = xmpBytes.size + 2
        val app1 =
            byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (segmentLength shr 8).toByte(), segmentLength.toByte()) +
                xmpBytes
        return File.createTempFile("motion_", ".jpg").apply {
            deleteOnExit()
            writeBytes(jpeg.copyOfRange(0, 2) + app1 + jpeg.copyOfRange(2, jpeg.size) + tail)
        }
    }

    private companion object {
        const val SAMPLE_JPEG = "src/test/resources/ImageTest/dark2.jpg"
    }
}
