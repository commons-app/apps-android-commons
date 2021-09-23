package fr.free.nrw.commons.settings

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
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

}