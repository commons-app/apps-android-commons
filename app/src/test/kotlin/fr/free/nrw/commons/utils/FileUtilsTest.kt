package fr.free.nrw.commons.utils

import com.nhaarman.mockitokotlin2.mock
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.FileUtilsWrapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.*

class FileUtilsTest {
    @Test
    fun deleteFile() {
        val file = File.createTempFile("testfile", "")
        file.writeText("Hello, World")

        assertEquals(true, file.exists())
        assertEquals(true, FileUtils.deleteFile(file))
        assertEquals(false, file.exists())
    }

    @Test
    fun testSHA1() {
        val fileUtilsWrapper = FileUtilsWrapper(mock())

        assertEquals(
                "907d14fb3af2b0d4f18c2d46abe8aedce17367bd",
                fileUtilsWrapper.getSHA1("Hello, World".byteInputStream())
        )

        assertEquals(
                "8b971da6347bd126872ea2f4f8d394e70c74073a",
                fileUtilsWrapper.getSHA1("apps-android-commons".byteInputStream())
        )

        assertEquals(
                "e9d30f5a3a82792b9d79c258366bd53207ceaeb3",
                fileUtilsWrapper.getSHA1("domdomegg was here".byteInputStream())
        )

        assertEquals(
                "96e733a3e59261c0621ba99be5bd10bb21abe53e",
                fileUtilsWrapper.getSHA1("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=".byteInputStream())
        )
    }
}
