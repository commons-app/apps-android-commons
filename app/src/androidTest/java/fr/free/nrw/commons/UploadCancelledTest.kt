package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
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
import org.junit.After
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
        try {
            Intents.init()
        } catch (ex: IllegalStateException) {

        }
        device.unfreezeRotation()
        device.setOrientationNatural()
        device.freezeRotation()
        UITestHelper.loginUser()
        UITestHelper.skipWelcome()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun teardown() {
        try {
            Intents.release()
        } catch (ex: IllegalStateException) {

        }
    }

    @Test
    fun uploadCancelledAfterLocationPickedTest() {

        val bottomNavigationItemView = onView(
            allOf(
                childAtPosition(
                    childAtPosition(
                        withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView.perform(click())

        UITestHelper.sleep(12000)

        val actionMenuItemView = onView(
            allOf(
                withId(R.id.list_sheet),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView.perform(click())

        val recyclerView = onView(
            allOf(
                withId(R.id.rv_nearby_list),
            )
        )
        recyclerView.perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                click()
            )
        )

        val linearLayout3 = onView(
            allOf(
                withId(R.id.cameraButton),
                childAtPosition(
                    allOf(
                        withId(R.id.nearby_button_layout),
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        linearLayout3.perform(click())

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
