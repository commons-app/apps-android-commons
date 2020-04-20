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

data class SubclassSparqlResponse(val results: SubclassResult) {
    fun toDepictedItems() =
        results.bindings.map {
            DepictedItem(
                it.subclassLabel.value,
                it.subclassDescription?.value ?: "",
                "",
                false,
                it.subclass.value.substringAfterLast("/")
            )
        }
}

data class ParentResult(val bindings: List<ParentBinding>)

data class SubclassResult(val bindings: List<SubclassBinding>)

data class ParentBinding(
    val parentClass: SparqInfo,
    val parentClassLabel: SparqInfo,
    val parentClassDescription: SparqInfo? = null
)

data class SubclassBinding(
    val subclass: SparqInfo,
    val subclassLabel: SparqInfo,
    val subclassDescription: SparqInfo? = null
)

data class SparqInfo(val type: String, val value: String)
