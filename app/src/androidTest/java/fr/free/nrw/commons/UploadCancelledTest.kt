package fr.free.nrw.commons


import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.auth.LoginActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadCancelledTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        UITestHelper.loginUser()
        UITestHelper.skipWelcome()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }
    @Test
    fun uploadCancelledAfterLocationPickedTest() {

        val floatingActionButton = onView(allOf(withId(R.id.fab_plus)))
        floatingActionButton.perform(click())
        UITestHelper.sleep(5000)
        val floatingActionButton2 = onView(allOf(withId(R.id.fab_camera)))
        floatingActionButton2.perform(click())

        val pasteSensitiveTextInputEditText = onView(
            allOf(
                withId(R.id.caption_item_edit_text),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.caption_item_edit_text_input_layout),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        pasteSensitiveTextInputEditText.perform(replaceText("test"), closeSoftKeyboard())

        val pasteSensitiveTextInputEditText2 = onView(
            allOf(
                withId(R.id.description_item_edit_text),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.description_item_edit_text_input_layout),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        pasteSensitiveTextInputEditText2.perform(replaceText("test"), closeSoftKeyboard())

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.btn_next),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.ll_container_media_detail),
                        2
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatButton2.perform(click())

        val appCompatButton3 = onView(
            allOf(
                withId(android.R.id.button1),
            )
        )
        appCompatButton3.perform(scrollTo(), click())

        Intents.intended(IntentMatchers.hasComponent(LocationPickerActivity::class.java.name))

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.location_chosen_button),
                isDisplayed()
            )
        )
        UITestHelper.sleep(2000)
        floatingActionButton3.perform(click())
    }
}
