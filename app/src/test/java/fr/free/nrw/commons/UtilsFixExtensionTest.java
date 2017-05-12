package fr.free.nrw.commons;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;

public class UtilsFixExtensionTest {

    @Test public void jpegResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpeg", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void capitalJpegWithNoHintResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.JPEG", null), is("SampleFile.jpg"));
    }

    @Test public void jpegWithBogusHintResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpeg", null), is("SampleFile.jpg"));
    }

    @Test public void jpegToCapitalJpegResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpeg", "JPEG"), is("SampleFile.jpg"));
    }

    @Test public void jpgToJpegResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpg", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void jpegToJpgResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpeg", "jpg"), is("SampleFile.jpg"));
    }

    @Test public void jpgRemainsJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile.jpg", "jpg"), is("SampleFile.jpg"));
    }

    @Test public void pngRemainsPng() {
        Assert.assertThat(Utils.fixExtension("SampleFile.png", "png"), is("SampleFile.png"));
    }

    @Test public void jpgHintResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile", "jpg"), is("SampleFile.jpg"));
    }

    @Test public void jpegHintResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SampleFile", "jpeg"), is("SampleFile.jpg"));
    }

    @Test public void dotLessJpgToJpgResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("SAMPLEjpg", "jpg"), is("SAMPLEjpg.jpg"));
    }

    @Test public void inWordJpegToJpgResultsInJpg() {
        Assert.assertThat(Utils.fixExtension("X.jpeg.SAMPLE", "jpg"),is("X.jpeg.SAMPLE.jpg"));
    }
}
