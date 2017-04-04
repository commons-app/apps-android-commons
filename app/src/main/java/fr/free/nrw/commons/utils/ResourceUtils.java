package fr.free.nrw.commons.utils;

import android.support.annotation.DrawableRes;

import fr.free.nrw.commons.R;

public class ResourceUtils {

    /**
     * See https://github.com/commons-app/apps-android-commons/issues/250
     * Most common types of desc: building, house, cottage, farmhouse,
     * village, civil parish, church, railway station,
     * gatehouse, milestone, inn, secondary school, hotel
     * @param description Place description
     * @return icon res id
     */
    @DrawableRes
    public static int getDescriptionIcon(String description) {
        int resourceId;
        switch (description) {
            case "building":
                resourceId = R.drawable.round_icon_generic_building;
                break;
            case "house":
                resourceId = R.drawable.round_icon_house;
                break;
            case "cottage":
                resourceId = R.drawable.round_icon_house;
                break;
            case "farmhouse":
                resourceId = R.drawable.round_icon_house;
                break;
            case "church":
                resourceId = R.drawable.round_icon_church;
                break;
            case "railway station":
                resourceId = R.drawable.round_icon_railway_station;
                break;
            case "gatehouse":
                resourceId = R.drawable.round_icon_gatehouse;
                break;
            case "milestone":
                resourceId = R.drawable.round_icon_milestone;
                break;
            case "inn":
                resourceId = R.drawable.round_icon_house;
                break;
            case "city":
                resourceId = R.drawable.round_icon_city;
                break;
            case "secondary school":
                resourceId = R.drawable.round_icon_school;
                break;
            case "edu":
                resourceId = R.drawable.round_icon_school;
                break;
            case "isle":
                resourceId = R.drawable.round_icon_island;
                break;
            case "mountain":
                resourceId = R.drawable.round_icon_mountain;
                break;
            case "airport":
                resourceId = R.drawable.round_icon_airport;
                break;
            case "bridge":
                resourceId = R.drawable.round_icon_bridge;
                break;
            case "road":
                resourceId = R.drawable.round_icon_road;
                break;
            case "forest":
                resourceId = R.drawable.round_icon_forest;
                break;
            case "park":
                resourceId = R.drawable.round_icon_park;
                break;
            case "river":
                resourceId = R.drawable.round_icon_river;
                break;
            case "waterfall":
                resourceId = R.drawable.round_icon_waterfall;
                break;
            default:
                resourceId = R.drawable.round_icon_unknown;
        }
        return resourceId;
    }
}
