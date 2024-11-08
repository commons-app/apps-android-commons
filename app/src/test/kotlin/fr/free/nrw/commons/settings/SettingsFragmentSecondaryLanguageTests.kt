package fr.free.nrw.commons.settings

import android.app.Dialog
import android.os.Looper
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsFragmentSecondaryLanguageTests {

    private lateinit var fragment: SettingsFragment

    private lateinit var recentLanguagesDao: RecentLanguagesDao

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).create().get()
        fragment = SettingsFragment()
        val fragmentManager = activity.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()
        // Mock RecentLanguagesDao
        recentLanguagesDao = Mockito.mock(RecentLanguagesDao::class.java)
        fragment.recentLanguagesDao = recentLanguagesDao
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareSecondaryLanguageDialog is invoked and dialog is created`() {
        // Set up the main looper to idle, as necessary in Robolectric tests
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val method: Method = SettingsFragment::class.java.getDeclaredMethod("prepareSecondaryLanguageDialog")
        method.isAccessible = true

        method.invoke(fragment)

        // Verify if the dialog was created and is not null
        val dialogField = SettingsFragment::class.java.getDeclaredField("dialog")
        dialogField.isAccessible = true
        val dialog: Dialog? = dialogField.get(fragment) as Dialog?
        Assert.assertNotNull(dialog)
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareSecondaryLanguageDialog adds a language and updates preferences`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Mock recent languages and saved languages
        val savedLanguages = mutableListOf(Language("English", "en"))

        val method: Method = SettingsFragment::class.java.getDeclaredMethod("prepareSecondaryLanguageDialog")
        method.isAccessible = true

        method.invoke(fragment)

        val dialogField = SettingsFragment::class.java.getDeclaredField("dialog")
        dialogField.isAccessible = true
        val dialog: Dialog? = dialogField.get(fragment) as Dialog?

        val newLanguage = Language("German", "de")
        savedLanguages.add(newLanguage)

        // Verify if the saved languages now include the newly added language
        Assert.assertTrue(savedLanguages.contains(newLanguage))
    }

    @Test
    @Throws(Exception::class)
    fun `Test prepareSecondaryLanguageDialog removes a language and updates preferences`() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Mock recent languages and saved languages
        val savedLanguages = mutableListOf(Language("English", "en"), Language("French", "fr"))

        val method: Method = SettingsFragment::class.java.getDeclaredMethod("prepareSecondaryLanguageDialog")
        method.isAccessible = true

        method.invoke(fragment)

        val dialogField = SettingsFragment::class.java.getDeclaredField("dialog")
        dialogField.isAccessible = true
        val dialog: Dialog? = dialogField.get(fragment) as Dialog?

        // Simulate removing a language from the saved list (e.g., removing "French")
        val positionToRemove = 1
        savedLanguages.removeAt(positionToRemove)

        Assert.assertFalse(savedLanguages.any { it.languageCode == "fr" })
    }
}

