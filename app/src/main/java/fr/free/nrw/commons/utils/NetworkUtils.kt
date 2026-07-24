package fr.free.nrw.commons.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo

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
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkConnectionType.UNKNOWN

        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkConnectionType.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkConnectionType.UNKNOWN

        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            NetworkConnectionType.WIFI
        } else {
            NetworkConnectionType.UNKNOWN
        }
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
