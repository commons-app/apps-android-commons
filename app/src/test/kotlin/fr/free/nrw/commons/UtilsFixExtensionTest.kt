package fr.free.nrw.commons

import fr.free.nrw.commons.Utils.fixExtension
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsFixExtensionTest {

    @Test
    fun jpegResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpeg", "jpeg"))
    }

    @Test
    fun capitalJpegWithNoHintResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.JPEG", null))
    }

    @Test
    fun jpegWithBogusHintResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpeg", null))
    }

    @Test
    fun jpegToCapitalJpegResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpeg", "JPEG"))
    }

    @Test
    fun jpgToJpegResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpg", "jpeg"))
    }

    @Test
    fun jpegToJpgResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpeg", "jpg"))
    }

    @Test
    fun jpgRemainsJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile.jpg", "jpg"))
    }

    @Test
    fun pngRemainsPng() {
        assertEquals("SampleFile.png", fixExtension("SampleFile.png", "png"))
    }

    @Test
    fun jpgHintResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile", "jpg"))
    }

    @Test
    fun jpegHintResultsInJpg() {
        assertEquals("SampleFile.jpg", fixExtension("SampleFile", "jpeg"))
    }

    @Test
    fun dotLessJpgToJpgResultsInJpg() {
        assertEquals("SAMPLEjpg.jpg", fixExtension("SAMPLEjpg", "jpg"))
    }

    @Test
    fun inWordJpegToJpgResultsInJpg() {
        assertEquals("X.jpeg.SAMPLE.jpg", fixExtension("X.jpeg.SAMPLE", "jpg"))
    }
}
