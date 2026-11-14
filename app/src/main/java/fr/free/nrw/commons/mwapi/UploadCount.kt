package fr.free.nrw.commons.mwapi

/**
 * A user's number of uploads. [isCapped] means the user has more than [count] uploads,
 * because the Commons API fallback stops counting there.
 */
data class UploadCount(
    val count: Int,
    val isCapped: Boolean = false,
)
