package fr.free.nrw.commons;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URLEncoder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class PageTitleTest {
    @Test
    public void displayTextShouldNotBeUnderscored() {
        assertThat(new PageTitle("Ex_1 ").getDisplayText(),
                is("Ex 1"));
    }

    @Test
    public void moreThanTwoColons() {
        assertThat(new PageTitle("File:sample:a.jpg").getPrefixedText(),
                is("File:Sample:a.jpg"));
    }

    @Test
    public void getTextShouldReturnWithoutNamespace() {
        assertThat(new PageTitle("File:sample.jpg").getText(),
                is("Sample.jpg"));
    }


    @Test
    public void capitalizeNameAfterNamespace() {
        assertThat(new PageTitle("File:sample.jpg").getPrefixedText(),
                is("File:Sample.jpg"));
    }

    @Test
    public void prefixedTextShouldBeUnderscored() {
        assertThat(new PageTitle("Ex 1 ").getPrefixedText(),
                is("Ex_1"));
    }

    @Test
    public void getMobileUriForTest() {
        assertThat(new PageTitle("Test").getMobileUri().toString(),
                is(BuildConfig.MOBILE_HOME_URL + "Test"));
    }

    @Test
    public void spaceBecomesUnderscoreInUri() {
        assertThat(new PageTitle("File:Ex 1.jpg").getCanonicalUri().toString(),
                is(BuildConfig.HOME_URL + "File:Ex_1.jpg"));
    }

    @Test
    public void leaveSubpageNamesUncapitalizedInUri() {
        assertThat(new PageTitle("User:Ex/subpage").getCanonicalUri().toString(),
                is(BuildConfig.HOME_URL + "User:Ex/subpage"));
    }

    @Test
    public void unicodeUri() throws Throwable {
        assertThat(new PageTitle("User:例").getCanonicalUri().toString(),
                is(BuildConfig.HOME_URL + "User:" + URLEncoder.encode("例", "utf-8")));
    }
}
