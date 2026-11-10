package fr.free.nrw.commons.wikidata.model.gallery

/**
 * Describes a subtitle track available for a media file.
 */
class TimedTextTrack {
    private val src: String? = null
    private val srclang: String? = null
    private val label: String? = null

    fun src(): String = src!!

    fun language(): String = srclang!!

    fun label(): String = label!!
}
