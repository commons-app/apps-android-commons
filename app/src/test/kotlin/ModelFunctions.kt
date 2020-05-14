import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

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
