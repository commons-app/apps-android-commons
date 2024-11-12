package fr.free.nrw.commons

import android.os.Parcelable
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.wikidata.model.page.PageTitle
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Locale
import java.util.UUID

@Parcelize
class Media constructor(
    /**
     * @return pageId for the current media object
     * Wikibase Identifier associated with media files
     */
    var pageId: String = UUID.randomUUID().toString(),
    var thumbUrl: String? = null,
    /**
     * Gets image URL
     * @return Image URL
     */
    var imageUrl: String? = null,
    /**
     * Gets the name of the file.
     * @return file name as a string
     */
    var filename: String? = null,
    /**
     * Gets or sets the file description.
     * @return file description as a string
     * @param fallbackDescription the new description of the file
     */
    var fallbackDescription: String? = null,
    /**
     * Gets the upload date of the file.
     * Can be null.
     * @return upload date as a Date
     */
    var dateUploaded: Date? = null,
    /**
     * Gets or sets the license name of the file.
     * @return license as a String
     * @param license license name as a String
     */
    var license: String? = null,
    var licenseUrl: String? = null,
    /**
     * Gets or sets the name of the creator of the file.
     * @return author name as a String
     * @param author creator name as a string
     */
    var author: String? = null,
    var user: String? = null,
    /**
     * Gets the categories the file falls under.
     * @return file categories as an ArrayList of Strings
     */
    var categories: List<String>? = null,
    /**
     * Gets the coordinates of where the file was created.
     * @return file coordinates as a LatLng
     */
    var coordinates: LatLng? = null,
    var captions: Map<String, String> = emptyMap(),
    var descriptions: Map<String, String> = emptyMap(),
    var depictionIds: List<String> = emptyList(),
    /**
     * This field was added to find non-hidden categories
     * Stores the mapping of category title to hidden attribute
     * Example: "Mountains" => false, "CC-BY-SA-2.0" => true
     */
    var categoriesHiddenStatus: Map<String, Boolean> = emptyMap(),
) : Parcelable {
    constructor(
        captions: Map<String, String>,
        categories: List<String>?,
        filename: String?,
        fallbackDescription: String?,
        author: String?,
        user: String?,
    ) : this(
        filename = filename,
        fallbackDescription = fallbackDescription,
        dateUploaded = Date(),
        author = author,
        user = user,
        categories = categories,
        captions = captions,
    )

    /**
     * Gets media display title
     * @return Media title
     */
    val displayTitle: String
        get() =
            if (filename != null) {
                pageTitle.displayTextWithoutNamespace.replaceFirst("[.][^.]+$".toRegex(), "")
            } else {
                ""
            }

    /**
     * Gets file page title
     * @return New media page title
     */
    val pageTitle: PageTitle get() = Utils.getPageTitle(filename!!)

    /**
     * Returns wikicode to use the media file on a MediaWiki site
     * @return
     */
    val wikiCode: String
        get() = String.format("[[%s|thumb|%s]]", filename, mostRelevantCaption)

    val mostRelevantCaption: String
        get() =
            captions[Locale.getDefault().language]
                ?: captions.values.firstOrNull()
                ?: displayTitle

    /**
     * Gets the categories the file falls under.
     * @return file categories as an ArrayList of Strings
     */
    @IgnoredOnParcel
    var addedCategories: List<String>? = null
        // TODO added categories should be removed. It is added for a short fix. On category update,
        //  categories should be re-fetched instead
        get() = field // getter
        set(value) {
            field = value
        } // setter
}
