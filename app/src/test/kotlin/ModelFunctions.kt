import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

fun depictedItem(
    name: String = "label",
    description: String = "desc",
    imageUrl: String = "",
    isSelected: Boolean = false,
    id: String = "entityId"
) = DepictedItem(name, description, imageUrl, isSelected, id)

fun categoryItem(name: String = "name", selected: Boolean = false) =
    CategoryItem(name, selected)
