package fr.free.nrw.commons.feedback;

/**
 * Pojo class for storing information that are required while uploading a feedback
 */
public class Feedback {
    // Version of app
    private String version;
    // API level of user's phone
    private String apiLevel;
    // Title/Description entered by user
    private String title;
    // Android version of user's device
    private String androidVersion;
    // Device Model of user's device
    private String deviceModel;
    // Device manufacturer name
    private String deviceManufacturer;
    // Device name stored on user's device
    private String device;
    // network type user is having (Ex: Wifi)
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

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getApiLevel() {
        return apiLevel;
    }

    public void setApiLevel(final String apiLevel) {
        this.apiLevel = apiLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(final String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(final String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(final String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(final String device) {
        this.device = device;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(final String networkType) {
        this.networkType = networkType;
    }

}
