package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class GeoSearchItem {
    @Nullable private String title;
    @SerializedName("lat") private double latitude;
    @SerializedName("lon") private double longitude;
    @SerializedName("dist") private double distance;

    @NonNull public String getTitle() {
        return StringUtils.defaultString(title);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }
}
