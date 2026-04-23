package fr.free.nrw.commons

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.utils.ConfigUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WelcomeActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<WelcomeActivity>()

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
    }

    @Test
    fun ifBetaShowsSkipButton() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
            composeTestRule.onNodeWithText("Skip Tutorial").assertIsDisplayed()
        }
    }

    @Test
    fun ifProdHidesSkipButton() {
        if (!ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("Skip Tutorial").assertDoesNotExist()
        }
    }

    @Test
    fun testBetaSkipButton() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
            composeTestRule.onNodeWithText("Skip Tutorial").performClick()
            composeTestRule.waitForIdle()
            assertThat(composeTestRule.activity.isDestroyed, equalTo(true))
        }
    }

    @Test
    fun testSwipingOnce() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
        }

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        assertThat(true, equalTo(true))

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        assertThat(true, equalTo(true))
    }

    @Test
    fun testSwipingWholeTutorial() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
        }

        repeat(4) {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
        }
        assertThat(true, equalTo(true))

        repeat(4) {
            composeTestRule.onRoot().performTouchInput { swipeRight() }
        }
        assertThat(true, equalTo(true))
    }

    @Test
    fun swipeBeyondBounds() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
        }

        repeat(4) {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
        }

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        assertThat(true, equalTo(true))

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        assertThat(true, equalTo(true))
    }

    @Test
    fun swipeTillLastAndFinish() {
        if (ConfigUtils.isBetaFlavour) {
            composeTestRule.onNodeWithText("OK").performClick()
        }

        repeat(4) {
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
        }

        composeTestRule.onNodeWithText("YES!").performClick()

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.activity.isDestroyed, equalTo(true))
    }

    @Test
    fun orientationChange() {
        device.setOrientationLeft()
        composeTestRule.waitForIdle()

        device.setOrientationNatural()
        composeTestRule.waitForIdle()
    }
}