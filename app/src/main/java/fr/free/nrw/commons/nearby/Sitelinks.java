package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import fr.free.nrw.commons.Utils;

public class Sitelinks implements Parcelable {
    private final String wikipediaLink;
    private final String commonsLink;
    private final String wikidataLink;


    protected Sitelinks(Parcel in) {
        wikipediaLink = in.readString();
        commonsLink = in.readString();
        wikidataLink = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(wikipediaLink);
        dest.writeString(commonsLink);
        dest.writeString(wikidataLink);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Sitelinks> CREATOR = new Creator<Sitelinks>() {
        @Override
        public Sitelinks createFromParcel(Parcel in) {
            return new Sitelinks(in);
        }

        @Override
        public Sitelinks[] newArray(int size) {
            return new Sitelinks[size];
        }
    };

    @Nullable
    public Uri getWikipediaLink() {
        return sanitiseString(wikipediaLink);
    }

    @Nullable
    public Uri getCommonsLink() {
        return sanitiseString(commonsLink);
    }

    @Nullable
    public Uri getWikidataLink() {
        return sanitiseString(wikidataLink);
    }

    @Nullable
    private Uri sanitiseString(String stringUrl) {
        stringUrl = stringUrl
                .replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll("\n", "");
        if (!Utils.isNullOrWhiteSpace(stringUrl) && stringUrl != null) {
            return Uri.parse(stringUrl);
        }
        return null;
    }

    public Sitelinks(Sitelinks.Builder builder) {
        this.wikidataLink = builder.wikidataLink;
        this.wikipediaLink = builder.wikipediaLink;
        this.commonsLink = builder.commonsLink;
    }

    public static class Builder {
        private String wikidataLink;
        private String commonsLink;
        private String wikipediaLink;

        public Builder() {
        }

        public Sitelinks.Builder setWikipediaLink(String link) {
            this.wikipediaLink = link;
            return this;
        }

        public Sitelinks.Builder setWikidataLink(String link) {
            this.wikidataLink = link;
            return this;
        }

        public Sitelinks.Builder setCommonsLink(@Nullable String link) {
            this.commonsLink = link;
            return this;
        }

        public Sitelinks build() {
            return new Sitelinks(this);
        }
    }
}