package fr.free.nrw.commons.utils

import android.content.Context
import android.content.pm.PackageManager
import fr.free.nrw.commons.BuildConfig

// TODO - this can be constructed in a Dagger provider method, in a module and injected.  No need
//        to compute these values every time, and it means we can avoid having a Context in various
//        other places in the app.
object ConfigUtils {
    @JvmStatic
    val isBetaFlavour: Boolean = BuildConfig.FLAVOR == "beta"

    @JvmStatic
    private fun Context.getVersionName(): String? =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }

    @JvmStatic
    fun Context.getVersionNameWithSha(): String = "${getVersionName()}~${BuildConfig.COMMIT_SHA}"
}
