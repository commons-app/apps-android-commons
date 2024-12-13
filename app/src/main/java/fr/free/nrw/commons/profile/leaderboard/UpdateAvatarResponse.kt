package fr.free.nrw.commons.profile.leaderboard

/**
 * GSON Response Class for Update Avatar API response
 */
data class UpdateAvatarResponse(
    var status: String? = null,
    var message: String? = null,
    var user: String? = null
)