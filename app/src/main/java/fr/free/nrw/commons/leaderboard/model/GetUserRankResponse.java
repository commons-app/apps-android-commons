package fr.free.nrw.commons.leaderboard.model;

import com.google.gson.annotations.SerializedName;

public class GetUserRankResponse {

    @SerializedName("userrank")
    private final int userRank;

    public GetUserRankResponse(int userRank) {
        this.userRank = userRank;
    }
    public int getUserRank() {
        return userRank;
    }
}
