package fr.free.nrw.commons.wikidata.model

class EnumCodeMap<T>(
    enumeration: Class<T>,
) where T : Enum<T>, T : EnumCode {
    private val map: HashMap<Int, T> =
        enumeration.enumConstants.associateByTo(HashMap()) { it.code() }

    operator fun get(code: Int): T = map[code] ?: throw IllegalArgumentException("code=$code")

    fun size(): Int = map.size
}
