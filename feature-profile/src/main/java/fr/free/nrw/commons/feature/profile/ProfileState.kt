package fr.free.nrw.commons.feature.profile

data class ProfileState(
    val userName: String = "",
    val userAvatarUrl: String = "",
    val userRank: Int = 0,
    val userContributions: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val leaderboard: List<LeaderboardUser> = emptyList()
)

data class Achievement(
    val iconUrl: String,
    val title: String,
    val description: String
)

data class LeaderboardUser(
    val rank: Int,
    val avatarUrl: String,
    val userName: String,
    val score: Int
)