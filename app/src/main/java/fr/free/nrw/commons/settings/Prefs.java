package fr.free.nrw.commons.settings;

public class Prefs {
    public static String GLOBAL_PREFS = "fr.free.nrw.commons.preferences";

    public static String TRACKING_ENABLED = "eventLogging";
    public static final String DEFAULT_LICENSE = "defaultLicense";
    public static final String UPLOADS_SHOWING = "uploadsshowing";

    public static class Licenses {
        public static final String CC_BY_SA_3 = "CC BY-SA 3.0";
        public static final String CC_BY_3 = "CC BY 3.0";
        public static final String CC_BY_SA_4 = "CC BY-SA 4.0";
        public static final String CC_BY_4 = "CC BY 4.0";
        public static final String CC0 = "CC0";

        // kept for backward compatibility to v2.1
        @Deprecated public static final String CC_BY = "CC BY";
        @Deprecated public static final String CC_BY_SA = "CC BY-SA";
    }
}
