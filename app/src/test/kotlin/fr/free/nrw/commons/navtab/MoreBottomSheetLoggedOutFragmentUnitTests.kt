package fr.free.nrw.commons.navtab

import android.os.Looper
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import fr.free.nrw.commons.R

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MoreBottomSheetLoggedOutFragmentUnitTests {
    private lateinit var scenario: FragmentScenario<MoreBottomSheetLoggedOutFragment>

    @Before
    fun setUp() {
        scenario = launchFragmentInContainer(
            initialState = Lifecycle.State.RESUMED,
            themeResId = R.style.LightAppTheme
        ) {
            MoreBottomSheetLoggedOutFragment()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testOnSettingsClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { it.onSettingsClicked() }
    }

    @Test
    @Throws(Exception::class)
    fun testOnAboutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { it.onAboutClicked() }
    }

    @Test
    @Throws(Exception::class)
    fun testOnFeedbackClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { it.onFeedbackClicked() }
    }

    @Test
    @Throws(Exception::class)
    fun testOnLogoutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment { it.onLogoutClicked() }
    }
}