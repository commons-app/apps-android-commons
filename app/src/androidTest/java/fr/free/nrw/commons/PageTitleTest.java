package fr.free.nrw.commons;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URLEncoder;

import static org.hamcrest.CoreMatchers.is;

// TODO: use Robolectric and make it runnable without a connected device
@RunWith(AndroidJUnit4.class)
public class PageTitleTest {
    @Test public void displayTextShouldNotBeUnderscored() {
        Assert.assertThat(new PageTitle("Ex_1 ").getDisplayText(),
                is("Ex 1"));
    }

    @Test public void moreThanTwoColons() {
        Assert.assertThat(new PageTitle("File:sample:a.jpg").getPrefixedText(),
                is("File:Sample:a.jpg"));
    }

    @Test public void getTextShouldReturnWithoutNamespace() {
        Assert.assertThat(new PageTitle("File:sample.jpg").getText(),
                is("Sample.jpg"));
    }


    @Test public void capitalizeNameAfterNamespace() {
        Assert.assertThat(new PageTitle("File:sample.jpg").getPrefixedText(),
                is("File:Sample.jpg"));
    }

    @Test public void prefixedTextShouldBeUnderscored() {
        Assert.assertThat(new PageTitle("Ex 1 ").getPrefixedText(),
                is("Ex_1"));
    }

    @Test public void getMobileUriForTest() {
        Assert.assertThat(new PageTitle("Test").getMobileUri().toString(),
                is("https://commons.m.wikimedia.org/wiki/Test"));
    }

    @Test public void spaceBecomesUnderscoreInUri() {
        Assert.assertThat(new PageTitle("File:Ex 1.jpg").getCanonicalUri().toString(),
                is("https://commons.wikimedia.org/wiki/File:Ex_1.jpg"));
    }

    @Test public void leaveSubpageNamesUncapitalizedInUri() {
        Assert.assertThat(new PageTitle("User:Ex/subpage").getCanonicalUri().toString(),
                is("https://commons.wikimedia.org/wiki/User:Ex/subpage"));
    }

    @Test public void unicodeUri() throws Throwable {
        Assert.assertThat(new PageTitle("User:例").getCanonicalUri().toString(),
                is("https://commons.wikimedia.org/wiki/User:" + URLEncoder.encode("例", "utf-8")));
    }
}
