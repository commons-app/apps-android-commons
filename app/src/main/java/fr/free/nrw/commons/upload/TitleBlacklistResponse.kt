package fr.free.nrw.commons.upload

data class TitleBlacklistResponse(
    val titleblacklist: TitleBlacklistResult?,
)

data class TitleBlacklistResult(
    val result: String?,
)
