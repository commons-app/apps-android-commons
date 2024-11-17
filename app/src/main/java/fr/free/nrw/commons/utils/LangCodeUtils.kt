package fr.free.nrw.commons.utils;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

/**
 * Utilities class for miscellaneous strings
 */
public class LangCodeUtils {
    /**
     * Replaces the deprecated ISO-639 language codes used by Android with the updated ISO-639-1.
     * @param code Language code you want to update.
     * @return Updated language code. If not in the "deprecated list" returns the same code.
     */
    public static String fixLanguageCode(String code) {
        if (code.equalsIgnoreCase("iw")) {
            return "he";
        } else if (code.equalsIgnoreCase("in")) {
            return "id";
        } else if (code.equalsIgnoreCase("ji")) {
            return "yi";
        } else {
            return code;
        }
    }
    
    /**
     * Returns configuration for locale of
     * our choice regardless of user's device settings
     */
    public static Resources getLocalizedResources(Context context, Locale desiredLocale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }
}
