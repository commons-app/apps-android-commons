import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.model.DepictSearchItem
import fr.free.nrw.commons.wikidata.model.*
import java.util.*

fun depictedItem(
    name: String = "label",
    description: String = "desc",
    imageUrl: String = "",
    instanceOfs: List<String> = listOf(),
    commonsCategories: List<CategoryItem> = listOf(),
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

fun categoryItem(name: String = "name", description: String = "desc",
                 thumbUrl: String = "thumbUrl", selected: Boolean = false) =
    CategoryItem(name, description, thumbUrl, selected)

fun media(
    thumbUrl: String? = "thumbUrl",
    imageUrl: String? = "imageUrl",
    filename: String? = "filename",
    fallbackDescription: String? = "fallbackDescription",
    dateUploaded: Date? = Date(),
    license: String? = "license",
    licenseUrl: String? = "licenseUrl",
    author: String? = "creator",
    user:String?="user",
    pageId: String = "pageId",
    categories: List<String>? = listOf("categories"),
    coordinates: LatLng? = LatLng(0.0, 0.0, 0.0f),
    captions: Map<String, String> = mapOf("en" to "caption"),
    descriptions: Map<String, String> = mapOf("en" to "description"),
    depictionIds: List<String> = listOf("depictionId")
) = Media(
    pageId,
    thumbUrl,
    imageUrl,
    filename,
    fallbackDescription,
    dateUploaded,
    license,
    licenseUrl,
    author,
    user,
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

fun place(
    name: String = "name",
    lang: String = "en",
    label: Label? = null,
    longDescription: String = "longDescription",
    latLng: LatLng? = null,
    category: String = "category",
    siteLinks: Sitelinks? = null,
    pic: String = "pic",
    exists: Boolean = false,
    entityID: String = "entityID"
): Place {
    return Place(lang, name, label, longDescription, latLng, category, siteLinks, pic, exists, entityID)
}

fun entityId(wikiBaseEntityValue: WikiBaseEntityValue = wikiBaseEntityValue()) =
    DataValue.EntityId(wikiBaseEntityValue)

fun wikiBaseEntityValue(
    entityType: String = "type",
    id: String = "id",
    numericId: Long = 0
) = WikiBaseEntityValue(entityType, id, numericId)

fun statement(
    mainSnak: Snak_partial = snak(),
    rank: String = "rank",
    type: String = "type"
) = Statement_partial(mainSnak, type, rank)

fun snak(
    snakType: String = "type",
    property: String = "property",
    dataValue: DataValue = valueString("")
) = Snak_partial(snakType, property, dataValue)

fun valueString(value: String) = DataValue.ValueString(value)

fun entity(
    labels: Map<String, String> = emptyMap(),
    descriptions: Map<String, String> = emptyMap(),
    statements: Map<String, List<Statement_partial>>? = emptyMap(),
    id: String = "id"
) = mock<Entities.Entity>().apply {
    val mockedLabels = labels.mockLabels()
    whenever(labels()).thenReturn(mockedLabels)
    val mockedDescriptions = descriptions.mockLabels()
    whenever(descriptions()).thenReturn(mockedDescriptions)
    whenever(this.statements).thenReturn(statements)
    whenever(id()).thenReturn(id)
}

private fun Map<String, String>.mockLabels(): Map<String, Entities.Label> {
    return mapValues { entry ->
        mock<Entities.Label>().also { whenever(it.value()).thenReturn(entry.value) }
    }
}
