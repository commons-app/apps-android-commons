package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.Intent;

public class ActivityUtils {

    public static <T> void startActivityWithFlags(Context context, Class<T> cls, int... flags) {
        Intent intent = new Intent(context, cls);
        for (int flag : flags) {
            intent.addFlags(flag);
        }
        context.startActivity(intent);
    }
}
