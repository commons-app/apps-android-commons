package fr.free.nrw.commons.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import javax.annotation.Nullable;

public class NetworkUtils {

    /**
     * https://developer.android.com/training/monitoring-device-state/connectivity-monitoring#java
     * Check if internet connection is established.
     * @param context context passed to this method could be null.
     * @return Returns current internet connection status. Returns false if null context was passed.
     */
    @SuppressLint("MissingPermission")
    public static boolean isInternetConnectionEstablished(@Nullable Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager cm =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
