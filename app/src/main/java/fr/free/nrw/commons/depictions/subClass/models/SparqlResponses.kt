package fr.free.nrw.commons.depictions.subClass.models

data class SparqlResponse(val results: Result)

data class Result(val bindings: List<Binding>)

data class Binding(
    val item: SparqInfo,
    val itemLabel: SparqInfo,
    val itemDescription: SparqInfo? = null
) {
    val id: String
        get() = item.value.substringAfterLast("/")
}

data class SparqInfo(val type: String, val value: String)
