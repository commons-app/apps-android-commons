package fr.free.nrw.commons.settings.sync

import com.google.gson.annotations.SerializedName

/**
 * Data type classification for preferences.
 */
enum class PrefType {
    BOOLEAN,
    INT,
    LONG,
    STRING,
    STRING_SET,
}

/**
 * Encapsulates a typed preference value to ensure exact restoration.
 */
data class PreferenceValueDto(
    @SerializedName("type")
    val type: PrefType = PrefType.STRING,
    @SerializedName("value")
    val value: String = "",
)

/**
 * Data model for saving and restoring user settings and bookmarks in user namespace.
 */
data class UserSettingsData(
    @SerializedName("schemaVersion")
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @SerializedName("lastUpdated")
    val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("preferences")
    val preferences: Map<String, PreferenceValueDto> = emptyMap(),
    @SerializedName("bookmarks")
    val bookmarks: BookmarksData = BookmarksData(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val USER_SETTINGS_PAGE_SUFFIX = "/commons-android-app.json"
    }
}

/**
 * Encapsulates all bookmarked entities for user namespace sync.
 */
data class BookmarksData(
    @SerializedName("locations")
    val locations: List<BookmarkLocationDto> = emptyList(),
    @SerializedName("categories")
    val categories: List<String> = emptyList(),
    @SerializedName("items")
    val items: List<BookmarkItemDto> = emptyList(),
    @SerializedName("pictures")
    val pictures: List<BookmarkPictureDto> = emptyList(),
)

/**
 * Data Transfer Object for Bookmarked Locations.
 */
data class BookmarkLocationDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("language")
    val language: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("lat")
    val lat: Double = 0.0,
    @SerializedName("long")
    val long: Double = 0.0,
    @SerializedName("category")
    val category: String = "",
    @SerializedName("labelText")
    val labelText: String = "",
    @SerializedName("labelIcon")
    val labelIcon: Int? = null,
    @SerializedName("imageUrl")
    val imageUrl: String = "",
    @SerializedName("wikipediaLink")
    val wikipediaLink: String = "",
    @SerializedName("wikidataLink")
    val wikidataLink: String = "",
    @SerializedName("commonsLink")
    val commonsLink: String = "",
    @SerializedName("pic")
    val pic: String = "",
    @SerializedName("exists")
    val exists: Boolean = true,
)

/**
 * Data transfer object for Bookmarked items
 */
data class BookmarkItemDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("instanceOfs")
    val instanceOfs: List<String> = emptyList(),
    @SerializedName("isSelected")
    val isSelected: Boolean = false,
)

/**
 * Data Transfer object for Bookmarked Pictures / Media
 */
data class BookmarkPictureDto(
    @SerializedName("mediaName")
    val mediaName: String,
    @SerializedName("mediaCreator")
    val mediaCreator: String? = null,
)
