package fr.free.nrw.commons.utils

import android.content.Context
import android.content.pm.PackageManager
import fr.free.nrw.commons.BuildConfig
import java.util.*

object ConfigUtils {
    @JvmStatic
    val isBetaFlavour: Boolean
        get() = BuildConfig.FLAVOR == "beta"

    @JvmStatic
    private fun getVersionName(context: Context): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }
    }

    @JvmStatic
    fun getVersionNameWithSha(context: Context): String {
        return String.format(
            Locale.getDefault(),
            "%s~%s",
            getVersionName(context),
            BuildConfig.COMMIT_SHA
        )
    }
}