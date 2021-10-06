@file:JvmName("Utils")

package fr.free.nrw.commons

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.settings.Licenses
import fr.free.nrw.commons.utils.ViewUtil
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

fun getPageTitle(title: String) = PageTitle(title, WikiSite(BuildConfig.COMMONS_URL))

/**
 * Generates licence name with given ID
 *
 * @param licenseId License ID
 * @return Name of license
 */
fun licenseNameFor(licenseId: String?) = Licenses.findById(licenseId).name

/**
 * Generates license url with given ID
 *
 * @param licenseId License ID
 * @return Url of license
 */
fun licenseUrlFor(licenseId: String?) = Licenses.findById(licenseId).url

/**
 * Generates license template for the given ID
 *
 * @param licenseId License ID
 * @return Template for the license
 */
fun licenseTemplateFor(licenseId: String?) = Licenses.findById(licenseId).template

/**
 * Adds extension to filename. Converts to .jpg if system provides .jpeg, adds .jpg if no
 * extension detected
 *
 * @param title     File name
 * @param extension Correct extension
 * @return File with correct extension
 */
fun fixExtension(title: String?, extension: String?): String {
    // People are used to ".jpg" more than ".jpeg" which the system gives us.
    var updatedExtension =
        if (extension?.lowercase(Locale.ENGLISH) != "jpeg") extension else "jpg"

    var updatedTitle = Pattern.compile("\\.jpeg$", Pattern.CASE_INSENSITIVE)
        .matcher(title)
        .replaceFirst(".jpg")
    if (updatedExtension != null &&
        !updatedTitle.lowercase(Locale.getDefault())
            .endsWith("." + updatedExtension.lowercase(Locale.ENGLISH))) {
        updatedTitle += ".$updatedExtension"
    }

    // If extension is still null, make it jpg. (Hotfix for https://github.com/commons-app/apps-android-commons/issues/228)
    // If updatedTitle has an extension in it, if won't be true
    if (updatedExtension == null && updatedTitle.lastIndexOf(".") <= 0) {
        updatedExtension = "jpg"
        updatedTitle += ".$updatedExtension"
    }
    return updatedTitle
}

/**
 * Launches intent to rate app
 *
 * @param context
 */
fun rateApp(context: Context) = with(Uri.parse(Urls.PLAY_STORE_PREFIX + context.packageName)) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, this))
    } catch (anfe: ActivityNotFoundException) {
        handleWebUrl(context, this)
    }
}

/**
 * Opens Custom Tab Activity with in-app browser for the specified URL. Launches intent for web
 * URL
 *
 * @param context
 * @param url
 */
fun handleWebUrl(context: Context, url: Uri) {
    Timber.d("Launching web url %s", url.toString())
    val browserIntent = Intent(Intent.ACTION_VIEW, url)
    if (browserIntent.resolveActivity(context.packageManager) == null) {
        Toast.makeText(context, R.string.no_web_browser, Toast.LENGTH_SHORT).show()
        return
    }

    val color = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor))
        .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.primaryDarkColor))
        .build()

    CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(color)
        .setExitAnimations(
            context, android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        ).build().apply {
            // Clear previous browser tasks, so that back/exit buttons work as intended.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            launchUrl(context, url)
        }
}

/**
 * Util function to handle geo coordinates It no longer depends on google maps and any app
 * capable of handling the map intent can handle it
 *
 * @param context
 * @param latLng
 */
fun handleGeoCoordinates(context: Context, latLng: LatLng) {
    val mapIntent = Intent(Intent.ACTION_VIEW, latLng.gmmIntentUri)
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        ViewUtil.showShortToast(context, R.string.map_application_missing)
    }
}

/**
 * To take screenshot of the screen and return it in Bitmap format
 *
 * @param view
 * @return
 */
fun getScreenShot(view: View): Bitmap? {
    val screenView = view.rootView
    return screenView.drawingCache?.let {
        screenView.isDrawingCacheEnabled = true
        return Bitmap.createBitmap(it).also {
            screenView.isDrawingCacheEnabled = false
        }
    }
}

/**
 * Copies the content to the clipboard
 */
fun copy(label: String?, text: String?, context: Context) =
    with(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager) {
        primaryClip = ClipData.newPlainText(label, text)
    }

/**
 * This method sets underlined string text to a TextView
 *
 * @param textView           TextView associated with string resource
 * @param stringResourceName string resource name
 * @param context
 */
fun setUnderlinedText(textView: TextView, stringResourceName: Int, context: Context) {
    textView.text = SpannableString(context.getString(stringResourceName)).apply {
        setSpan(UnderlineSpan(), 0, length, 0)
    }
}

/**
 * For now we are enabling the monuments only when the date lies between 1 Sept & 31 OCt
 * @param date
 * @return
 */
fun isMonumentsEnabled(date: Date): Boolean {
    return date.month == 8
}

/**
 * Util function to get the start date of wlm monument
 * For this release we are hardcoding it to be 1st September
 * @return
 */
const val wLMStartDate = "1 Sep"

/***
 * Util function to get the end date of wlm monument
 * For this release we are hardcoding it to be 31st October
 * @return
 */
const val wLMEndDate = "30 Sep"
