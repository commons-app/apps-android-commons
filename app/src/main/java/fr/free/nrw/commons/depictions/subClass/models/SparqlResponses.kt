package fr.free.nrw.commons.depictions.subClass.models

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

data class SparqlResponse(val results: Result) {
    fun toDepictedItems() =
        results.bindings.map {
            DepictedItem(
                it.itemLabel.value,
                it.itemDescription?.value ?: "",
                "",
                false,
                it.item.value.substringAfterLast("/")
            )
        }
}

data class Result(val bindings: List<Binding>)

data class Binding(
    val item: SparqInfo,
    val itemLabel: SparqInfo,
    val itemDescription: SparqInfo? = null
)

data class SparqInfo(val type: String, val value: String)
