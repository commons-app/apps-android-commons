package fr.free.nrw.commons.profile.leaderboard;

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

}