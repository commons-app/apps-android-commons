package fr.free.nrw.commons;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {
    @Test public void stripLocalizedStringPass() {
        Assert.assertThat(Utils.stripLocalizedString("Hello"), is("Hello"));
    }

    @Test public void stripLocalizedStringJa() {
        Assert.assertThat(Utils.stripLocalizedString("\"こんにちは\"@ja"), is("こんにちは"));
    }
}
