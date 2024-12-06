package fr.free.nrw.commons.feedback.model;

/**
 * Pojo class for storing information that are required while uploading a feedback
 */
public class Feedback {
    /**
     * Version of app
     */
    private String version;
    /**
     * API level of user's phone
     */
    private String apiLevel;
    /**
     * Title/Description entered by user
     */
    private String title;
    /**
     * Android version of user's device
     */
    private String androidVersion;
    /**
     * Device Model of user's device
     */
    private String deviceModel;
    /**
     * Device manufacturer name
     */
    private String deviceManufacturer;
    /**
     * Device name stored on user's device
     */
    private String device;
    /**
     * network type user is having (Ex: Wifi)
     */
    private String networkType;

    public Feedback(final String version, final String apiLevel, final String title, final String androidVersion,
        final String deviceModel, final String deviceManufacturer, final String device, final String networkType
        ) {
        this.version = version;
        this.apiLevel = apiLevel;
        this.title = title;
        this.androidVersion = androidVersion;
        this.deviceModel = deviceModel;
        this.deviceManufacturer = deviceManufacturer;
        this.device = device;
        this.networkType = networkType;
    }

    /**
     * Get the version from which this piece of feedback is being sent.
     * Ex: 3.0.1
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version of app to given version
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * gets api level of device
     * Ex: 28
     */
    public String getApiLevel() {
        return apiLevel;
    }

    /**
     * sets api level value to given value
     */
    public void setApiLevel(final String apiLevel) {
        this.apiLevel = apiLevel;
    }

    /**
     * gets feedback text entered by user
     */
    public String getTitle() {
        return title;
    }

    /**
     * sets feedback text
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * gets android version of device
     * Ex: 9
     */
    public String getAndroidVersion() {
        return androidVersion;
    }

    /**
     * sets value of android version
     */
    public void setAndroidVersion(final String androidVersion) {
        this.androidVersion = androidVersion;
    }

    /**
     * get device model of current device
     * Ex: Redmi 6 Pro
     */
    public String getDeviceModel() {
        return deviceModel;
    }

    /**
     * sets value of device model to a given value
     */
    public void setDeviceModel(final String deviceModel) {
        this.deviceModel = deviceModel;
    }

    /**
     * get device manufacturer of user's device
     * Ex: Redmi
     */
    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    /**
     * set device manufacturer value to a given value
     */
    public void setDeviceManufacturer(final String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    /**
     * get device name of user's device
     */
    public String getDevice() {
        return device;
    }

    /**
     * sets device name value to a given value
     */
    public void setDevice(final String device) {
        this.device = device;
    }

    /**
     * get network type of user's network
     * Ex: wifi
     */
    public String getNetworkType() {
        return networkType;
    }

    /**
     * sets network type to a given value
     */
    public void setNetworkType(final String networkType) {
        this.networkType = networkType;
    }

}
