package fr.free.nrw.commons.profile.leaderboard;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LeaderboardList {

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("category_count")
    @Expose
    private Integer categoryCount;

    @SerializedName("avatar")
    @Expose
    private String avatar;

    @SerializedName("rank")
    @Expose
    private Integer rank;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(Integer categoryCount) {
        this.categoryCount = categoryCount;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }


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
                return newItem.getRank().equals(oldItem.getRank()) || newItem.getCategoryCount().equals(oldItem.getCategoryCount());
            }
        };

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        LeaderboardList leaderboardList = (LeaderboardList) obj;
        return leaderboardList.getRank().equals(this.getRank()) || leaderboardList.getCategoryCount().equals(this.getCategoryCount());
    }
}