package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.wikidata.model.DepictSearchItem

/**
 * Model class for Depicted Item in Upload and Explore
 */
data class DepictedItem constructor(
  val depictsLabel: String?,
  val description: String?,
  var imageUrl: String,
  var isSelected: Boolean,
  val entityId: String
) {
  constructor(depictSearchItem: DepictSearchItem) : this(
    depictSearchItem.label,
    depictSearchItem.description,
    "",
    false,
    depictSearchItem.id
  )

  var position = 0

  override fun equals(o: Any?) = when {
    this === o -> true
    o is DepictedItem -> depictsLabel == o.depictsLabel
    else -> false
  }

  override fun hashCode(): Int {
    return depictsLabel?.hashCode() ?: 0
  }

}
