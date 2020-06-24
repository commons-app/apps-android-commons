import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.model.DepictSearchItem
import java.util.*

fun depictedItem(
    name: String = "label",
    description: String = "desc",
    imageUrl: String = "",
    instanceOfs: List<String> = listOf(),
    commonsCategories: List<String> = listOf(),
    isSelected: Boolean = false,
    id: String = "entityId"
) = DepictedItem(
    name = name,
    description = description,
    imageUrl = imageUrl,
    instanceOfs = instanceOfs,
    commonsCategories = commonsCategories,
    isSelected = isSelected,
    id = id
)

fun categoryItem(name: String = "name", selected: Boolean = false) =
    CategoryItem(name, selected)

fun media(
    thumbUrl: String? = "thumbUrl",
    imageUrl: String? = "imageUrl",
    filename: String? = "filename",
    fallbackDescription: String? = "fallbackDescription",
    dateUploaded: Date? = Date(),
    license: String? = "license",
    licenseUrl: String? = "licenseUrl",
    creator: String? = "creator",
    pageId: String = "pageId",
    categories: List<String>? = listOf("categories"),
    coordinates: LatLng? = LatLng(0.0, 0.0, 0.0f),
    captions: Map<String?, String?> = mapOf("en" to "caption"),
    descriptions: Map<String?, String?> = mapOf("en" to "description"),
    depictionIds: List<String> = listOf("depictionId")
) = Media(
    thumbUrl,
    imageUrl,
    filename,
    fallbackDescription,
    dateUploaded,
    license,
    licenseUrl,
    creator,
    pageId,
    categories,
    coordinates,
    captions,
    descriptions,
    depictionIds
)


fun depictSearchItem(
    id: String = "id",
    pageId: String = "pageid",
    url: String = "url",
    label: String = "label",
    description: String = "description"
) = DepictSearchItem(id, pageId, url, label, description)
