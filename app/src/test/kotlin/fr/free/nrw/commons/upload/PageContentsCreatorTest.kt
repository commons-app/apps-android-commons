package fr.free.nrw.commons.upload

import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.settings.Prefs.Licenses
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class PageContentsCreatorTest {
    /**
     * A date that doesn't come from EXIF is written with Western digits even when the device language
     * writes numbers differently
     */
    @Test
    fun createFromWritesDateWithWesternDigits() {
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("bn"))
        try {
            val contribution =
                Contribution(
                    media = Media(license = Licenses.CC_BY_SA_4),
                    dateCreated = GregorianCalendar(2023, Calendar.NOVEMBER, 7).time,
                    dateCreatedSource = "",
                )

            val pageContents = PageContentsCreator(ApplicationProvider.getApplicationContext()).createFrom(contribution)

            assertTrue(pageContents, pageContents.contains("|date=2023-11-07\n"))
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }
}
