package fr.free.nrw.commons;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.nrw.commons.settings.Prefs;
import timber.log.Timber;

import static android.widget.Toast.LENGTH_SHORT;

public class Utils {

    /**
     * Strips localization symbols from a string.
     * Removes the suffix after "@" and quotes.
     *
     * @param s string possibly containing localization symbols
     * @return stripped string
     */
    public static String stripLocalizedString(String s) {
        Matcher matcher = Pattern.compile("\\\"(.*)\\\"(@\\w+)?").matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return s;
        }
    }

    /**
     * Creates an URL for thumbnail
     *
     * @param filename Thumbnail file name
     * @return URL of thumbnail
     */
    public static String makeThumbBaseUrl(@NonNull String filename) {
        String name = new PageTitle(filename).getPrefixedText();
        String sha = new String(Hex.encodeHex(DigestUtils.md5(name)));
        return String.format("%s/%s/%s/%s", BuildConfig.IMAGE_URL_BASE, sha.substring(0, 1), sha.substring(0, 2), urlEncode(name));
    }

    /**
     * URL Encode an URL in UTF-8 format
     * @param url Unformatted URL
     * @return Encoded URL
     */
    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Capitalizes the first character of a string.
     *
     * @param string String to alter
     * @return string with capitalized first character
     */
    public static String capitalize(String string) {
        if (string.length() > 0) {
            return string.substring(0, 1).toUpperCase(Locale.getDefault()) + string.substring(1);
        } else {
            return string;
        }
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
            case Prefs.Licenses.CC_BY:  // for backward compatibility to v2.1
                return R.string.license_name_cc_by_3_0;
            case Prefs.Licenses.CC_BY_SA:  // for backward compatibility to v2.1
                return R.string.license_name_cc_by_sa_3_0;
        }
        throw new RuntimeException("Unrecognized license value: " + license);
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
     * Tells whether dark theme is active or not
     * @param context Activity context
     * @return The state of dark theme
     */
    public static boolean isDarkTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("theme", false);
    }

    public static void rateApp(Context context) {
        final String appPackageName = BuildConfig.class.getPackage().getName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        }
        catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

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
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        customTabsIntent.launchUrl(context, url);
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

}
