package fr.free.nrw.commons.utils;

import android.content.Context;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.utils.model.ConnectionType;
import fr.free.nrw.commons.utils.model.NetworkConnectionType;

import static fr.free.nrw.commons.utils.model.ConnectionType.CELLULAR;
import static fr.free.nrw.commons.utils.model.ConnectionType.CELLULAR_3G;
import static fr.free.nrw.commons.utils.model.ConnectionType.CELLULAR_4G;
import static fr.free.nrw.commons.utils.model.ConnectionType.NO_INTERNET;
import static fr.free.nrw.commons.utils.model.ConnectionType.WIFI_NETWORK;
import static fr.free.nrw.commons.utils.model.NetworkConnectionType.FOUR_G;
import static fr.free.nrw.commons.utils.model.NetworkConnectionType.THREE_G;
import static fr.free.nrw.commons.utils.model.NetworkConnectionType.TWO_G;
import static fr.free.nrw.commons.utils.model.NetworkConnectionType.UNKNOWN;
import static fr.free.nrw.commons.utils.model.NetworkConnectionType.WIFI;

public class DeviceInfoUtil {
    private static final Map<NetworkConnectionType, ConnectionType> TYPE_MAPPING = new HashMap<>();

    static {
        TYPE_MAPPING.put(TWO_G, CELLULAR);
        TYPE_MAPPING.put(THREE_G, CELLULAR_3G);
        TYPE_MAPPING.put(FOUR_G, CELLULAR_4G);
        TYPE_MAPPING.put(WIFI, WIFI_NETWORK);
        TYPE_MAPPING.put(UNKNOWN, CELLULAR);
    }

    public static ConnectionType getConnectionType(Context context) {
        if (!NetworkUtils.isInternetConnectionEstablished(context)) {
            return NO_INTERNET;
        }
        NetworkConnectionType networkType = NetworkUtils.getNetworkType(context);
        ConnectionType deviceNetworkType = TYPE_MAPPING.get(networkType);
        return deviceNetworkType == null ? CELLULAR : deviceNetworkType;
    }

    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getDeviceModel() {
        return Build.DEVICE;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }
}
