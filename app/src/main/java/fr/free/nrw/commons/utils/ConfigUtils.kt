package fr.free.nrw.commons.utils

import android.content.Context
import android.content.pm.PackageManager
import fr.free.nrw.commons.BuildConfig

object ConfigUtils {
    @JvmStatic
    val isBetaFlavour: Boolean = BuildConfig.FLAVOR == "beta"

    @JvmStatic
    private fun Context.getVersionName(): String =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }

    @JvmStatic
    fun Context.getVersionNameWithSha(): String = "${getVersionName()}~${BuildConfig.COMMIT_SHA}"
}
