package fr.free.nrw.commons.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager

import fr.free.nrw.commons.utils.model.NetworkConnectionType

object NetworkUtils {

    /**
     * https://developer.android.com/training/monitoring-device-state/connectivity-monitoring#java
     * Check if internet connection is established.
     *
     * @param context context passed to this method could be null.
     * @return Returns current internet connection status. Returns false if null context was passed.
     */
    @SuppressLint("MissingPermission")
    @JvmStatic
    fun isInternetConnectionEstablished(context: Context?): Boolean {
        if (context == null) {
            return false
        }

        val activeNetwork = getNetworkInfo(context)
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    /**
     * Detect network connection type
     */
    @JvmStatic
    fun getNetworkType(context: Context): NetworkConnectionType {
        val telephonyManager = context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return NetworkConnectionType.UNKNOWN

        val networkInfo = getNetworkInfo(context)
            ?: return NetworkConnectionType.UNKNOWN

        val network = networkInfo.type
        if (network == ConnectivityManager.TYPE_WIFI) {
            return NetworkConnectionType.WIFI
        }

        // TODO for Android 12+ request permission from user is mandatory
        /*
        val mobileNetwork = telephonyManager.networkType
        return when (mobileNetwork) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT -> NetworkConnectionType.TWO_G

            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> NetworkConnectionType.THREE_G

            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_HSPAP -> NetworkConnectionType.FOUR_G

            else -> NetworkConnectionType.UNKNOWN
        }
         */
        return NetworkConnectionType.UNKNOWN
    }

    /**
     * Extracted private method to get nullable network info
     */
    @JvmStatic
    private fun getNetworkInfo(context: Context): NetworkInfo? {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null

        return connectivityManager.activeNetworkInfo
    }
}
