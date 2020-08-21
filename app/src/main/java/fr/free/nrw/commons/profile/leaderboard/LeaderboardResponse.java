package fr.free.nrw.commons.profile.leaderboard;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * GSON Response Class for Leaderboard API response
 */
public class LeaderboardResponse {

    @SerializedName("status")
    @Expose
    private Integer status;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("category_count")
    @Expose
    private Integer categoryCount;

    @SerializedName("limit")
    @Expose
    private int limit;

    @SerializedName("avatar")
    @Expose
    private String avatar;

    @SerializedName("offset")
    @Expose
    private int offset;

    @SerializedName("duration")
    @Expose
    private String duration;

    @SerializedName("leaderboard_list")
    @Expose
    private List<LeaderboardList> leaderboardList = null;

    @SerializedName("category")
    @Expose
    private String category;

    @SerializedName("rank")
    @Expose
    private Integer rank;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public List<LeaderboardList> getLeaderboardList() {
        return leaderboardList;
    }

    public void setLeaderboardList(List<LeaderboardList> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

}