package fr.free.nrw.commons.utils

import android.content.Context
import android.content.pm.PackageManager
import fr.free.nrw.commons.BuildConfig

object ConfigUtils {
    @JvmStatic
    val isBetaFlavour: Boolean = BuildConfig.FLAVOR == "beta"

    @JvmStatic
    private fun Context.getVersionName(): String {
        return try {
            this.packageManager.getPackageInfo(this.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }
    }

    @JvmStatic
    fun Context.getVersionNameWithSha(): String {
        return "${this.getVersionName()}~${BuildConfig.COMMIT_SHA}"
    }
}