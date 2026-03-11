package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import android.util.SparseArray;
import androidx.annotation.DrawableRes;

import java.util.Arrays;
import java.util.List;

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
    COTTAGE(R.array.cottage_QIDs, R.drawable.round_icon_house),
    FARMHOUSE(R.array.farmhouse_QIDs, R.drawable.round_icon_house),
    TEMPLE(R.array.temple_QIDs, R.drawable.round_icon_church),
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
    UNKNOWN(0, R.drawable.round_icon_unknown);

    /**
     * Sorted array of Q-ID integers for binary-search lookup.
     */
    private static volatile int[] QIDS = new int[0];

    /**
     * Labels parallel to {@link #QIDS}: QIDS[i] maps to QID_LABELS[i].
     */
    private static volatile Label[] QID_LABELS = new Label[0];

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
     * Loads Q-IDs from Android resources into sorted primitive arrays.
     * This method is idempotent; subsequent calls after the first are no-ops.
     *
     * @param context any Android context; applicationContext is safest
     */
    public static synchronized void init(final Context context) {
        if (QIDS.length != 0) {
            return;
        }
        final Resources res = context.getResources();
        // load all values in temp map which will be sorted by default
        final SparseArray<Label> tempMap = new SparseArray<>();
        for (final Label label : values()) {
            if (label.arrayResId != 0) {
                final int[] resArray = res.getIntArray(label.arrayResId);
                for (final int id : resArray) {
                    tempMap.put(id, label);
                }
            }
        }
        //separate the ids and labels into arrays for binary search
        final int[] ids = new int[tempMap.size()];
        final Label[] labels = new Label[tempMap.size()];
        for (int i = 0; i < tempMap.size(); i++) {
            ids[i] = tempMap.keyAt(i);
            labels[i] = tempMap.valueAt(i);
        }
        tempMap.clear();
        QIDS = ids;
        QID_LABELS = labels;
    }

    /**
     * Looks up a Label by its numeric Q-ID using binary search.
     *
     * @param id the integer Q-ID (e.g. 16970)
     * @return the matching {@link Label}, or {@link Label#UNKNOWN} if not found
     */
    public static Label fromQidInt(final int id) {
        final int pos = Arrays.binarySearch(QIDS, id);
        return pos >= 0 ? QID_LABELS[pos] : UNKNOWN;
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
        if (!(text.charAt(0) == 'Q')) {
            return UNKNOWN;
        }
        return fromQidInt(Integer.parseInt(text.substring(1)));
    }

    public static List<Label> valuesAsList() {
        return Arrays.asList(Label.values());
    }
}