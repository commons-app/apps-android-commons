package fr.free.nrw.commons.utils.model

enum class ConnectionType(private val text: String) {
    WIFI_NETWORK("wifi"), CELLULAR_4G("cellular-4g"), CELLULAR_3G("cellular-3g"), CELLULAR("cellular"), NO_INTERNET("no-internet");

    override fun toString(): String {
        return text
    }
}