package fr.free.nrw.commons.settings

import android.app.Dialog
import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.SettingsFragment.Companion.createLocale
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsFragmentUnitTests {
    private lateinit var fragment: SettingsFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context

    @Mock
    private lateinit var recentLanguagesDao: RecentLanguagesDao

    @Mock
    private lateinit var recentLanguagesTextView: TextView

    @Mock
    private lateinit var separator: View

    @Mock
    private lateinit var languageHistoryListView: ListView

    @Mock
    private lateinit var adapterView: AdapterView<RecentLanguagesAdapter>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).create().get()
        context = ApplicationProvider.getApplicationContext()

        fragment = SettingsFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        Whitebox.setInternalState(fragment, "recentLanguagesDao", recentLanguagesDao)
        Whitebox.setInternalState(
            fragment.languageController,
            "recentLanguagesTextView",
            recentLanguagesTextView,
        )
        Whitebox.setInternalState(fragment.languageController, "separator", separator)
        Whitebox.setInternalState(
            fragment.languageController,
            "languageHistoryListView",
            languageHistoryListView,
        )
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testRequestExternalStoragePermissions() {
        val method: Method =
            SettingsFragment::class.java.getDeclaredMethod(
                "requestExternalStoragePermissions",
            )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckPermissionsAndSendLogs() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            SettingsFragment::class.java.getDeclaredMethod(
                "checkPermissionsAndSendLogs",
            )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentLanguageCode() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.languageController.getCurrentLanguageCode("")
    }

    @Test
    @Throws(Exception::class)
    fun testSaveLanguageValueCase_appUiDefaultLanguagePref() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.languageController.saveLanguageValue("", "appUiDefaultLanguagePref")
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareAppLanguages when recently used languages is empty`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.languageController.prepareAppLanguages("appUiDefaultLanguagePref")
        verify(recentLanguagesDao, times(1)).getRecentLanguages()
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareAppLanguages when recently used languages is not empty`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        whenever(recentLanguagesDao.getRecentLanguages())
            .thenReturn(
                mutableListOf(
                    Language("English", "en"),
                    Language("English", "en"),
                    Language("English", "en"),
                    Language("English", "en"),
                    Language("English", "en"),
                    Language("English", "en"),
                ),
            )
        fragment.languageController.prepareAppLanguages("appUiDefaultLanguagePref")
        verify(recentLanguagesDao, times(2)).getRecentLanguages()
    }

    @Test
    @Throws(Exception::class)
    fun testSaveLanguageValueCase_descriptionDefaultLanguagePref() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.languageController.saveLanguageValue("", "descriptionDefaultLanguagePref")
    }

    @Test
    @Throws(Exception::class)
    fun testHideRecentLanguagesSection() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.languageController.hideRecentLanguagesSection()
        verify(recentLanguagesTextView, times(1)).visibility = any()
        verify(separator, times(1)).visibility = any()
        verify(languageHistoryListView, times(1)).visibility = any()
    }

    @Test
    @Throws(Exception::class)
    fun testOnRecentLanguageClicked() {
        whenever(recentLanguagesDao.findRecentLanguage(any()))
            .thenReturn(true)
        whenever(adapterView.adapter)
            .thenReturn(
                RecentLanguagesAdapter(
                    context,
                    listOf(Language("English", "en")),
                    mutableMapOf(),
                ),
            )
        fragment.languageController.onRecentLanguageClicked("test", Mockito.mock(Dialog::class.java), adapterView, 0)
        verify(recentLanguagesDao, times(1)).findRecentLanguage(any())
        verify(adapterView, times(2)).adapter
    }

    @Test
    fun `Test setUpRecentLanguagesSection when list is empty`() {
        fragment.languageController.setUpRecentLanguagesSection(emptyList<Language>(), hashMapOf<Int, String>())
        verify(languageHistoryListView, times(1)).visibility = View.GONE
        verify(separator, times(1)).visibility = View.GONE
        verify(recentLanguagesTextView, times(1)).visibility = View.GONE
    }

    @Test
    fun `Test setUpRecentLanguagesSection when list is not empty`() {
        fragment.languageController.setUpRecentLanguagesSection(
            listOf(
                Language("Bengali", "bn"),
                Language("Bengali", "bn"),
                Language("Bengali", "bn"),
                Language("Bengali", "bn"),
                Language("Bengali", "bn"),
                Language("Bengali", "bn"),
            ),
            hashMapOf<Int, String>(),
        )
        verify(languageHistoryListView, times(1)).visibility = View.VISIBLE
        verify(separator, times(1)).visibility = View.VISIBLE
        verify(recentLanguagesTextView, times(1)).visibility = View.VISIBLE
    }

    @Test
    fun testCreateLocaleWithLanguageCode() {
        val locale: Locale = createLocale("en")

        assertEquals("en", locale.language)
        assertEquals("", locale.country)
        assertEquals("", locale.variant)
    }

    @Test
    fun testCreateLocaleWithLanguageAndCountryCode() {
        val locale: Locale = createLocale("zh-CN")

        assertEquals("zh", locale.language)
        assertEquals("CN", locale.country)
        assertEquals("", locale.variant)
    }

    @Test
    fun testCreateLocaleWithLanguageCountryAndVariantCode() {
        val locale: Locale = createLocale("pt-BR-variant")

        assertEquals("pt", locale.language)
        assertEquals("BR", locale.country)
        assertEquals("variant", locale.variant)
    }
}
