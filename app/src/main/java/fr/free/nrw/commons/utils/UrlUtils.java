package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import fr.free.nrw.commons.R;
import timber.log.Timber;

public final class UrlUtils {

    /**
     * Opens Custom Tab Activity with in-app browser for the specified URL.
     * Launches intent for web URL
     * @param context
     * @param url
     */
    public static void handleWebUrl(Context context, Uri url) {
        Timber.d("Launching web url %s", url.toString());

        final CustomTabColorSchemeParams color = new CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor))
            .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.primaryDarkColor))
            .build();

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setDefaultColorSchemeParams(color);
        builder.setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        CustomTabsIntent customTabsIntent = builder.build();
        // Clear previous browser tasks, so that back/exit buttons work as intended.
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        customTabsIntent.launchUrl(context, url);
    }
}
