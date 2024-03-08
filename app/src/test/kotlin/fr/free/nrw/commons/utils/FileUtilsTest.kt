package fr.free.nrw.commons.utils

import com.nhaarman.mockitokotlin2.mock
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.FileUtilsWrapper
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import java.io.*

class FileUtilsTest {
    @Test
    fun deleteFile() {
        val file = File.createTempFile("testfile", "")
        file.writeText("Hello, World")

        assertThat(true, equalTo( file.exists()))
        assertThat(true, equalTo( FileUtils.deleteFile(file)))
        assertThat(false, equalTo( file.exists()))
    }

    @Test
    fun testSHA1() {
        val fileUtilsWrapper = FileUtilsWrapper(mock())

        assertThat(
                "907d14fb3af2b0d4f18c2d46abe8aedce17367bd",
                equalTo(fileUtilsWrapper.getSHA1("Hello, World".byteInputStream()))
        )

        assertThat(
                "8b971da6347bd126872ea2f4f8d394e70c74073a",
                equalTo(fileUtilsWrapper.getSHA1("apps-android-commons".byteInputStream()))
        )

        assertThat(
                "e9d30f5a3a82792b9d79c258366bd53207ceaeb3",
                equalTo(fileUtilsWrapper.getSHA1("domdomegg was here".byteInputStream()))
        )

        assertThat(
                "96e733a3e59261c0621ba99be5bd10bb21abe53e",
                equalTo(fileUtilsWrapper.getSHA1("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=".byteInputStream()))
        )
    }
}
