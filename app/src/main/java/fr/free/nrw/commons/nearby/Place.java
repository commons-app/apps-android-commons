package fr.free.nrw.commons.nearby;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.DrawableRes;

import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;

public class Place {

    public final String name;
    private final Label label;
    private final String longDescription;
    private final Uri secondaryImageUrl;
    public final LatLng location;

    public Bitmap image;
    public Bitmap secondaryImage;
    public String distance;
    public final Sitelinks siteLinks;


    public Place(String name, Label label, String longDescription,
                 Uri secondaryImageUrl, LatLng location, Sitelinks siteLinks) {
        this.name = name;
        this.label = label;
        this.longDescription = longDescription;
        this.secondaryImageUrl = secondaryImageUrl;
        this.location = location;
        this.siteLinks = siteLinks;
    }

    public Label getLabel() {
        return label;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

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
        return String.format("Place(%s@%s)", name, location);
    }

    /**
     * See https://github.com/commons-app/apps-android-commons/issues/250
     * Most common types of desc: building, house, cottage, farmhouse,
     * village, civil parish, church, railway station,
     * gatehouse, milestone, inn, secondary school, hotel
     *
     * TODO Give a more accurate class name (see issue #742).
     */
    public enum Label {

        BUILDING("building", R.drawable.round_icon_generic_building),
        HOUSE("house", R.drawable.round_icon_house),
        COTTAGE("cottage", R.drawable.round_icon_house),
        FARMHOUSE("farmhouse", R.drawable.round_icon_house),
        CHURCH("church", R.drawable.round_icon_church),
        RAILWAY_STATION("railway station", R.drawable.round_icon_railway_station),
        GATEHOUSE("gatehouse", R.drawable.round_icon_gatehouse),
        MILESTONE("milestone", R.drawable.round_icon_milestone),
        INN("inn", R.drawable.round_icon_house),
        CITY("city", R.drawable.round_icon_city),
        SECONDARY_SCHOOL("secondary school", R.drawable.round_icon_school),
        EDU("edu", R.drawable.round_icon_school),
        ISLE("isle", R.drawable.round_icon_island),
        MOUNTAIN("mountain", R.drawable.round_icon_mountain),
        AIRPORT("airport", R.drawable.round_icon_airport),
        BRIDGE("bridge", R.drawable.round_icon_bridge),
        ROAD("road", R.drawable.round_icon_road),
        FOREST("forest", R.drawable.round_icon_forest),
        PARK("park", R.drawable.round_icon_park),
        RIVER("river", R.drawable.round_icon_river),
        WATERFALL("waterfall", R.drawable.round_icon_waterfall),
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
