package fr.free.nrw.commons.profile.leaderboard

import androidx.recyclerview.widget.DiffUtil
import com.google.gson.annotations.SerializedName

/**
 * This class represents the leaderboard API response sub part of i.e. leaderboard list
 * The leaderboard list will contain the ranking of the users from 1 to n,
 * avatars, username and count in the selected category.
 */
data class LeaderboardList (
    @SerializedName("username")
    var username: String? = null,
    @SerializedName("category_count")
    var categoryCount: Int? = null,
    @SerializedName("avatar")
    var avatar: String? = null,
    @SerializedName("rank")
    var rank: Int? = null
) {

    /**
     * Returns true if two objects are equal, false otherwise
     * @param other
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        val leaderboardList = other as LeaderboardList
        return leaderboardList.rank == rank
    }

    override fun hashCode(): Int {
        var result = username?.hashCode() ?: 0
        result = 31 * result + (categoryCount ?: 0)
        result = 31 * result + (avatar?.hashCode() ?: 0)
        result = 31 * result + (rank ?: 0)
        return result
    }

    companion object {
        /**
         * This method checks for the diff in the callbacks for paged lists
         */
        var DIFF_CALLBACK: DiffUtil.ItemCallback<LeaderboardList> =
            object : DiffUtil.ItemCallback<LeaderboardList>() {
                override fun areItemsTheSame(
                    oldItem: LeaderboardList,
                    newItem: LeaderboardList
                ): Boolean = newItem === oldItem

                override fun areContentsTheSame(
                    oldItem: LeaderboardList,
                    newItem: LeaderboardList
                ): Boolean = newItem.rank == oldItem.rank
            }
    }
}