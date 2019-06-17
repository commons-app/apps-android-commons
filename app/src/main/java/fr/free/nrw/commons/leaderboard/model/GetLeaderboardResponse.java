package fr.free.nrw.commons.leaderboard.model;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.achievements.FeaturedImages;

public class GetLeaderboardResponse {

    private final int rank;
    private final String username;
    private final Media avatar;
    private final int score;

    public GetLeaderboardResponse(int rank,
                                  String username,
                                  Media avatar,
                                  int score) {
        this.rank = rank;
        this.username = username;
        this.avatar = avatar;
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public Media getAvatar() {
        return avatar;
    }

    public int getScore() {
        return score;
    }
}
