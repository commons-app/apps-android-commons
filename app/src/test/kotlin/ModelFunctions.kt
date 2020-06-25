import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.wikipedia.wikidata.*

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

fun place(
    name: String = "name",
    label: Label? = null,
    longDescription: String = "longDescription",
    latLng: LatLng? = null,
    category: String = "category",
    siteLinks: Sitelinks? = null,
    pic: String = "pic",
    destroyed: String = "destroyed"
): Place {
    return Place(name, label, longDescription, latLng, category, siteLinks, pic, destroyed)
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
