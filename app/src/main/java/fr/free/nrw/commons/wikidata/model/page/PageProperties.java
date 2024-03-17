package fr.free.nrw.commons.wikidata.model.page;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
public class PageProperties implements Parcelable {
    private final int pageId;
    @NonNull private final Namespace namespace;
    private final long revisionId;
    private final Date lastModified;
    private final String displayTitleText;
    private final String editProtectionStatus;
    private final int languageCount;
    private final boolean isMainPage;
    private final boolean isDisambiguationPage;
    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/. */
    @Nullable private final String leadImageUrl;
    @Nullable private final String leadImageName;
    @Nullable private final String titlePronunciationUrl;
    @Nullable private final Location geo;
    @Nullable private final String wikiBaseItem;
    @Nullable private final String descriptionSource;

    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
    private final boolean canEdit;

    public int getPageId() {
        return pageId;
    }

    public boolean isMainPage() {
        return isMainPage;
    }

    public boolean isDisambiguationPage() {
        return isDisambiguationPage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(pageId);
        parcel.writeInt(namespace.code());
        parcel.writeLong(revisionId);
        parcel.writeLong(lastModified.getTime());
        parcel.writeString(displayTitleText);
        parcel.writeString(titlePronunciationUrl);
        parcel.writeString(GeoMarshaller.marshal(geo));
        parcel.writeString(editProtectionStatus);
        parcel.writeInt(languageCount);
        parcel.writeInt(canEdit ? 1 : 0);
        parcel.writeInt(isMainPage ? 1 : 0);
        parcel.writeInt(isDisambiguationPage ? 1 : 0);
        parcel.writeString(leadImageUrl);
        parcel.writeString(leadImageName);
        parcel.writeString(wikiBaseItem);
        parcel.writeString(descriptionSource);
    }

    private PageProperties(Parcel in) {
        pageId = in.readInt();
        namespace = Namespace.of(in.readInt());
        revisionId = in.readLong();
        lastModified = new Date(in.readLong());
        displayTitleText = in.readString();
        titlePronunciationUrl = in.readString();
        geo = GeoUnmarshaller.unmarshal(in.readString());
        editProtectionStatus = in.readString();
        languageCount = in.readInt();
        canEdit = in.readInt() == 1;
        isMainPage = in.readInt() == 1;
        isDisambiguationPage = in.readInt() == 1;
        leadImageUrl = in.readString();
        leadImageName = in.readString();
        wikiBaseItem = in.readString();
        descriptionSource = in.readString();
    }

    public static final Parcelable.Creator<PageProperties> CREATOR
            = new Parcelable.Creator<PageProperties>() {
        @Override
        public PageProperties createFromParcel(Parcel in) {
            return new PageProperties(in);
        }

        @Override
        public PageProperties[] newArray(int size) {
            return new PageProperties[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PageProperties that = (PageProperties) o;

        return pageId == that.pageId
                && namespace == that.namespace
                && revisionId == that.revisionId
                && lastModified.equals(that.lastModified)
                && displayTitleText.equals(that.displayTitleText)
                && TextUtils.equals(titlePronunciationUrl, that.titlePronunciationUrl)
                && (geo == that.geo || geo != null && geo.equals(that.geo))
                && languageCount == that.languageCount
                && canEdit == that.canEdit
                && isMainPage == that.isMainPage
                && isDisambiguationPage == that.isDisambiguationPage
                && TextUtils.equals(editProtectionStatus, that.editProtectionStatus)
                && TextUtils.equals(leadImageUrl, that.leadImageUrl)
                && TextUtils.equals(leadImageName, that.leadImageName)
                && TextUtils.equals(wikiBaseItem, that.wikiBaseItem);
    }

    @Override
    public int hashCode() {
        int result = lastModified.hashCode();
        result = 31 * result + displayTitleText.hashCode();
        result = 31 * result + (titlePronunciationUrl != null ? titlePronunciationUrl.hashCode() : 0);
        result = 31 * result + (geo != null ? geo.hashCode() : 0);
        result = 31 * result + (editProtectionStatus != null ? editProtectionStatus.hashCode() : 0);
        result = 31 * result + languageCount;
        result = 31 * result + (isMainPage ? 1 : 0);
        result = 31 * result + (isDisambiguationPage ? 1 : 0);
        result = 31 * result + (leadImageUrl != null ? leadImageUrl.hashCode() : 0);
        result = 31 * result + (leadImageName != null ? leadImageName.hashCode() : 0);
        result = 31 * result + (wikiBaseItem != null ? wikiBaseItem.hashCode() : 0);
        result = 31 * result + (canEdit ? 1 : 0);
        result = 31 * result + pageId;
        result = 31 * result + namespace.code();
        result = 31 * result + (int) revisionId;
        return result;
    }
}
