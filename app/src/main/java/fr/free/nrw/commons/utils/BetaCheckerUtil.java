package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.BuildConfig;

public class BetaCheckerUtil {

    public static boolean isBetaFlavour() {
        return BuildConfig.FLAVOR.equals("beta");
    }
}
