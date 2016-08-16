package fr.free.nrw.commons;
import org.junit.Test;

import fr.free.nrw.commons.upload.UploadController;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test public void fixExtensionJpegToJpg() {
        assertEquals("SampleFile.jpg", Utils.fixExtension("SampleFile.jpeg", "jpeg"));
    }

    @Test public void fixExtensionJpgToJpg() {
        assertEquals("SampleFile.jpg", Utils.fixExtension("SampleFile.jpg", "jpg"));
    }

    @Test public void fixExtensionPngToPng() {
        assertEquals("SampleFile.png", Utils.fixExtension("SampleFile.png", "png"));
    }

    @Test public void fixExtensionEmptyToJpg() {
        assertEquals("SampleFile.jpg", Utils.fixExtension("SampleFile", "jpg"));
    }

    @Test public void fixExtensionJpgNotExtension() {
        assertEquals("SampleFileJpg.jpg", Utils.fixExtension("SampleFileJpg", "jpg"));
    }
}
