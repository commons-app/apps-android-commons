package fr.free.nrw.commons.wikidata.mwapi

@Suppress("UnusedPrivateProperty") // Fields deserialized by Gson via reflection
class ListUserResponse {
    private val name: String? = null
    private val userid: Long = 0
    private val groups: List<String>? = null

    fun name(): String? = name

    fun getGroups(): Set<String> = groups?.toSet() ?: emptySet()
}
