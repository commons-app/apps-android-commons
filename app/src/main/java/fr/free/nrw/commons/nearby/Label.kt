package fr.free.nrw.commons.nearby

import androidx.annotation.DrawableRes
import fr.free.nrw.commons.R
import java.util.Arrays

/**
 * See https://github.com/commons-app/apps-android-commons/issues/250
 * Most common types of desc: building, house, cottage, farmhouse,
 * village, civil parish, church, railway station,
 * gatehouse, milestone, inn, secondary school, hotel
 */
enum class Label {
    BOOKMARKS("BOOKMARK", R.drawable.ic_filled_star_24dp),
    BUILDING("Q41176", R.drawable.round_icon_generic_building),
    HOUSE("Q3947", R.drawable.round_icon_house),
    COTTAGE("Q5783996", R.drawable.round_icon_house),
    FARMHOUSE("Q489357", R.drawable.round_icon_house),
    CHURCH("Q16970", R.drawable.round_icon_church),  //changed from church to church building
    RAILWAY_STATION("Q55488", R.drawable.round_icon_railway_station),
    GATEHOUSE("Q277760", R.drawable.round_icon_gatehouse),
    MILESTONE("Q10145", R.drawable.round_icon_milestone),
    INN("Q256020", R.drawable.round_icon_house),  //Q27686
    HOTEL("Q27686", R.drawable.round_icon_house),
    CITY("Q515", R.drawable.round_icon_city),
    UNIVERSITY("Q3918", R.drawable.round_icon_school),  //added university
    SCHOOL("Q3914", R.drawable.round_icon_school),  //changed from "secondary school" to school
    EDUCATION("Q8434", R.drawable.round_icon_school),  //changed from edu to education, there is no id for "edu"
    ISLE("Q23442", R.drawable.round_icon_island),
    MOUNTAIN("Q8502", R.drawable.round_icon_mountain),
    AIRPORT("Q1248784", R.drawable.round_icon_airport),
    BRIDGE("Q12280", R.drawable.round_icon_bridge),
    ROAD("Q34442", R.drawable.round_icon_road),
    FOREST("Q4421", R.drawable.round_icon_forest),
    PARK("Q22698", R.drawable.round_icon_park),
    RIVER("Q4022", R.drawable.round_icon_river),
    WATERFALL("Q34038", R.drawable.round_icon_waterfall),
    TEMPLE("Q44539", R.drawable.round_icon_church),
    UNKNOWN("?", R.drawable.round_icon_unknown);

    @JvmField
    val text: String?

    @JvmField
    val icon: Int

    /**
     * Will be used for nearby filter, to determine if place type is selected or not
     * @param isSelected true if user selected the place type
     */
    var isSelected: Boolean = false

    constructor(text: String, @DrawableRes icon: Int) {
        this.text = text
        this.icon = icon
    }

    companion object {
        val TEXT_TO_DESCRIPTION : MutableMap<String, Label> =
            Label.entries.associateBy { it.text!! }.toMutableMap()

        @JvmStatic
        fun fromText(text: String?): Label {
            return TEXT_TO_DESCRIPTION.getOrDefault(text, UNKNOWN)
        }
    }
}