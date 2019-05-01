package fr.free.nrw.commons

import android.Manifest
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.utils.ConfigUtils
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@LargeTest
@RunWith(AndroidJUnit4::class)
class UploadTest {
    @get:Rule
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION)!!

    @get:Rule
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    private val randomBitmap: Bitmap
        get() {
            val random = Random()
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(random.nextInt(255))
            return bitmap
        }

    @Before
    fun setup() {
        try {
            Intents.init()
        } catch (ex: IllegalStateException) {

        }
        UITestHelper.skipWelcome()
        UITestHelper.loginUser()
        saveToInternalStorage()
    }

    @After
    fun teardown() {
        Intents.release()
    }

    private fun saveToInternalStorage() {
        val bitmapImage = randomBitmap

        // path to /data/data/yourapp/app_data/imageDir
        val mypath = File(Environment.getExternalStorageDirectory(), "image.jpg")

        Timber.d("Filepath: %s", mypath.path)

        Timber.d("Absolute Filepath: %s", mypath.absolutePath)

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(mypath)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    @Test
    fun uploadTest() {
        if (!ConfigUtils.isBetaFlavour()) {
            throw Error("This test should only be run in Beta!")
        }

        // Uri to return by our mock gallery selector
        // Requires file 'image.jpg' to be placed at root of file structure
        val imageUri = Uri.parse("file://mnt/sdcard/image.jpg")

        // Build a result to return from the Camera app
        val intent = Intent()
        intent.data = imageUri
        val result = ActivityResult(Activity.RESULT_OK, intent)

        // Stub out the File picker. When an intent is sent to the File picker, this tells
        // Espresso to respond with the ActivityResult we just created
        intending(allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*"))).respondWith(result)

        // Open FAB
        onView(allOf<View>(withId(R.id.fab_plus), isDisplayed()))
                .perform(click())

        // Click gallery
        onView(allOf<View>(withId(R.id.fab_gallery), isDisplayed()))
                .perform(click())

        // Validate that an intent to get an image is sent
        intended(allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*")))

        // Create filename with the current time (to prevent overwrites)
        val dateFormat = SimpleDateFormat("yyMMdd-hhmmss")
        val commonsFileName = "MobileTest " + dateFormat.format(Date())

        // Try to dismiss the error, if there is one (probably about duplicate files on Commons)
        try {
            onView(withText("Yes"))
                    .check(matches(isDisplayed()))
                    .perform(click())
        } catch (ignored: NoMatchingViewException) {}

        onView(allOf<View>(withId(R.id.description_item_edit_text), withParent(withParent(withId(R.id.image_title_container)))))
                .perform(replaceText(commonsFileName))

        onView(withId(R.id.bottom_card_next))
                .perform(click())

        UITestHelper.sleep(1000)

        onView(withId(R.id.category_search))
                .perform(replaceText("Uploaded with Mobile/Android Tests"))

        UITestHelper.sleep(3000)

        onView(withParent(withId(R.id.categories)))
                .perform(click())

        onView(withId(R.id.category_next))
                .perform(click())

        UITestHelper.sleep(500)

        onView(withId(R.id.submit))
                .perform(click())

        UITestHelper.sleep(10000)

        val fileUrl = "https://commons.wikimedia.beta.wmflabs.org/wiki/File:" +
                commonsFileName.replace(' ', '_') + ".jpg"
        Timber.i("File should be uploaded to $fileUrl")
    }
}