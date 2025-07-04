package fr.free.nrw.commons.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.R
import timber.log.Timber

/**
 * Opens Custom Tab Activity with in-app browser for the specified URL.
 * Launches intent for web URL
 */
fun handleWebUrl(context: Context, url: Uri) {
    Timber.d("Launching web url %s", url.toString())

    val color = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, R.color.primaryColor))
        .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.primaryDarkColor))
        .build()

    val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(color)
            .setExitAnimations(
                context, android.R.anim.slide_in_left, android.R.anim.slide_out_right
            ).build()

    // Clear previous browser tasks, so that back/exit buttons work as intended.
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    customTabsIntent.launchUrl(context, url)
}
