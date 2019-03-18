package fr.free.nrw.commons;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
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
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public ActivityTestRule activityRule = new IntentsTestRule<>(WelcomeActivity.class);

    @Before
    public void setup() {
        saveToInternalStorage();
    }

    private void saveToInternalStorage() {
        Bitmap bitmapImage = getRandomBitmap();
        Context applicationContext = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ContextWrapper cw = new ContextWrapper(applicationContext);
        // path to /data/data/yourapp/app_data/imageDir
        File mypath = new File(Environment.getExternalStorageDirectory(), "image.jpg");

        Timber.d("Filepath: %s", mypath.getPath());

        Timber.d("Absolute Filepath: %s", mypath.getAbsolutePath());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getRandomBitmap() {
        Random random = new Random();
        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(random.nextInt(255));
        return bitmap;
    }

    private void getToMainActivity() {
        //Skip tutorial
        onView(withId(R.id.finishTutorialButton))
                .perform(click());
        //Perform Login

    }

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