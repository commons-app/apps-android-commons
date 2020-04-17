package fr.free.nrw.commons.depictions.subClass.models

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem


data class ParentSparqlResponse(val results: ParentResult) {
    fun toDepictedItems() =
        results.bindings
            .filter { it.parentClassDescription != null }
            .map {
                DepictedItem(
                    it.parentClassLabel.value,
                    it.parentClassDescription!!.value,
                    "",
                    false,
                    it.parentClass.value
                )
            }
}

data class ParentResult(val bindings: List<ParentBinding>)

data class ParentBinding(
    val parentClass: SparqInfo,
    val parentClassLabel: SparqInfo,
    val parentClassDescription: SparqInfo? = null
)

data class SparqInfo(val type: String, val value: String)
