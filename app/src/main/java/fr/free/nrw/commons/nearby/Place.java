package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.lang3.StringUtils;

import androidx.annotation.Nullable;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.utils.PlaceUtils;
import timber.log.Timber;

/**
 * A single geolocated Wikidata item
 */
public class Place implements Parcelable {

    public final String name;
    private final Label label;
    private final String longDescription;
    public final LatLng location;
    private final String category;

    public String distance;
    public final Sitelinks siteLinks;


    public Place(String name, Label label, String longDescription, LatLng location, String category, Sitelinks siteLinks) {
        this.name = name;
        this.label = label;
        this.longDescription = longDescription;
        this.location = location;
        this.category = category;
        this.siteLinks = siteLinks;
    }

    public Place(Parcel in) {
        this.name = in.readString();
        this.label = (Label) in.readSerializable();
        this.longDescription = in.readString();
        this.location = in.readParcelable(LatLng.class.getClassLoader());
        this.category = in.readString();
        this.siteLinks = in.readParcelable(Sitelinks.class.getClassLoader());
    }

    public static Place from(NearbyResultItem item) {
        String itemClass = item.getClassName().getValue();
        String classEntityId = "";
        if(!StringUtils.isBlank(itemClass)) {
            classEntityId = itemClass.replace("http://www.wikidata.org/entity/", "");
        }
        return new Place(
                item.getLabel().getValue(),
                Label.fromText(classEntityId), // list
                item.getClassLabel().getValue(), // details
                PlaceUtils.latLngFromPointString(item.getLocation().getValue()),
                item.getCommonsCategory().getValue(),
                new Sitelinks.Builder()
                        .setWikipediaLink(item.getWikipediaArticle().getValue())
                        .setCommonsLink(item.getCommonsArticle().getValue())
                        .setWikidataLink(item.getItem().getValue())
                        .build());
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
    public String getLongDescription() { return longDescription; }

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
     * @return returns the entity id if wikidata link exists
     */
    @Nullable
    public String getWikiDataEntityId() {
        if (!hasWikidataLink()) {
            Timber.d("Wikidata entity ID is null for place with sitelink %s", siteLinks.toString());
            return null;
        }

        String wikiDataLink = siteLinks.getWikidataLink().toString();
        Timber.d("Wikidata entity is %s", wikiDataLink);
        return wikiDataLink.replace("http://www.wikidata.org/entity/", "");
    }

    /**
     * Checks if the Wikidata item has a Wikipedia page associated with it
     * @return true if there is a Wikipedia link
     */
    boolean hasWikipediaLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getWikipediaLink()));
    }

    /**
     * Checks if the Wikidata item has a Wikidata page associated with it
     * @return true if there is a Wikidata link
     */
    boolean hasWikidataLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getWikidataLink()));
    }

    /**
     * Checks if the Wikidata item has a Commons page associated with it
     * @return true if there is a Commons link
     */
    boolean hasCommonsLink() {
        return !(siteLinks == null || Uri.EMPTY.equals(siteLinks.getCommonsLink()));
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
                ", label='" + label + '\'' +
                ", longDescription='" + longDescription + '\'' +
                ", location='" + location + '\'' +
                ", category='" + category + '\'' +
                ", distance='" + distance + '\'' +
                ", siteLinks='" + siteLinks.toString() + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeSerializable(label);
        dest.writeString(longDescription);
        dest.writeParcelable(location, 0);
        dest.writeString(category);
        dest.writeParcelable(siteLinks, 0);
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
}
