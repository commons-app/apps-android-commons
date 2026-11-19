package fr.free.nrw.commons.settings.sync

import com.google.gson.Gson
import com.google.gson.JsonParser
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.category.BookmarksCategoryModal
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarksLocations
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.settings.sync.UserSettingsData.Companion.USER_SETTINGS_PAGE_SUFFIX
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Service manager responsible for backing up and restoring user settings and bookmarks
 * to/from the user's Wikimedia Commons user subpage: User:<Username>/commons-android-app.json.
 */
@Singleton
@Suppress("LongParameterList")
class UserSettingsSyncManager
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        @param:Named("commons-page-edit") private val pageEditClient: PageEditClient,
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
        private val bookmarkLocationsDao: BookmarkLocationsDao,
        private val bookmarkCategoriesDao: BookmarkCategoriesDao,
        private val bookmarkItemsDao: BookmarkItemsDao,
        private val bookmarkPicturesDao: BookmarkPicturesDao,
        private val gson: Gson,
    ) {
        companion object {
            private const val EDIT_SUMMARY_BACKUP = "Backing up Commons Android App settings and bookmarks"
            private val SYNCABLE_PREFERENCE_KEYS =
                setOf(
                    Prefs.KEY_THEME_VALUE,
                    Prefs.APP_UI_LANGUAGE,
                    "appUiDefaultLanguagePref",
                    Prefs.DESCRIPTION_LANGUAGE,
                    "descriptionDefaultLanguagePref",
                    Prefs.DEFAULT_LICENSE,
                    Prefs.VIDEO_QUALITY,
                    Prefs.MANAGED_EXIF_TAGS,
                    "displayNearbyCardView",
                    "displayLocationPermissionForCardView",
                    "displayCampaignsCardView",
                    "displayDeletionButton",
                    "useExternalStorage",
                    "inAppCameraLocationPref",
                    "openDocumentPhotoPickerPref",
                    "useAuthorName",
                    "authorName",
                )

            private val KNOWN_BOOLEAN_KEYS =
                setOf(
                    "displayNearbyCardView",
                    "displayLocationPermissionForCardView",
                    "displayCampaignsCardView",
                    "displayDeletionButton",
                    "useExternalStorage",
                    "inAppCameraLocationPref",
                    "openDocumentPhotoPickerPref",
                    "useAuthorName",
                )
        }

        /**
         * Backs up current user settings and bookmarks to User:<Username>/commons-android-app.json
         */
        fun backupToUserPage(): Single<Boolean> {
            val userName = sessionManager.userName
            if (!sessionManager.isUserLoggedIn || userName.isNullOrBlank()) {
                return Single.error(IllegalStateException("User must be logged in to sync settings."))
            }

            val pageTitle = "User:$userName$USER_SETTINGS_PAGE_SUFFIX"

            return Single
                .fromCallable {
                    val preferencesMap = extractPreferences()
                    val bookmarksData = collectBookmarks()
                    val userSettingsData =
                        UserSettingsData(
                            schemaVersion = UserSettingsData.CURRENT_SCHEMA_VERSION,
                            lastUpdated = System.currentTimeMillis(),
                            preferences = preferencesMap,
                            bookmarks = bookmarksData,
                        )
                    gson.toJson(userSettingsData)
                }.flatMap { jsonContent ->
                    pageEditClient
                        .edit(pageTitle, jsonContent, EDIT_SUMMARY_BACKUP)
                        .singleOrError()
                }.subscribeOn(Schedulers.io())
        }

        /**
         * Restores user settings and bookmarks from User:<Username>/commons-android-app.json
         */
        fun restoreFromUserPage(): Single<Boolean> {
            val userName = sessionManager.userName
            if (!sessionManager.isUserLoggedIn || userName.isNullOrBlank()) {
                return Single.error(IllegalStateException("User must be logged in to restore settings."))
            }

            val pageTitle = "User:$userName$USER_SETTINGS_PAGE_SUFFIX"

            return pageEditClient
                .getCurrentWikiText(pageTitle)
                .flatMap { wikiText ->
                    if (wikiText.isNullOrEmpty()) {
                        Single.error(IllegalStateException("No settings backup found on user page."))
                    } else {
                        Single.fromCallable {
                            val jsonObject = JsonParser.parseString(wikiText).asJsonObject
                            val userSettingsData = gson.fromJson(jsonObject, UserSettingsData::class.java)

                            val prefsElement = jsonObject.getAsJsonObject("preferences")
                            val parsedPrefs = mutableMapOf<String, PreferenceValueDto>()

                            if (prefsElement != null) {
                                for ((key, element) in prefsElement.entrySet()) {
                                    if (element.isJsonObject) {
                                        val dto = gson.fromJson(element, PreferenceValueDto::class.java)
                                        parsedPrefs[key] = dto
                                    } else if (element.isJsonPrimitive) {
                                        val strVal = element.asString
                                        val type =
                                            if (KNOWN_BOOLEAN_KEYS.contains(key)) PrefType.BOOLEAN else PrefType.STRING
                                        parsedPrefs[key] = PreferenceValueDto(type, strVal)
                                    }
                                }
                            }

                            applyPreferences(parsedPrefs)
                            applyBookmarks(userSettingsData.bookmarks)
                            true
                        }
                    }
                }.subscribeOn(Schedulers.io())
        }

        private fun extractPreferences(): Map<String, PreferenceValueDto> {
            val rawPrefs = defaultKvStore.all ?: emptyMap<String, Any?>()
            val filteredPrefs = mutableMapOf<String, PreferenceValueDto>()

            for ((key, value) in rawPrefs) {
                if (key in SYNCABLE_PREFERENCE_KEYS && value != null) {
                    val prefDto =
                        when (value) {
                            is Boolean -> PreferenceValueDto(PrefType.BOOLEAN, value.toString())
                            is Int -> PreferenceValueDto(PrefType.INT, value.toString())
                            is Long -> PreferenceValueDto(PrefType.LONG, value.toString())
                            is Collection<*> -> PreferenceValueDto(PrefType.STRING_SET, gson.toJson(value))
                            else -> PreferenceValueDto(PrefType.STRING, value.toString())
                        }
                    filteredPrefs[key] = prefDto
                }
            }
            return filteredPrefs
        }

        private fun collectBookmarks(): BookmarksData =
            runBlocking {
                val locations =
                    bookmarkLocationsDao.getAllBookmarksLocations().map { loc ->
                        BookmarkLocationDto(
                            name = loc.locationName,
                            language = loc.locationLanguage,
                            description = loc.locationDescription,
                            lat = loc.locationLat,
                            long = loc.locationLong,
                            category = loc.locationCategory,
                            labelText = loc.locationLabelText,
                            labelIcon = loc.locationLabelIcon,
                            imageUrl = loc.locationImageUrl,
                            wikipediaLink = loc.locationWikipediaLink,
                            wikidataLink = loc.locationWikidataLink,
                            commonsLink = loc.locationCommonsLink,
                            pic = loc.locationPic,
                            exists = loc.locationExists,
                        )
                    }

                val categories =
                    bookmarkCategoriesDao
                        .getAllCategories()
                        .firstOrNull()
                        ?.map { it.categoryName }
                        ?: emptyList()

                val items =
                    bookmarkItemsDao.getAllBookmarksItems().map { item ->
                        BookmarkItemDto(
                            id = item.id,
                            name = item.name,
                            description = item.description,
                            imageUrl = item.imageUrl,
                            instanceOfs = item.instanceOfs,
                            isSelected = item.isSelected,
                        )
                    }

                val pictures =
                    bookmarkPicturesDao.getAllBookmarks().map { pic ->
                        BookmarkPictureDto(
                            mediaName = pic.mediaName,
                            mediaCreator = pic.mediaCreator,
                        )
                    }

                BookmarksData(
                    locations = locations,
                    categories = categories,
                    items = items,
                    pictures = pictures,
                )
            }

        @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
        private fun applyPreferences(preferences: Map<String, PreferenceValueDto>) {
            for ((key, prefDto) in preferences) {
                if (key in SYNCABLE_PREFERENCE_KEYS) {
                    try {
                        defaultKvStore.remove(key)
                        when (prefDto.type) {
                            PrefType.BOOLEAN -> defaultKvStore.putBoolean(key, prefDto.value.toBoolean())
                            PrefType.INT -> defaultKvStore.putInt(key, prefDto.value.toInt())
                            PrefType.LONG -> defaultKvStore.putLong(key, prefDto.value.toLong())
                            PrefType.STRING_SET -> {
                                val set = gson.fromJson(prefDto.value, Array<String>::class.java)?.toSet()
                                if (set != null) {
                                    defaultKvStore.putStringSet(key, set)
                                }
                            }
                            PrefType.STRING -> defaultKvStore.putString(key, prefDto.value)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to apply preference for key: %s", key)
                    }
                }
            }
        }

        private fun applyBookmarks(bookmarks: BookmarksData) {
            runBlocking {
                // Restore Location Bookmarks
                for (loc in bookmarks.locations) {
                    if (!bookmarkLocationsDao.findBookmarkLocation(loc.name)) {
                        bookmarkLocationsDao.addBookmarkLocation(
                            BookmarksLocations(
                                locationName = loc.name,
                                locationLanguage = loc.language,
                                locationDescription = loc.description,
                                locationLat = loc.lat,
                                locationLong = loc.long,
                                locationCategory = loc.category,
                                locationLabelText = loc.labelText,
                                locationLabelIcon = loc.labelIcon,
                                locationImageUrl = loc.imageUrl,
                                locationWikipediaLink = loc.wikipediaLink,
                                locationWikidataLink = loc.wikidataLink,
                                locationCommonsLink = loc.commonsLink,
                                locationPic = loc.pic,
                                locationExists = loc.exists,
                            ),
                        )
                    }
                }

                // Restore Category Bookmarks
                for (catName in bookmarks.categories) {
                    if (!bookmarkCategoriesDao.doesExist(catName)) {
                        bookmarkCategoriesDao.insert(BookmarksCategoryModal(catName))
                    }
                }

                // Restore Item Bookmarks
                for (itemDto in bookmarks.items) {
                    if (!bookmarkItemsDao.findBookmarkItem(itemDto.id)) {
                        val depictedItem =
                            DepictedItem(
                                name = itemDto.name,
                                description = itemDto.description,
                                imageUrl = itemDto.imageUrl,
                                instanceOfs = itemDto.instanceOfs,
                                commonsCategories = emptyList(),
                                isSelected = itemDto.isSelected,
                                id = itemDto.id,
                            )
                        bookmarkItemsDao.updateBookmarkItem(depictedItem)
                    }
                }

                // Restore Picture Bookmarks
                for (picDto in bookmarks.pictures) {
                    val bookmark =
                        Bookmark(
                            picDto.mediaName,
                            picDto.mediaCreator,
                            BookmarkPicturesContentProvider.uriForName(picDto.mediaName),
                        )
                    if (!bookmarkPicturesDao.findBookmark(bookmark)) {
                        bookmarkPicturesDao.updateBookmark(bookmark)
                    }
                }
            }
        }
    }
