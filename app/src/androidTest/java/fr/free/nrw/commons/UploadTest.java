package fr.free.nrw.commons;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.net.Uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.ConfigUtils;
import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UploadTest {
    @Rule
    public ActivityTestRule activityRule = new IntentsTestRule<>(MainActivity.class);

    @Test
    public void uploadTest() {
        if (!ConfigUtils.isBetaFlavour()) {
            throw new Error("This test should only be run in Beta!");
        }

        // Uri to return by our mock gallery selector
        // Requires file 'image.jpg' to be placed at root of file structure
        Uri imageUri = Uri.parse("file://mnt/sdcard/image.jpg");

        // Build a result to return from the Camera app
        Intent intent = new Intent();
        intent.setData(imageUri);
        ActivityResult result = new ActivityResult(Activity.RESULT_OK, intent);

        // Stub out the File picker. When an intent is sent to the File picker, this tells
        // Espresso to respond with the ActivityResult we just created
        intending(allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*"))).respondWith(result);

        // Open FAB
        onView(allOf(withId(R.id.fab_plus), isDisplayed()))
                .perform(click());

        // Click gallery
        onView(allOf(withId(R.id.fab_gallery), isDisplayed()))
                .perform(click());

        // Validate that an intent to get an image is sent
        intended(allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*")));

        // Create filename with the current time (to prevent overwrites)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd-hhmmss");
        String commonsFileName = "MobileTest " + dateFormat.format(new Date());

        // Try to dismiss the error, if there is one (probably about duplicate files on Commons)
        try {
            onView(withText("Yes"))
                    .check(matches(isDisplayed()))
                    .perform(click());
        } catch (NoMatchingViewException ignored) {}

        onView(allOf(withId(R.id.description_item_edit_text), withParent(withParent(withId(R.id.image_title_container)))))
                .perform(replaceText(commonsFileName));

        onView(withId(R.id.bottom_card_next))
                .perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.category_search))
                .perform(replaceText("Uploaded with Mobile/Android Tests"));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withParent(withId(R.id.categories)))
                .perform(click());

        onView(withId(R.id.category_next))
                .perform(click());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.submit))
                .perform(click());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String fileUrl = "https://commons.wikimedia.beta.wmflabs.org/wiki/File:" +
                commonsFileName.replace(' ', '_') + ".jpg";
        Timber.i("File should be uploaded to " + fileUrl);
    }
}