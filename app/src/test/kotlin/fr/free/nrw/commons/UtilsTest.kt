package fr.free.nrw.commons

import fr.free.nrw.commons.settings.Licenses.Constants.CC0_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_3_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_4_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_URL
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_TEMPLATE
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_4_URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class UtilsTest {
    @Test
    fun getPageTitle() {
        with(getPageTitle("example")) {
            assertEquals(BuildConfig.COMMONS_URL, wikiSite.url())
            assertEquals("example", text)
            assertNull(thumbUrl)
            assertNull(properties)
        }
    }

    @Test
    fun licenseNameForKnownLicenses() {
        assertEquals(R.string.license_name_cc_by, licenseNameFor(CC_BY_3_ID))
        assertEquals(R.string.license_name_cc_by_four, licenseNameFor(CC_BY_4_ID))
        assertEquals(R.string.license_name_cc_by_sa, licenseNameFor(CC_BY_SA_3_ID))
        assertEquals(R.string.license_name_cc_by_sa_four, licenseNameFor(CC_BY_SA_4_ID))
        assertEquals(R.string.license_name_cc0, licenseNameFor(CC0_ID))
    }

    @Test(expected = IllegalStateException::class)
    fun licenseNameForUnknownLicense() {
        licenseNameFor("unknown-license")
    }

    @Test(expected = IllegalStateException::class)
    fun licenseNameForNullLicense() {
        licenseNameFor(null)
    }

    @Test
    fun licenseUrlForKnownLicenses() {
        assertEquals(CC_BY_3_URL, licenseUrlFor(CC_BY_3_ID))
        assertEquals(CC_BY_4_URL, licenseUrlFor(CC_BY_4_ID))
        assertEquals(CC_BY_SA_3_URL, licenseUrlFor(CC_BY_SA_3_ID))
        assertEquals(CC_BY_SA_4_URL, licenseUrlFor(CC_BY_SA_4_ID))
        assertEquals(CC0_URL, licenseUrlFor(CC0_ID))
    }

    @Test(expected = IllegalStateException::class)
    fun licenseUrlForUnknownLicense() {
        licenseUrlFor("unknown-license")
    }

    @Test(expected = IllegalStateException::class)
    fun licenseUrlForNullLicense() {
        licenseUrlFor(null)
    }

    @Test
    fun licenseTemplateForKnownLicenses() {
        assertEquals(CC_BY_3_TEMPLATE, licenseTemplateFor(CC_BY_3_ID))
        assertEquals(CC_BY_4_TEMPLATE, licenseTemplateFor(CC_BY_4_ID))
        assertEquals(CC_BY_SA_3_TEMPLATE, licenseTemplateFor(CC_BY_SA_3_ID))
        assertEquals(CC_BY_SA_4_TEMPLATE, licenseTemplateFor(CC_BY_SA_4_ID))
        assertEquals(CC0_TEMPLATE, licenseTemplateFor(CC0_ID))
    }

    @Test(expected = IllegalStateException::class)
    fun licenseTemplateForUnknownLicense() {
        licenseTemplateFor("unknown-license")
    }

    @Test(expected = IllegalStateException::class)
    fun licenseTemplateForNullLicense() {
        licenseTemplateFor(null)
    }

    @Test
    fun isMonumentsEnabled() {
        val calendar = Calendar.getInstance()
        (1..12).forEach {
            calendar.set(2020, it, 1)
            assertEquals(it == 8, isMonumentsEnabled(calendar.time))
        }
    }

    @Test
    fun wlmMonumentStartAndEnd() {
        assertEquals("1 Sep", wLMStartDate)
        assertEquals("30 Sep", wLMEndDate)
    }
}