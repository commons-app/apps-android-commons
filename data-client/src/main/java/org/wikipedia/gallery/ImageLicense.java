package org.wikipedia.gallery;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class ImageLicense implements Serializable {
    private static final String CREATIVE_COMMONS_PREFIX = "cc";
    private static final String PUBLIC_DOMAIN_PREFIX = "pd";
    private static final String CC_BY_SA = "ccbysa";

    @NonNull @SerializedName("type") private final String license;
    @NonNull @SerializedName("code") private final String licenseShortName;
    @NonNull @SerializedName("url") private final String licenseUrl;

    public ImageLicense(@NonNull ExtMetadata metadata) {
        this.license = metadata.license();
        this.licenseShortName = metadata.licenseShortName();
        this.licenseUrl = metadata.licenseUrl();
    }

    private ImageLicense(@NonNull String license, @NonNull String licenseShortName, @NonNull String licenseUrl) {
        this.license = license;
        this.licenseShortName = licenseShortName;
        this.licenseUrl = licenseUrl;
    }

    public ImageLicense() {
        this("", "", "");
    }

    @NonNull public String getLicenseName() {
        return license;
    }

    @NonNull public String getLicenseShortName() {
        return licenseShortName;
    }

    @NonNull public String getLicenseUrl() {
        return licenseUrl;
    }

    public boolean isLicenseCC() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(CREATIVE_COMMONS_PREFIX);
    }

    public boolean isLicensePD() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).startsWith(PUBLIC_DOMAIN_PREFIX);
    }

    public boolean isLicenseCCBySa() {
        return defaultString(license).toLowerCase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA)
                || defaultString(licenseShortName).toLowerCase(Locale.ENGLISH).replace("-", "").startsWith(CC_BY_SA);
    }

    public boolean hasLicenseInfo() {
        return !(license.isEmpty() && licenseShortName.isEmpty() && licenseUrl.isEmpty());
    }
}
