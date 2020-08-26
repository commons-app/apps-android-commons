package fr.free.nrw.commons.profile.leaderboard;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This class represents the leaderboard API response sub part of i.e. leaderboard list
 * The leaderboard list will contain the ranking of the users from 1 to n,
 * avatars, username and count in the selected category.
 */
public class LeaderboardList {

    /**
     * Username of the user
     * Example value - Syced
     */
    @SerializedName("username")
    @Expose
    private String username;

    /**
     * Count in the category
     * Example value - 10
     */
    @SerializedName("category_count")
    @Expose
    private Integer categoryCount;

    /**
     * URL of the avatar of user
     * Example value = https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Gnome-stock_person.svg/200px-Gnome-stock_person.svg.png
     */
    @SerializedName("avatar")
    @Expose
    private String avatar;

    /**
     * Rank of the user
     * Example value - 1
     */
    @SerializedName("rank")
    @Expose
    private Integer rank;

    /**
     * @return the username of the user in the leaderboard list
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user in the leaderboard list
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the category count of the user in the leaderboard list
     */
    public Integer getCategoryCount() {
        return categoryCount;
    }

    /**
     * Sets the category count of the user in the leaderboard list
     */
    public void setCategoryCount(Integer categoryCount) {
        this.categoryCount = categoryCount;
    }

    /**
     * @return the avatar of the user in the leaderboard list
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * Sets the avatar of the user in the leaderboard list
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * @return the rank of the user in the leaderboard list
     */
    public Integer getRank() {
        return rank;
    }

    /**
     * Sets the rank of the user in the leaderboard list
     */
    public void setRank(Integer rank) {
        this.rank = rank;
    }


    /**
     * This method checks for the diff in the callbacks for paged lists
     */
    public static DiffUtil.ItemCallback<LeaderboardList> DIFF_CALLBACK =
        new ItemCallback<LeaderboardList>() {
            @Override
            public boolean areItemsTheSame(@NonNull LeaderboardList oldItem,
                @NonNull LeaderboardList newItem) {
                return newItem == oldItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull LeaderboardList oldItem,
                @NonNull LeaderboardList newItem) {
                return newItem.getRank().equals(oldItem.getRank());
            }
        };

    /**
     * Returns true if two objects are equal, false otherwise
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        LeaderboardList leaderboardList = (LeaderboardList) obj;
        return leaderboardList.getRank().equals(this.getRank());
    }
}