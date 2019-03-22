package fr.free.nrw.commons.media.model;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.utils.StringUtils;

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

    @NonNull public Values dateTime() {
        return dateTime != null ? dateTime : new Values();
    }

    @NonNull public Values dateTimeOriginal() {
        return dateTimeOriginal != null ? dateTimeOriginal : new Values();
    }

    @NonNull public Values licenseShortName() {
        return licenseShortName != null ? licenseShortName : new Values();
    }

    @NonNull public Values licenseUrl() {
        return licenseUrl != null ? licenseUrl : new Values();
    }

    @NonNull public Values license() {
        return license != null ? license : new Values();
    }

    @NonNull public Values imageDescription() {
        return imageDescription != null ? imageDescription : new Values();
    }

    @NonNull public Values objectName() {
        return objectName != null ? objectName : new Values();
    }

    @NonNull public Values usageTerms() {
        return usageTerms != null ? usageTerms : new Values();
    }

    @NonNull public Values artist() {
        return artist != null ? artist : new Values();
    }

    public class Values {
        @SuppressWarnings("unused,NullableProblems") @Nullable private String value;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String source;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String hidden;

        @NonNull public String value() {
            return StringUtils.defaultString(value);
        }

        @NonNull public String source() {
            return StringUtils.defaultString(source);
        }
    }
}
