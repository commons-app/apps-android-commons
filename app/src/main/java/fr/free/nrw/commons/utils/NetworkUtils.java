package fr.free.nrw.commons.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtils {

    /**
     * Check if internet connection is established
     * @param context
     * @return true if network is connected or connecting
     */
    public static boolean isInternetConnectionEstablished(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
