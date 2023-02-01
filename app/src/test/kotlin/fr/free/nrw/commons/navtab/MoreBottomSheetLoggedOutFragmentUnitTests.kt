package fr.free.nrw.commons.navtab

import android.content.Context
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MoreBottomSheetLoggedOutFragmentUnitTests {

    private lateinit var fragment: MoreBottomSheetLoggedOutFragment

    private lateinit var context: Context

    @Before
    fun setUp() {

        context = ApplicationProvider.getApplicationContext()

        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = MoreBottomSheetLoggedOutFragment()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSettingsClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onSettingsClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnAboutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onAboutClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnFeedbackClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onFeedbackClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnLogoutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onLogoutClicked()
    }

}