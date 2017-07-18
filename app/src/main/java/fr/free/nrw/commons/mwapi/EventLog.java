package fr.free.nrw.commons.mwapi;

import android.os.Build;

import fr.free.nrw.commons.Utils;

public class EventLog {
    static final String DEVICE;

    static {
        if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
            DEVICE = Utils.capitalize(Build.MODEL);
        } else {
            DEVICE = Utils.capitalize(Build.MANUFACTURER) + " " + Build.MODEL;
        }
    }

    private static LogBuilder schema(String schema, long revision) {
        return new LogBuilder(schema, revision);
    }

    public static LogBuilder schema(Object[] scid) {
        if (scid.length != 2) {
            throw new IllegalArgumentException("Needs an object array with schema as first param and revision as second");
        }
        return schema((String) scid[0], (Long) scid[1]);
    }
}
