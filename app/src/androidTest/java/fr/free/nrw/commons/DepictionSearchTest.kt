package fr.free.nrw.commons

import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import android.net.Uri
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import fr.free.nrw.commons.upload.UploadActivity
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class DepictionSearchTest {
    @get:Rule
    var activityRule = ActivityTestRule(UploadActivity::class.java)

    @Test
    fun TestForCaptionsAndDepictions() {
        val imageUri = Uri.parse("file://mnt/sdcard/image.jpg")

        // Build a result to return from the Camera app


        // Stub out the File picker. When an intent is sent to the File picker, this tells
        // Espresso to respond with the ActivityResult we just created

        Espresso.onView(ViewMatchers.withId(R.id.caption_item_edit_text))
                .perform(ViewActions.typeText("caption in english"))
        Espresso.onView(ViewMatchers.withId(R.id.description_item_edit_text))
                .perform(ViewActions.typeText("description in english"))
        Espresso.onView(ViewMatchers.withId(R.id.spinner_description_languages))
                .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.spinner_description_languages)).perform(ViewActions.click());
        Espresso.onData(AllOf.allOf(Matchers.anything("spinner text"))).atPosition(1).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.caption_item_edit_text))
                .perform(ViewActions.typeText("caption in some other language"))
        Espresso.onView(ViewMatchers.withId(R.id.description_item_edit_text))
                .perform(ViewActions.typeText("description in some other language"))
        Espresso.onView(ViewMatchers.withId(R.id.btn_next))
                .perform(ViewActions.click())
    }
}