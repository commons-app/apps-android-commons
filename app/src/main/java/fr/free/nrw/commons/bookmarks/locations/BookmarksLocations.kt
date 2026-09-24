package fr.free.nrw.commons.bookmarks.locations

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks

@Entity(tableName = "bookmarks_locations")
data class BookmarksLocations(
    @PrimaryKey @ColumnInfo(name = "location_name") val locationName: String,
    @ColumnInfo(name = "location_language") val locationLanguage: String,
    @ColumnInfo(name = "location_description") val locationDescription: String,
    @ColumnInfo(name = "location_lat") val locationLat: Double,
    @ColumnInfo(name = "location_long") val locationLong: Double,
    @ColumnInfo(name = "location_category") val locationCategory: String,
    @ColumnInfo(name = "location_label_text") val locationLabelText: String,
    @ColumnInfo(name = "location_label_icon") val locationLabelIcon: Int?,
    @ColumnInfo(name = "location_image_url") val locationImageUrl: String,
    @ColumnInfo(name = "location_wikipedia_link") val locationWikipediaLink: String,
    @ColumnInfo(name = "location_wikidata_link") val locationWikidataLink: String,
    @ColumnInfo(name = "location_commons_link") val locationCommonsLink: String,
    @ColumnInfo(name = "location_pic") val locationPic: String,
    @ColumnInfo(name = "location_exists") val locationExists: Boolean
)
