package fr.free.nrw.commons;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;

// TODO: use Robolectric and make it runnable without a connected device
@RunWith(AndroidJUnit4.class)
public class MediaTest {
    @Test public void displayTitleShouldStripExtension() {
        Media m = new Media("File:Example.jpg");
        Assert.assertThat(m.getDisplayTitle(), is("Example"));
    }

    @Test public void displayTitleShouldUseSpaceForUnderscore() {
        Media m = new Media("File:Example 1_2.jpg");
        Assert.assertThat(m.getDisplayTitle(), is("Example 1 2"));
    }
}
