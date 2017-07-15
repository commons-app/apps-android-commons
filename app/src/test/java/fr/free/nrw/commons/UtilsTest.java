package fr.free.nrw.commons;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

public class UtilsTest {
    @Test public void stripLocalizedStringPass() {
        Assert.assertThat(Utils.stripLocalizedString("Hello"), is("Hello"));
    }

    @Test public void stripLocalizedStringJa() {
        Assert.assertThat(Utils.stripLocalizedString("\"こんにちは\"@ja"), is("こんにちは"));
    }

    @Test public void capitalizeLowercase() {
        Assert.assertThat(Utils.capitalize("hello"), is("Hello"));
    }

    @Test public void capitalizeFullCaps() {
        Assert.assertThat(Utils.capitalize("HELLO"), is("HELLO"));
    }

    @Test public void capitalizeNumbersPass() {
        Assert.assertThat(Utils.capitalize("12x"), is("12x"));
    }

    @Test public void capitalizeJaPass() {
        Assert.assertThat(Utils.capitalize("こんにちは"), is("こんにちは"));
    }
}
