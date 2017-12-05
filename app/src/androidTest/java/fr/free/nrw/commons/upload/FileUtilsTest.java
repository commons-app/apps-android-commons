package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FileUtilsTest {
    @Test
    public void isSelfOwned() throws Exception {
        Uri uri = Uri.parse("content://fr.free.nrw.commons.provider/document/1");
        boolean selfOwned = FileUtils.isSelfOwned(InstrumentationRegistry.getTargetContext(), uri);
        assertThat(selfOwned, is(true));
    }

    @Test
    public void isNotSelfOwned() throws Exception {
        Uri uri = Uri.parse("content://com.android.providers.media.documents/document/1");
        boolean selfOwned = FileUtils.isSelfOwned(InstrumentationRegistry.getTargetContext(), uri);
        assertThat(selfOwned, is(false));
    }
}