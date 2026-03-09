package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import androidx.annotation.DrawableRes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.R;

/**
 * See https://github.com/commons-app/apps-android-commons/issues/250
 * Most common types of desc: building, house, cottage, farmhouse,
 * village, civil parish, church, railway station,
 * gatehouse, milestone, inn, secondary school, hotel
 */
public enum Label {

    BOOKMARKS(0, R.drawable.ic_filled_star_24dp),
    BUILDING(R.array.building_QIDs, R.drawable.round_icon_generic_building),
    BANK(R.array.bank_QIDs, R.drawable.round_icon_bank),
    HOSPITAL(R.array.hospital_QIDs, R.drawable.round_icon_hospital),
    HOUSE(R.array.house_QIDs, R.drawable.round_icon_house),
    COTTAGE(R.array.cottage_Ids, R.drawable.round_icon_house),
    FARMHOUSE(R.array.farmhouse_QIDs, R.drawable.round_icon_house),
    CHURCH(R.array.church_QIDs, R.drawable.round_icon_church),
    GAS_STATION(R.array.gas_station_QIDs, R.drawable.round_icon_gas_station),
    RAILWAY_STATION(R.array.railway_station_QIDs, R.drawable.round_icon_railway_station),
    GATEHOUSE(R.array.gatehouse_QIDs, R.drawable.round_icon_gatehouse),
    MILESTONE(R.array.milestone_QIDs, R.drawable.round_icon_milestone),
    INN(R.array.inn_QIDs, R.drawable.round_icon_house),
    HOTEL(R.array.hotel_QIDs, R.drawable.round_icon_house),
    CITY(R.array.city_QIDs, R.drawable.round_icon_city),
    UNIVERSITY(R.array.university_QIDs, R.drawable.round_icon_school),
    SCHOOL(R.array.school_QIDs, R.drawable.round_icon_school),
    EDUCATION(R.array.education_QIDs, R.drawable.round_icon_school),
    ISLE(R.array.island_QIDs, R.drawable.round_icon_island),
    MOUNTAIN(R.array.mountain_QIDs, R.drawable.round_icon_mountain),
    AIRPORT(R.array.airport_QIDs, R.drawable.round_icon_airport),
    BRIDGE(R.array.bridge_QIDs, R.drawable.round_icon_bridge),
    ROAD(R.array.road_QIDs, R.drawable.round_icon_road),
    FOREST(R.array.forest_QIDs, R.drawable.round_icon_forest),
    PARK(R.array.park_QIDs, R.drawable.round_icon_park),
    RIVER(R.array.river_QIDs, R.drawable.round_icon_river),
    WATERFALL(R.array.waterfall_QIDs, R.drawable.round_icon_waterfall),
    TEMPLE(R.array.temple_QIDs, R.drawable.round_icon_church),
    UNKNOWN(0, R.drawable.round_icon_unknown);

    /**
     * Lookup map which maps Q-ID -> Label.
     */
    public static final Map<String, Label> TEXT_TO_DESCRIPTION = new HashMap<>();

    private final int arrayResId;
    @DrawableRes
    private final int icon;
    private boolean selected;

    Label(final int arrayResId, @DrawableRes final int icon) {
        this.arrayResId = arrayResId;
        this.icon = icon;
    }

    Label(final Parcel in) {
        this.icon = in.readInt();
        this.arrayResId = 0;
    }

    /**
     * Loads Q-IDs from Android resources
     *
     * @param context any Android context applicationContext - much safer
     */
    public static void init(final Context context) {
        final Resources res = context.getResources();
        for (final Label label : values()) {
            if (label.arrayResId != 0) {
                final int[] resArray = res.getIntArray(label.arrayResId);
                for (final int id : resArray) {
                    final String qid = "Q" + id;
                    TEXT_TO_DESCRIPTION.put(qid, label);
                }
            }
        }
    }

    /**
     * Will be used for nearby filter, to determine if place type is selected or not
     *
     * @param isSelected true if user selected the place type
     */
    public void setSelected(final boolean isSelected) {
        this.selected = isSelected;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getText() {
        return name();
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public static Label fromText(final String text) {
        if ("BOOKMARK".equals(text)) {
            return BOOKMARKS;
        }
        final Label label = TEXT_TO_DESCRIPTION.get(text);
        return label == null ? UNKNOWN : label;
    }

    public static List<Label> valuesAsList() {
        return Arrays.asList(Label.values());
    }
}