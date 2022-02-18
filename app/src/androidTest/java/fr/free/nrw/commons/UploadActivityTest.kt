package fr.free.nrw.commons

import android.net.Uri
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(UploadActivity::class.java)

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }

    @Test
    @Ignore("Fix Failing Test")
    fun TestForCaptionsAndDepictions() {
        val imageUri = Uri.parse("file://mnt/sdcard/image.jpg")

        Espresso.onView(ViewMatchers.withId(R.id.caption_item_edit_text))
                .perform(ViewActions.typeText("caption in english"))
        Espresso.onView(ViewMatchers.withId(R.id.description_item_edit_text))
                .perform(ViewActions.typeText("description in english"))
        Espresso.onData(AllOf.allOf(Matchers.anything("spinner text"))).atPosition(1).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.caption_item_edit_text))
                .perform(ViewActions.typeText("caption in some other language"))
        Espresso.onView(ViewMatchers.withId(R.id.description_item_edit_text))
                .perform(ViewActions.typeText("description in some other language"))
        Espresso.onView(ViewMatchers.withId(R.id.btn_next))
                .perform(ViewActions.click())
        Intents.intended(IntentMatchers.hasComponent(DepictsFragment::class.java.name))
    }
}
