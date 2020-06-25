package fr.free.nrw.commons.mwapi

data class SparqlResponse(val results: Result)

data class Result(val bindings: List<Binding>)

data class Binding(
    val item: SparqInfo
) {
    val id: String
        get() = item.value.substringAfterLast("/")
}

data class SparqInfo(val type: String, val value: String)
