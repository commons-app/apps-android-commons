package fr.free.nrw.commons.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import javax.annotation.Nullable;

import fr.free.nrw.commons.utils.model.NetworkConnectionType;

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

        NetworkInfo activeNetwork = getNetworkInfo(context);
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static NetworkConnectionType getNetworkType(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        NetworkInfo networkInfo = getNetworkInfo(context);
        NetworkConnectionType networkConnectionType;
        if (networkInfo == null) {
            networkConnectionType = NetworkConnectionType.UNKNOWN;
        } else {
            int network = networkInfo.getType();
            if (network == ConnectivityManager.TYPE_WIFI) {
                networkConnectionType = NetworkConnectionType.WIFI;
            } else {
                int mobileNetwork = telephonyManager.getNetworkType();
                switch (mobileNetwork) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                        networkConnectionType = NetworkConnectionType.TWO_G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        networkConnectionType = NetworkConnectionType.THREE_G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        networkConnectionType = NetworkConnectionType.FOUR_G;
                        break;
                    default:
                        networkConnectionType = NetworkConnectionType.UNKNOWN;
                        break;
                }
            }
        }
        return networkConnectionType;
    }

    @Nullable
    private static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return null;
        }

        return connectivityManager.getActiveNetworkInfo();
    }
}
