package fr.free.nrw.commons

import android.os.Parcelable
import fr.free.nrw.commons.location.LatLng
import kotlinx.android.parcel.Parcelize
import org.wikipedia.page.PageTitle
import java.util.*

@Parcelize
class Media constructor(
    /**
     * @return pageId for the current media object
     * Wikibase Identifier associated with media files
     */
    val pageId: String = UUID.randomUUID().toString(),
    val thumbUrl: String? = null,

    /**
     * Gets image URL
     * @return Image URL
     */
    val imageUrl: String? = null,
    /**
     * Gets the name of the file.
     * @return file name as a string
     */
    val filename: String? = null,
    /**
     * Gets the file description.
     * @return file description as a string
     */
    // monolingual description on input...
    /**
     * Sets the file description.
     * @param fallbackDescription the new description of the file
     */
    var fallbackDescription: String? = null,

    /**
     * Gets the upload date of the file.
     * Can be null.
     * @return upload date as a Date
     */
    val dateUploaded: Date? = null,
    /**
     * Gets the license name of the file.
     * @return license as a String
     */
    /**
     * Sets the license name of the file.
     *
     * @param license license name as a String
     */
    var license: String? = null,
    val licenseUrl: String? = null,
    /**
     * Gets the name of the creator of the file.
     * @return author name as a String
     */
    /**
     * Sets the author name of the file.
     * @param author creator name as a string
     */
    var author: String? = null,

    var user:String?=null,

    /**
     * Gets the categories the file falls under.
     * @return file categories as an ArrayList of Strings
     */
    val categories: List<String>? = null,
    /**
     * Gets the coordinates of where the file was created.
     * @return file coordinates as a LatLng
     */
    val coordinates: LatLng? = null,
    val captions: Map<String, String> = emptyMap(),
    val descriptions: Map<String, String> = emptyMap(),
    val depictionIds: List<String> = emptyList()
) : Parcelable {

    constructor(
        captions: Map<String, String>,
        categories: List<String>?,
        filename: String?,
        fallbackDescription: String?,
        author: String?, user:String?
    ) : this(
        filename = filename,
        fallbackDescription = fallbackDescription,
        dateUploaded = Date(),
        author = author,
        user=user,
        categories = categories,
        captions = captions
    )

    /**
     * Gets media display title
     * @return Media title
     */
    val displayTitle: String
        get() =
            if (filename != null)
                pageTitle.displayTextWithoutNamespace.replaceFirst("[.][^.]+$".toRegex(), "")
            else
                ""

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
        get() = captions[Locale.getDefault().language]
            ?: captions.values.firstOrNull()
            ?: displayTitle

    /**
     * Gets the categories the file falls under.
     * @return file categories as an ArrayList of Strings
     */
    var addedCategories: List<String>? = null
        // TODO added categories should be removed. It is added for a short fix. On category update,
        //  categories should be re-fetched instead
        get() = field                     // getter
        set(value) { field = value }      // setter
}
