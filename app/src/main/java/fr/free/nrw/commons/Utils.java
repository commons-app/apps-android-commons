package fr.free.nrw.commons;

import android.content.Context;
import android.preference.PreferenceManager;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.settings.Prefs;

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

    public static String makeThumbBaseUrl(String filename) {
        String name = new PageTitle(filename).getPrefixedText();
        String sha = new String(Hex.encodeHex(DigestUtils.md5(name)));
        return String.format("%s/%s/%s/%s", BuildConfig.IMAGE_URL_BASE, sha.substring(0, 1), sha.substring(0, 2), urlEncode(name));
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase(Locale.getDefault()) + string.substring(1);
    }

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
        return title;
    }

    public static boolean isDarkTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("theme", false);
    }
}
