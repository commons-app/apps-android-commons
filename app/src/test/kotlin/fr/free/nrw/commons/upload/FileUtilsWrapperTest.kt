package fr.free.nrw.commons.upload

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
        whenever(context.noBackupFilesDir).thenReturn(temporaryFolder.newFolder())
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
    fun deleteUploadFileDeletesAFileInTheAppFolders() {
        val copy = File(context.cacheDir, "copy.jpg")
        copy.writeBytes(ByteArray(3))

        FileUtilsWrapper(context).deleteUploadFile(copy)

        assertFalse(copy.exists())
    }

    @Test
    fun deleteUploadFileLeavesFilesOutsideTheAppFolders() {
        val original = temporaryFolder.newFile("original.jpg")

        FileUtilsWrapper(context).deleteUploadFile(original)

        assertTrue(original.exists())
    }

    @Test
    fun getFileChunksDeletesChunksLeftFromAStoppedUpload() {
        val wrapper = FileUtilsWrapper(context)
        val file = temporaryFolder.newFile("next.jpg")
        file.writeBytes(ByteArray(4))
        val leftover = File(File(context.noBackupFilesDir, "UploadChunks").apply { mkdirs() }, "old-chunk")
        leftover.writeBytes(ByteArray(4))

        val chunks = wrapper.getFileChunks(file, chunkSize = 4)

        assertFalse(leftover.exists())
        assertEquals(1, chunks.size)
        assertTrue(chunks[0].path.startsWith(context.noBackupFilesDir.path))
    }

    @Test
    fun getFileChunksReturnsEmptyListForNullFile() {
        assertEquals(
            emptyList<Any>(),
            FileUtilsWrapper(context).getFileChunks(null, chunkSize = 4),
        )
    }
}
