package fr.free.nrw.commons

import fr.free.nrw.commons.upload.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.*

class FileUtilsTest {
    @Test
    fun copiedFileIsIdenticalToSource() {
        val source = File.createTempFile("temp", "")
        val dest = File.createTempFile("temp", "")
        writeToFile(source, "Hello, World")

        FileUtils.copy(FileInputStream(source), FileOutputStream(dest))

        assertEquals(getString(source), getString(dest))
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
}
