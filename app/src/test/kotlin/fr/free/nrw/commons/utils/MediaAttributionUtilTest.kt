package fr.free.nrw.commons.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.media.IdAndLabels
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class, qualifiers="en-rUS")
class MediaAttributionUtilTest {

    @Mock
    private lateinit var appContext: Context

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getTagLineWithUploaderOnly() {
        val media = mock(Media::class.java)
        whenever(media.user).thenReturn("TestUploader")
        whenever(media.author).thenReturn(null)
        assertEquals("Uploaded by: TestUploader",
            MediaAttributionUtil.getTagLine(media, appContext))
    }

    @Test
    fun `get tag line from same author and uploader`() {
        val media = mock(Media::class.java)
        whenever(media.user).thenReturn("TestUser")
        whenever(media.getAttributedAuthor()).thenReturn("TestUser")
        assertEquals("Created and uploaded by: TestUser",
            MediaAttributionUtil.getTagLine(media, appContext))
    }

    @Test
    fun `get creator name from EN label`() {
        assertEquals("FooBar",
            MediaAttributionUtil.getCreatorName(listOf(IdAndLabels("Q1", mapOf("en" to "FooBar")))))
    }

    @Test
    fun `get creator name from ES label`() {
        assertEquals("FooBar",
            MediaAttributionUtil.getCreatorName(listOf(IdAndLabels("Q2", mapOf("es" to "FooBar")))))
    }

    @Test
    fun `get creator name from EN label and ignore ES label`() {
        assertEquals("Bar",
            MediaAttributionUtil.getCreatorName(listOf(
                IdAndLabels("Q3", mapOf("en" to "Bar", "es" to "Foo")))))
    }

    @Test
    fun `get creator name from two creators`() {
        val name = MediaAttributionUtil.getCreatorName(listOf(
            IdAndLabels("Q1", mapOf("en" to "Foo")),
            IdAndLabels("Q1", mapOf("en" to "Bar"))
        ))
        assertNotNull(name)
        assertTrue(name!!.contains("Foo"))
        assertTrue(name.contains("Bar"))
    }
}