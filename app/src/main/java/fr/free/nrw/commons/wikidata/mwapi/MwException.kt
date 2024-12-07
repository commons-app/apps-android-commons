package fr.free.nrw.commons.wikidata.mwapi

class MwException(
    val error: MwServiceError?,
    private val errors: List<MwServiceError>?
) : RuntimeException() {
    val errorCode: String?
        get() = error?.code ?: errors?.get(0)?.code

    val title: String?
        get() = error?.title ?: errors?.get(0)?.title

    override val message: String?
        get() = error?.details ?: errors?.get(0)?.details
}
