package fr.free.nrw.commons.settings

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

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

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).create().get()
        context = RuntimeEnvironment.application.applicationContext

        fragment = SettingsFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        Whitebox.setInternalState(fragment, "recentLanguagesDao", recentLanguagesDao)
        Whitebox.setInternalState(fragment, "recentLanguagesTextView",
            recentLanguagesTextView)
        Whitebox.setInternalState(fragment, "separator", separator)
        Whitebox.setInternalState(fragment, "languageHistoryListView",
            languageHistoryListView)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testRequestExternalStoragePermissions() {
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "requestExternalStoragePermissions"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testTelemetryOptInOut() {
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "telemetryOptInOut",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckPermissionsAndSendLogs() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "checkPermissionsAndSendLogs"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentLanguageCode() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "getCurrentLanguageCode",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "")
    }

    @Test
    @Throws(Exception::class)
    fun testSaveLanguageValueCase_appUiDefaultLanguagePref() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "saveLanguageValue",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "", "appUiDefaultLanguagePref")
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareAppLanguages when recently used languages is empty`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "prepareAppLanguages",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment,  "appUiDefaultLanguagePref")
        verify(recentLanguagesDao, times(1)).recentLanguages
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareAppLanguages when recently used languages is not empty`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        whenever(recentLanguagesDao.recentLanguages)
            .thenReturn(
                mutableListOf(Language("English", "en"),
                Language("English", "en"),
                Language("English", "en"),
                Language("English", "en"),
                Language("English", "en"),
                Language("English", "en"))
            )
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "prepareAppLanguages",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment,  "appUiDefaultLanguagePref")
        verify(recentLanguagesDao, times(2)).recentLanguages
    }

    @Test
    @Throws(Exception::class)
    fun testSaveLanguageValueCase_descriptionDefaultLanguagePref() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "saveLanguageValue",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "", "descriptionDefaultLanguagePref")
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveRecentLanguages() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = SettingsFragment::class.java.getDeclaredMethod(
            "removeRecentLanguages"
        )
        method.isAccessible = true
        method.invoke(fragment)
        verify(recentLanguagesTextView, times(1)).visibility = any()
        verify(separator, times(1)).visibility = any()
        verify(languageHistoryListView, times(1)).visibility = any()
    }

}