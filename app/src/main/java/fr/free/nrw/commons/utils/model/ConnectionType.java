package fr.free.nrw.commons.utils.model;

public enum ConnectionType {
    WIFI_NETWORK("wifi"), CELLULAR_4G("cellular-4g"), CELLULAR_3G("cellular-3g"), CELLULAR("cellular"), NO_INTERNET("no-internet");

    private final String text;

    ConnectionType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
