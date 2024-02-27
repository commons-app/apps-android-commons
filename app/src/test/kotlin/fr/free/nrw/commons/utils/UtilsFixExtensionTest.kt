package fr.free.nrw.commons.utils

import fr.free.nrw.commons.Utils.fixExtension
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

class UtilsFixExtensionTest {

    @Test
    fun jpegResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpeg", "jpeg")))
    }

    @Test
    fun capitalJpegWithNoHintResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.JPEG", null)))
    }

    @Test
    fun jpegWithBogusHintResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpeg", null)))
    }

    @Test
    fun jpegToCapitalJpegResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpeg", "JPEG")))
    }

    @Test
    fun jpgToJpegResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpg", "jpeg")))
    }

    @Test
    fun jpegToJpgResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpeg", "jpg")))
    }

    @Test
    fun jpgRemainsJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile.jpg", "jpg")))
    }

    @Test
    fun pngRemainsPng() {
        assertThat("SampleFile.png", equalTo(fixExtension("SampleFile.png", "png")))
    }

    @Test
    fun jpgHintResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile", "jpg")))
    }

    @Test
    fun jpegHintResultsInJpg() {
        assertThat("SampleFile.jpg", equalTo(fixExtension("SampleFile", "jpeg")))
    }

    @Test
    fun dotLessJpgToJpgResultsInJpg() {
        assertThat("SAMPLEjpg.jpg", equalTo(fixExtension("SAMPLEjpg", "jpg")))
    }

    @Test
    fun inWordJpegToJpgResultsInJpg() {
        assertThat("X.jpeg.SAMPLE.jpg", equalTo(fixExtension("X.jpeg.SAMPLE", "jpg")))
    }

    @Test
    fun noExtensionShouldResultInJpg() {
        assertThat("Sample.jpg", equalTo(fixExtension("Sample", null)))
    }

    @Test
    fun extensionAlreadyInTitleShouldRemain() {
        assertThat("Sample.jpg", equalTo(fixExtension("Sample.jpg", null)))
    }
}
