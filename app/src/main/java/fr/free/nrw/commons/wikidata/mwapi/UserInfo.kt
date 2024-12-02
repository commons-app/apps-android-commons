package fr.free.nrw.commons.wikidata.mwapi

data class UserInfo(
    val name: String = "",
    val id: Int = 0,

    //Block information
    val blockid: Int = 0,
    val blockedby: String? = null,
    val blockedbyid: Int = 0,
    val blockreason: String? = null,
    val blocktimestamp: String? = null,
    val blockexpiry: String? = null,

    // Object type is any JSON type.
    val options: Map<String, *>? = null
) {
    fun id(): Int = id

    fun blockexpiry(): String = blockexpiry ?: ""
}
