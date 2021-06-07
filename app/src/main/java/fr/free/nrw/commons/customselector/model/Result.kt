package fr.free.nrw.commons.customselector.model

data class Result(
    /**
     * CallbackStatus : stores the result status
     */
    val status:CallbackStatus,

    /**
     * Images : images retrieved
     */
    val images: ArrayList<Image>) {
}