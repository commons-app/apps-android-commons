package fr.free.nrw.commons.upload

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Test class for [FileUtilsWrapper]
 */
class FileUtilsWrapperTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = mock()

    @Before
    fun setUp() {
        whenever(context.cacheDir).thenReturn(temporaryFolder.newFolder())
    }

    @Test
    fun getFileChunksSplitsFileIntoChunks() {
        val file = temporaryFolder.newFile("upload.jpg")
        val content = ByteArray(10) { it.toByte() }
        file.writeBytes(content)

        val chunks = FileUtilsWrapper(context).getFileChunks(file, chunkSize = 4)

        assertEquals(3, chunks.size)

        val reassembled = chunks.fold(ByteArray(0)) { acc, chunk -> acc + chunk.readBytes() }
        assertTrue(reassembled.contentEquals(content))

        chunks.forEach { it.delete() }
    }

    @Test
    fun getFileChunksReturnsEmptyListForNullFile() {
        assertEquals(
            emptyList<Any>(),
            FileUtilsWrapper(context).getFileChunks(null, chunkSize = 4),
        )
    }
}
