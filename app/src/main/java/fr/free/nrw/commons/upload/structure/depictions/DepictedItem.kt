package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.WikidataItem
import fr.free.nrw.commons.wikidata.model.DepictSearchItem

/**
 * Model class for Depicted Item in Upload and Explore
 */
data class DepictedItem constructor(
    override val name: String,
    val description: String?,
    var imageUrl: String,
    var isSelected: Boolean,
    override val id: String
) : WikidataItem {
    constructor(depictSearchItem: DepictSearchItem) : this(
        depictSearchItem.label,
        depictSearchItem.description,
        "",
        false,
        depictSearchItem.id
    )

    constructor(place: Place) : this(
        place.name,
        place.longDescription,
        "",
        false,
        place.wikiDataEntityId!!
    )

    var position = 0

    override fun equals(o: Any?) = when {
        this === o -> true
        o is DepictedItem -> name == o.name
        else -> false
    }

    override fun hashCode(): Int {
        return name?.hashCode() ?: 0
    }

}
