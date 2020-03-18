package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

/**
 * Handles the links to Wikipedia, Commons, and Wikidata that are displayed for a Place
 */
public class Sitelinks implements Parcelable {
    private final String wikipediaLink;
    private final String commonsLink;
    private final String wikidataLink;


    private Sitelinks(Parcel in) {
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

    /**
     * Creates sitelinks from the parcel
     */
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

    /**
     * Gets the Wikipedia link for a Place
     * @return Wikipedia link
     */
    public Uri getWikipediaLink() {
        return sanitiseString(wikipediaLink);
    }

    /**
     * Gets the Commons link for a Place
     * @return Commons link
     */
    public Uri getCommonsLink() {
        return sanitiseString(commonsLink);
    }

    /**
     * Gets the Wikidata link for a Place
     * @return Wikidata link
     */
    public Uri getWikidataLink() {
        return sanitiseString(wikidataLink);
    }

    /**
     * Sanitises and parses the links before using them
     * @param stringUrl unsanitised link
     * @return sanitised and parsed link
     */
    private static Uri sanitiseString(String stringUrl) {
        String sanitisedStringUrl = stringUrl.replaceAll("[<>\n\r]", "").trim();
        return Uri.parse(sanitisedStringUrl);
    }

    @Override
    public String toString() {
        return "Sitelinks{" +
                "wikipediaLink='" + wikipediaLink + '\'' +
                ", commonsLink='" + commonsLink + '\'' +
                ", wikidataLink='" + wikidataLink + '\'' +
                '}';
    }

    private Sitelinks(Sitelinks.Builder builder) {
        this.wikidataLink = builder.wikidataLink;
        this.wikipediaLink = builder.wikipediaLink;
        this.commonsLink = builder.commonsLink;
    }

    /**
     * Builds a list of Sitelinks for a Place
     */
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
