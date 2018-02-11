package fr.free.nrw.commons;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class MediaTest {
    @Test
    public void displayTitleShouldStripExtension() {
        Media m = new Media("File:Example.jpg");
        assertThat(m.getDisplayTitle(), is("Example"));
    }

    @Test
    public void displayTitleShouldUseSpaceForUnderscore() {
        Media m = new Media("File:Example 1_2.jpg");
        assertThat(m.getDisplayTitle(), is("Example 1 2"));
    }
}
