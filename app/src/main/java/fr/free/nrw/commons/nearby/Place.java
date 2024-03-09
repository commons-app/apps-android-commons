package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.utils.PlaceUtils;
import timber.log.Timber;

/**
 * A single geolocated Wikidata item
 */
public class Place implements Parcelable {

    public final String language;
    public final String name;
    private final Label label;
    private final String longDescription;
    public final LatLng location;
    private final String category;
    public final String pic;
    // exists boolean will tell whether the place exists or not,
    // For a place to be existing both destroyed and endTime property should be null but it is also not necessary for a non-existing place to have both properties either one property is enough (in such case that not given property will be considered as null).
    public final Boolean exists;

    public String distance;
    public final Sitelinks siteLinks;
    private boolean isMonument;
    private String thumb;

    public Place() {
        language = null;
        name = null;
        label = null;
        longDescription = null;
        location = null;
        category = null;
        pic = null;
        exists = null;
        siteLinks = null;
    }

    public Place(String language,String name, Label label, String longDescription, LatLng location, String category, Sitelinks siteLinks, String pic, Boolean exists) {
        this.language = language;
        this.name = name;
        this.label = label;
        this.longDescription = longDescription;
        this.location = location;
        this.category = category;
        this.siteLinks = siteLinks;
        this.pic = (pic == null) ? "":pic;
        this.exists = exists;
    }

    public Place(String name, String longDescription, LatLng location, String category, Sitelinks siteLinks, String pic, String thumb) {
        this.name = name;
        this.longDescription = longDescription;
        this.location = location;
        this.category = category;
        this.siteLinks = siteLinks;
        this.pic = (pic == null) ? "":pic;
        this.thumb = thumb;
        this.language = null;
        this.label = null;
        this.exists = true;
    }

    public Place(Parcel in) {
        this.language = in.readString();
        this.name = in.readString();
        this.label = (Label) in.readSerializable();
        this.longDescription = in.readString();
        this.location = in.readParcelable(LatLng.class.getClassLoader());
        this.category = in.readString();
        this.siteLinks = in.readParcelable(Sitelinks.class.getClassLoader());
        String picString = in.readString();
        this.pic = (picString == null) ? "":picString;
        String existString = in.readString();
        this.exists = Boolean.parseBoolean(existString);
        this.isMonument = in.readInt() == 1;
    }
    public static Place from(NearbyResultItem item) {
        String itemClass = item.getClassName().getValue();
        String classEntityId = "";
        if(!StringUtils.isBlank(itemClass)) {
            classEntityId = itemClass.replace("http://www.wikidata.org/entity/", "");
        }
        // Set description when not null and not empty
        String description = (item.getDescription().getValue() != null && !item.getDescription().getValue().isEmpty()) ? item.getDescription().getValue() : "";
        // When description is "?" but we have a valid label, just use the label. So replace "?" by "" in description
        description = (description.equals("?")
            && (item.getLabel().getValue() != null
            && !item.getLabel().getValue().isEmpty()) ? "" : description);
        /*
         * If we have a valid label
         *     - If have a valid label add the description at the end of the string with parenthesis
         *     - If we don't have a valid label, string will include only the description. So add it without paranthesis
         */
        description = ((item.getLabel().getValue() != null && !item.getLabel().getValue().isEmpty())
            ? item.getLabel().getValue()
                + ((description != null && !description.isEmpty())
                ? " (" + description + ")" : "")
            : description);
        return new Place(
            item.getLabel().getLanguage(),
            item.getLabel().getValue(),
            Label.fromText(classEntityId), // list
            description, // description and label of Wikidata item
            PlaceUtils.latLngFromPointString(item.getLocation().getValue()),
            item.getCommonsCategory().getValue(),
            new Sitelinks.Builder()
                .setWikipediaLink(item.getWikipediaArticle().getValue())
                .setCommonsLink(item.getCommonsArticle().getValue())
                .setWikidataLink(item.getItem().getValue())
                .build(),
            item.getPic().getValue(),
            // Checking if the place exists or not
            (item.getDestroyed().getValue() == "") && (item.getEndTime().getValue() == ""));
    }

    /**
     * Gets the language of the caption ie name.
     * @return language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Gets the name of the place
     * @return name
     */
    public String getName() { return name; }

    /** Gets the label of the place
     * e.g. "building", "city", etc
     * @return label
     */
    public Label getLabel() {
        return label;
    }

    public LatLng getLocation() {
        return location;
    }

    /**
     * Gets the long description of the place
     * @return long description
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * Gets the Commons category of the place
     * @return Commons category
     */
    public String getCategory() {return category; }

    /**
     * Sets the distance of the place from the user's location
     * @param distance distance of place from user's location
     */
    public void setDistance(String distance) {
        this.distance = distance;
    }

    /**
     * Extracts the entity id from the wikidata link
     * @return returns the entity id if wikidata link destroyed
     */
    @Nullable
    public String getWikiDataEntityId() {
        if (!hasWikidataLink()) {
            Timber.d("Wikidata entity ID is null for place with sitelink %s", siteLinks.toString());
            return null;
        }

        String wikiDataLink = siteLinks.getWikidataLink().toString();
        return wikiDataLink.replace("http://www.wikidata.org/entity/", "");
    }

    /**
     * Checks if the Wikidata item has a Wikipedia page associated with it
     * @return true if there is a Wikipedia link
     */
    public boolean hasWikipediaLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getWikipediaLink()));
    }

    /**
     * Checks if the Wikidata item has a Wikidata page associated with it
     * @return true if there is a Wikidata link
     */
    public boolean hasWikidataLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getWikidataLink()));
    }

    /**
     * Checks if the Wikidata item has a Commons page associated with it
     * @return true if there is a Commons link
     */
    public boolean hasCommonsLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getCommonsLink()));
    }

    /**
     * Sets that this place in nearby is a WikiData monument
     * @param monument
     */
    public void setMonument(final boolean monument) {
        isMonument = monument;
    }

    /**
     * Returns if this place is a WikiData monument
     * @return
     */
    public boolean isMonument() {
        return isMonument;
    }

    /**
     * Check if we already have the exact same Place
     * @param o Place being tested
     * @return true if name and location of Place is exactly the same
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Place) {
            Place that = (Place) o;
            return this.name.equals(that.name) && this.location.equals(that.location);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 + this.location.hashCode();
    }

    @Override
    public String toString() {
        return "Place{" +
                "name='" + name + '\'' +
                ", lang='" + language + '\'' +
                ", label='" + label + '\'' +
                ", longDescription='" + longDescription + '\'' +
                ", location='" + location + '\'' +
                ", category='" + category + '\'' +
                ", distance='" + distance + '\'' +
                ", siteLinks='" + siteLinks.toString() + '\'' +
                ", pic='" + pic + '\'' +
                ", exists='" + exists.toString() + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(language);
        dest.writeString(name);
        dest.writeSerializable(label);
        dest.writeString(longDescription);
        dest.writeParcelable(location, 0);
        dest.writeString(category);
        dest.writeParcelable(siteLinks, 0);
        dest.writeString(pic);
        dest.writeString(exists.toString());
        dest.writeInt(isMonument ? 1 : 0);
    }

    public static final Creator<Place> CREATOR = new Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel in) {
            return new Place(in);
        }

        @Override
        public Place[] newArray(int size) {
            return new Place[size];
        }
    };

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }
}
