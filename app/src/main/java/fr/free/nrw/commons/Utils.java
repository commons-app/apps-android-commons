package fr.free.nrw.commons;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.util.Locale;
import java.util.regex.Pattern;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static android.widget.Toast.LENGTH_SHORT;

public class Utils {

    public static PageTitle getPageTitle(@NonNull String title) {
        return new PageTitle(title, new WikiSite(BuildConfig.COMMONS_URL));
    }

    /**
     * Generates licence name with given ID
     * @param license License ID
     * @return Name of license
     */
    public static int licenseNameFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return R.string.license_name_cc_by;
            case Prefs.Licenses.CC_BY_4:
                return R.string.license_name_cc_by_four;
            case Prefs.Licenses.CC_BY_SA_3:
                return R.string.license_name_cc_by_sa;
            case Prefs.Licenses.CC_BY_SA_4:
                return R.string.license_name_cc_by_sa_four;
            case Prefs.Licenses.CC0:
                return R.string.license_name_cc0;
        }
        throw new IllegalStateException("Unrecognized license value: " + license);
    }

    /**
     * Generates license url with given ID
     * @param license License ID
     * @return Url of license
     */


    @NonNull
    public static String licenseUrlFor(String license) {
        switch (license) {
            case Prefs.Licenses.CC_BY_3:
                return "https://creativecommons.org/licenses/by/3.0/";
            case Prefs.Licenses.CC_BY_4:
                return "https://creativecommons.org/licenses/by/4.0/";
            case Prefs.Licenses.CC_BY_SA_3:
                return "https://creativecommons.org/licenses/by-sa/3.0/";
            case Prefs.Licenses.CC_BY_SA_4:
                return "https://creativecommons.org/licenses/by-sa/4.0/";
            case Prefs.Licenses.CC0:
                return "https://creativecommons.org/publicdomain/zero/1.0/";
            default:
                throw new IllegalStateException("Unrecognized license value: " + license);
        }
    }

    /**
     * Adds extension to filename. Converts to .jpg if system provides .jpeg, adds .jpg if no extension detected
     * @param title File name
     * @param extension Correct extension
     * @return File with correct extension
     */
    public static String fixExtension(String title, String extension) {
        Pattern jpegPattern = Pattern.compile("\\.jpeg$", Pattern.CASE_INSENSITIVE);

        // People are used to ".jpg" more than ".jpeg" which the system gives us.
        if (extension != null && extension.toLowerCase(Locale.ENGLISH).equals("jpeg")) {
            extension = "jpg";
        }
        title = jpegPattern.matcher(title).replaceFirst(".jpg");
        if (extension != null && !title.toLowerCase(Locale.getDefault())
                .endsWith("." + extension.toLowerCase(Locale.ENGLISH))) {
            title += "." + extension;
        }

        // If extension is still null, make it jpg. (Hotfix for https://github.com/commons-app/apps-android-commons/issues/228)
        // If title has an extension in it, if won't be true
        if (extension == null && title.lastIndexOf(".")<=0) {
           extension = "jpg";
           title += "." + extension;
        }

        return title;
    }

    /**
     * Launches intent to rate app
     * @param context
     */
    public static void rateApp(Context context) {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.PLAY_STORE_PREFIX + appPackageName)));
        }
        catch (android.content.ActivityNotFoundException anfe) {
            handleWebUrl(context, Uri.parse(Urls.PLAY_STORE_URL_PREFIX + appPackageName));
        }
    }

    /**
     * Opens Custom Tab Activity with in-app browser for the specified URL.
     * Launches intent for web URL
     * @param context
     * @param url
     */
    public static void handleWebUrl(Context context, Uri url) {
        Timber.d("Launching web url %s", url.toString());
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
        if (browserIntent.resolveActivity(context.getPackageManager()) == null) {
            Toast toast = Toast.makeText(context, context.getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
            return;
        }

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor));
        builder.setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.primaryDarkColor));
        builder.setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        CustomTabsIntent customTabsIntent = builder.build();
        // Clear previous browser tasks, so that back/exit buttons work as intended.
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        customTabsIntent.launchUrl(context, url);
    }

    /**
     * Util function to handle geo coordinates
     * It no longer depends on google maps and any app capable of handling the map intent can handle it
     * @param context
     * @param latLng
     */
    public static void handleGeoCoordinates(Context context, LatLng latLng) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, latLng.getGmmIntentUri());
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            ViewUtil.showShortToast(context, context.getString(R.string.map_application_missing));
        }
    }

    /**
     * To take screenshot of the screen and return it in Bitmap format
     *
     * @param view
     * @return
     */
    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /*
    *Copies the content to the clipboard
    *
    */
    public static void copy(String label,String text, Context context){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * This method sets underlined string text to a TextView
     *
     * @param textView TextView associated with string resource
     * @param stringResourceName string resource name
     * @param context
     */
    public static void setUnderlinedText(TextView textView, int stringResourceName, Context context) {
        SpannableString content = new SpannableString(context.getString(stringResourceName));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        textView.setText(content);
    }

}
