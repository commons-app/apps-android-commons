package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.BuildConfig;

public class ConfigUtils {

    public static boolean isBetaFlavour() {
        return BuildConfig.FLAVOR.equals("beta");
    }
}
