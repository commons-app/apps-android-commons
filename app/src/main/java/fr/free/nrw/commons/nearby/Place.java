package fr.free.nrw.commons.nearby;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import timber.log.Timber;

/**
 * A single geolocated Wikidata item
 */
public class Place {

    public final String name;
    private final Label label;
    private final String longDescription;
    private final Uri secondaryImageUrl;
    public final LatLng location;
    private final String category;

    public Bitmap image;
    private Bitmap secondaryImage;
    public String distance;
    public final Sitelinks siteLinks;


    public Place(String name, Label label, String longDescription,
                 Uri secondaryImageUrl, LatLng location, String category, Sitelinks siteLinks) {
        this.name = name;
        this.label = label;
        this.longDescription = longDescription;
        this.secondaryImageUrl = secondaryImageUrl;
        this.location = location;
        this.category = category;
        this.siteLinks = siteLinks;
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
     * Gets the secondary image url for bookmarks
     * @return secondary image url
     */
    public Uri getSecondaryImageUrl() { return this.secondaryImageUrl; }

    /**
     * Extracts the entity id from the wikidata link
     * @return returns the entity id if wikidata link exists
     */
    @Nullable
    String getWikiDataEntityId() {
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
                ", secondaryImageUrl='" + secondaryImageUrl + '\'' +
                ", location='" + location + '\'' +
                ", category='" + category + '\'' +
                ", image='" + image + '\'' +
                ", secondaryImage=" + secondaryImage +
                ", distance='" + distance + '\'' +
                ", siteLinks='" + siteLinks.toString() + '\'' +
                '}';
    }

    /**
     * See https://github.com/commons-app/apps-android-commons/issues/250
     * Most common types of desc: building, house, cottage, farmhouse,
     * village, civil parish, church, railway station,
     * gatehouse, milestone, inn, secondary school, hotel
     */
    public enum Label {

        BUILDING("Q41176", R.drawable.round_icon_generic_building),
        HOUSE("Q3947", R.drawable.round_icon_house),
        COTTAGE("Q5783996", R.drawable.round_icon_house),
        FARMHOUSE("Q489357", R.drawable.round_icon_house),
        CHURCH("Q16970", R.drawable.round_icon_church), //changed from church to church building
        RAILWAY_STATION("Q55488", R.drawable.round_icon_railway_station),
        GATEHOUSE("Q277760", R.drawable.round_icon_gatehouse),
        MILESTONE("Q10145", R.drawable.round_icon_milestone),
        INN("Q256020", R.drawable.round_icon_house), //Q27686
        HOTEL("Q27686", R.drawable.round_icon_house),
        CITY("Q515", R.drawable.round_icon_city),
        UNIVERSITY("Q3918",R.drawable.round_icon_school), //added university
        SCHOOL("Q3914", R.drawable.round_icon_school), //changed from "secondary school" to school
        EDUCATION("Q8434", R.drawable.round_icon_school), //changed from edu to education, there is no id for "edu"
        ISLE("Q23442", R.drawable.round_icon_island),
        MOUNTAIN("Q8502", R.drawable.round_icon_mountain),
        AIRPORT("Q1248784", R.drawable.round_icon_airport),
        BRIDGE("Q12280", R.drawable.round_icon_bridge),
        ROAD("Q34442", R.drawable.round_icon_road),
        FOREST("Q4421", R.drawable.round_icon_forest),
        PARK("Q22698", R.drawable.round_icon_park),
        RIVER("Q4022", R.drawable.round_icon_river),
        WATERFALL("Q34038", R.drawable.round_icon_waterfall),
        TEMPLE("Q44539",R.drawable.round_icon_church),
        UNKNOWN("?", R.drawable.round_icon_unknown);

        private static final Map<String, Label> TEXT_TO_DESCRIPTION
                = new HashMap<>(Label.values().length);

        static {
            for (Label label : values()) {
                TEXT_TO_DESCRIPTION.put(label.text, label);
            }
        }

        private final String text;
        @DrawableRes private final int icon;

        Label(String text, @DrawableRes int icon) {
            this.text = text;
            this.icon = icon;
        }

        public String getText() {
            return text;
        }

        @DrawableRes
        public int getIcon() {
            return icon;
        }

        public static Label fromText(String text) {
            Label label = TEXT_TO_DESCRIPTION.get(text);
            return label == null ? UNKNOWN : label;
        }
    }
}
