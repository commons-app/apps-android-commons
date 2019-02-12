package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Locale;

import fr.free.nrw.commons.BuildConfig;

public class ConfigUtils {

    public static boolean isBetaFlavour() {
        return BuildConfig.FLAVOR.equals("beta");
    }

    private static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return BuildConfig.VERSION_NAME;
        }
    }

    public static String getVersionNameWithSha(Context context) {
        return String.format(Locale.getDefault(), "%s~%s", getVersionName(context), BuildConfig.COMMIT_SHA);
    }
}
