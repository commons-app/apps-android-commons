package fr.free.nrw.commons.upload

import org.junit.Test
import org.mockito.Mockito.mock
import java.io.FileDescriptor

class GPSExtractorTest {

    @Test
    fun getCoords() {
        val fileDescriptor = mock(FileDescriptor::class.java)
        val gpsExtractor = GPSExtractor(fileDescriptor)
        val coords = gpsExtractor.coords
    }

    @Test
    fun getDecLatitude() {
    }

    @Test
    fun getDecLongitude() {
    }
}