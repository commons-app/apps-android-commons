package fr.free.nrw.commons;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


public class UtilsTest {

    @Test public void fixExtensionJpegToJpeg() {
        assertThat(Utils.fixExtension("SampleFile.jpeg", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionJpegToJpg() {
        assertThat(Utils.fixExtension("SampleFile.JPEG", null), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionNull() {
        assertThat(Utils.fixExtension("SampleFile.jpeg", "JPEG"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionJpgToJpeg() {
        assertThat(Utils.fixExtension("SampleFile.jpg", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionJpgToJpg() {
        assertThat(Utils.fixExtension("SampleFile.jpg", "jpg"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionPngToPng() {
        assertThat(Utils.fixExtension("SampleFile.png", "png"), is("SampleFile.png"));
    }

    @Test public void fixExtensionEmptyToJpg() {
        assertThat(Utils.fixExtension("SampleFile", "jpg"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionEmptyToJpeg() {
        assertThat(Utils.fixExtension("SampleFile", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void fixExtensionJpgNotExtension() {
        assertThat(Utils.fixExtension("SAMPLEjpg", "jpg"), is("SAMPLEjpg.jpg"));
    }

    @Test public void fixExtensionJpegNotExtension() {
        assertThat(Utils.fixExtension("SAMPLE.jpeg.SAMPLE", "jpg"), is("SAMPLE.jpeg.SAMPLE.jpg"));
    }

    @Test public void stripLocalizedStringPass() {
        assertThat(Utils.stripLocalizedString("Hello"), is("Hello"));
    }

    @Test public void stripLocalizedStringJa() {
        assertThat(Utils.stripLocalizedString("\"こんにちは\"@ja"), is("こんにちは"));
    }
}
