package fr.free.nrw.commons.utils

import android.content.Context
import android.os.Build
import fr.free.nrw.commons.utils.model.ConnectionType
import fr.free.nrw.commons.utils.model.NetworkConnectionType

/**
 * Util class to get any information about the user's device
 * Ensure that any sensitive information like IMEI is not fetched/shared without user's consent
 */
object DeviceInfoUtil {
    private val TYPE_MAPPING = mapOf(
        NetworkConnectionType.TWO_G to ConnectionType.CELLULAR,
        NetworkConnectionType.THREE_G to ConnectionType.CELLULAR_3G,
        NetworkConnectionType.FOUR_G to ConnectionType.CELLULAR_4G,
        NetworkConnectionType.WIFI to ConnectionType.WIFI_NETWORK,
        NetworkConnectionType.UNKNOWN to ConnectionType.CELLULAR
    )

    /**
     * Get network connection type
     * @param context
     * @return wifi/cellular-4g/cellular-3g/cellular-2g/no-internet
     */
    @JvmStatic
    fun getConnectionType(context: Context): ConnectionType {
        return if (!NetworkUtils.isInternetConnectionEstablished(context)) {
            ConnectionType.NO_INTERNET
        } else {
            val networkType = NetworkUtils.getNetworkType(context)
            TYPE_MAPPING[networkType] ?: ConnectionType.CELLULAR
        }
    }

    /**
     * Get Device manufacturer
     * @return
     */
    @JvmStatic
    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    /**
     * Get Device model name
     * @return
     */
    @JvmStatic
    fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * Get Android version. Eg. 4.4.2
     * @return
     */
    @JvmStatic
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }

    /**
     * Get API Level. Eg. 26
     * @return
     */
    @JvmStatic
    fun getAPILevel(): String {
        return Build.VERSION.SDK
    }

    /**
     * Get Device.
     * @return
     */
    @JvmStatic
    fun getDevice(): String {
        return Build.DEVICE
    }
}
