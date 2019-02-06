package fr.free.nrw.commons.media.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ExtMetadata {
    @SuppressWarnings("unused") @SerializedName("DateTime") @Nullable
    private Values dateTime;
    @SuppressWarnings("unused") @SerializedName("ObjectName") @Nullable private Values objectName;
    @SuppressWarnings("unused") @SerializedName("CommonsMetadataExtension") @Nullable private Values commonsMetadataExtension;
    @SuppressWarnings("unused") @SerializedName("Categories") @Nullable private Values categories;
    @SuppressWarnings("unused") @SerializedName("Assessments") @Nullable private Values assessments;
    @SuppressWarnings("unused") @SerializedName("ImageDescription") @Nullable private Values imageDescription;
    @SuppressWarnings("unused") @SerializedName("DateTimeOriginal") @Nullable private Values dateTimeOriginal;
    @SuppressWarnings("unused") @SerializedName("Artist") @Nullable private Values artist;
    @SuppressWarnings("unused") @SerializedName("Credit") @Nullable private Values credit;
    @SuppressWarnings("unused") @SerializedName("Permission") @Nullable private Values permission;
    @SuppressWarnings("unused") @SerializedName("AuthorCount") @Nullable private Values authorCount;
    @SuppressWarnings("unused") @SerializedName("LicenseShortName") @Nullable private Values licenseShortName;
    @SuppressWarnings("unused") @SerializedName("UsageTerms") @Nullable private Values usageTerms;
    @SuppressWarnings("unused") @SerializedName("LicenseUrl") @Nullable private Values licenseUrl;
    @SuppressWarnings("unused") @SerializedName("AttributionRequired") @Nullable private Values attributionRequired;
    @SuppressWarnings("unused") @SerializedName("Copyrighted") @Nullable private Values copyrighted;
    @SuppressWarnings("unused") @SerializedName("Restrictions") @Nullable private Values restrictions;
    @SuppressWarnings("unused") @SerializedName("License") @Nullable private Values license;

    @Nullable public Values licenseShortName() {
        return licenseShortName;
    }

    @Nullable public Values licenseUrl() {
        return licenseUrl;
    }

    @Nullable public Values license() {
        return license;
    }

    @Nullable public Values imageDescription() {
        return imageDescription;
    }

    @Nullable
    public Values getDateTime() {
        return dateTime;
    }

    @Nullable
    public Values getObjectName() {
        return objectName;
    }

    @Nullable
    public Values getCommonsMetadataExtension() {
        return commonsMetadataExtension;
    }

    @Nullable
    public Values getCategories() {
        return categories;
    }

    @Nullable
    public Values getAssessments() {
        return assessments;
    }

    @Nullable
    public Values getImageDescription() {
        return imageDescription;
    }

    @Nullable
    public Values getDateTimeOriginal() {
        return dateTimeOriginal;
    }

    @Nullable
    public Values getArtist() {
        return artist;
    }

    @Nullable
    public Values getCredit() {
        return credit;
    }

    @Nullable
    public Values getPermission() {
        return permission;
    }

    @Nullable
    public Values getAuthorCount() {
        return authorCount;
    }

    @Nullable
    public Values getLicenseShortName() {
        return licenseShortName;
    }

    @Nullable
    public Values getUsageTerms() {
        return usageTerms;
    }

    @Nullable
    public Values getLicenseUrl() {
        return licenseUrl;
    }

    @Nullable
    public Values getAttributionRequired() {
        return attributionRequired;
    }

    @Nullable
    public Values getCopyrighted() {
        return copyrighted;
    }

    @Nullable
    public Values getRestrictions() {
        return restrictions;
    }

    @Nullable
    public Values getLicense() {
        return license;
    }

    @Nullable public Values objectName() {
        return objectName;
    }

    @Nullable public Values usageTerms() {
        return usageTerms;
    }

    @Nullable public Values artist() {
        return artist;
    }

    public class Values {
        @SuppressWarnings("unused,NullableProblems") @NonNull
        private String value;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String source;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String hidden;

        @NonNull public String value() {
            return value;
        }

        @NonNull public String source() {
            return source;
        }
    }
}