package fr.free.nrw.commons.profile.leaderboard

import com.google.gson.annotations.SerializedName

/**
 * GSON Response Class for Leaderboard API response
 */
data class LeaderboardResponse(
    @SerializedName("status") var status: Int? = null,
    @SerializedName("username") var username: String? = null,
    @SerializedName("category_count") var categoryCount: Int? = null,
    @SerializedName("limit") var limit: Int = 0,
    @SerializedName("avatar") var avatar: String? = null,
    @SerializedName("offset") var offset: Int = 0,
    @SerializedName("duration") var duration: String? = null,
    @SerializedName("leaderboard_list") var leaderboardList: List<LeaderboardList>? = null,
    @SerializedName("category") var category: String? = null,
    @SerializedName("rank") var rank: Int? = null
)