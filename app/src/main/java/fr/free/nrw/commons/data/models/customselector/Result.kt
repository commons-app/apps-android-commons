package fr.free.nrw.commons.data.models.customselector

/**
 * Custom selector data class Result.
 */
data class Result(
    /**
     * CallbackStatus : stores the result status
     */
    val status: CallbackStatus,

    /**
     * Images : images retrieved
     */
    val images: ArrayList<Image>) {
}