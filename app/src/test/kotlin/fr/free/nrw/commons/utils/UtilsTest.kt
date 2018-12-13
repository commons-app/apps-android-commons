package fr.free.nrw.commons.utils

import fr.free.nrw.commons.Utils
import org.junit.Assert
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as _is

class UtilsTest {
    @Test fun `strip nothing from non-localized string`() {
        Assert.assertThat(Utils.stripLocalizedString("Hello"), _is("Hello"))
    }

    @Test fun `strip tag from Japanese string`() {
        Assert.assertThat(Utils.stripLocalizedString("\"こんにちは\"@ja"), _is("こんにちは"))
    }

    @Test fun `capitalize first letter`() {
        Assert.assertThat(Utils.capitalize("hello"), _is("Hello"))
    }

    @Test fun `capitalize - pass all-capital string as it is`() {
        Assert.assertThat(Utils.capitalize("HELLO"), _is("HELLO"))
    }

    @Test fun `capitalize - pass numbers`() {
        Assert.assertThat(Utils.capitalize("12x"), _is("12x"))
    }

    @Test fun `capitalize - pass Japanase characters`() {
        Assert.assertThat(Utils.capitalize("こんにちは"), _is("こんにちは"))
    }

    @Test fun `capitalize does not fail on empty string`() {
        Assert.assertThat(Utils.capitalize(""), _is(""))
    }
}
