package fr.free.nrw.commons.utils

import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.FileUtilsWrapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.*

class FileUtilsTest {
    @Test
    fun deleteFile() {
        val file = File.createTempFile("testfile", "")
        writeToFile(file, "Hello, World")

        assertEquals(true, file.exists())
        assertEquals(true, FileUtils.deleteFile(file))
        assertEquals(false, file.exists())
    }

    @Test
    fun testSHA1() {
        val fileUtilsWrapper = FileUtilsWrapper()

        assertEquals(
                "907d14fb3af2b0d4f18c2d46abe8aedce17367bd",
                fileUtilsWrapper.getSHA1(toInputStream("Hello, World"))
        )

        assertEquals(
                "8b971da6347bd126872ea2f4f8d394e70c74073a",
                fileUtilsWrapper.getSHA1(toInputStream("apps-android-commons"))
        )

        assertEquals(
                "e9d30f5a3a82792b9d79c258366bd53207ceaeb3",
                fileUtilsWrapper.getSHA1(toInputStream("domdomegg was here"))
        )

        assertEquals(
                "96e733a3e59261c0621ba99be5bd10bb21abe53e",
                fileUtilsWrapper.getSHA1(toInputStream("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="))
        )
    }

    private fun writeToFile(file: File, s: String) {
        val buf = BufferedOutputStream(FileOutputStream(file))
        buf.write(s.toByteArray())
        buf.close()
    }

    private fun getString(file: File): String {
        val bytes = ByteArray(file.length().toInt())
        val buf = BufferedInputStream(FileInputStream(file))
        buf.read(bytes, 0, bytes.size)
        buf.close()
        return String(bytes)
    }

    private fun toInputStream(str: String) : InputStream {
        return ByteArrayInputStream(str.toByteArray(Charsets.UTF_8))
    }
}
