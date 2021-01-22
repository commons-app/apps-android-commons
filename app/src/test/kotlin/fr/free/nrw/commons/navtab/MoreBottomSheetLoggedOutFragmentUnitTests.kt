package fr.free.nrw.commons.navtab

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MoreBottomSheetLoggedOutFragmentUnitTests {

    private lateinit var fragment: MoreBottomSheetLoggedOutFragment

    private lateinit var context: Context

    @Before
    fun setUp() {

        context = RuntimeEnvironment.application.applicationContext

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
    fun testOnTutorialClicked() {
        fragment.onTutorialClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnSettingsClicked() {
        fragment.onSettingsClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnAboutClicked() {
        fragment.onAboutClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnFeedbackClicked() {
        fragment.onFeedbackClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnLogoutClicked() {
        fragment.onLogoutClicked()
    }

}