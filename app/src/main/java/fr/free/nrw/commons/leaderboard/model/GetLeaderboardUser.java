package fr.free.nrw.commons.leaderboard.model;

import java.util.ArrayList;
import java.util.List;

public class GetLeaderboardUser {

    private String rank;
    private String avatar_uri;
    private String username;
    private String score;

    public GetLeaderboardUser() {
    }

    public GetLeaderboardUser(String rank,
                              String avatar_uri,
                              String username,
                              String score) {
        this.rank = rank;
        this.avatar_uri = avatar_uri;
        this.username = username;
        this.score = score;
    }

    public String getRank() { return rank; }

    public String getAvatar() {
        return avatar_uri;
    }

    public String getUsername() {
        return username;
    }

    public String getScore() {
        return score;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public void setAvatar_uri(String avatar_uri) {
        this.avatar_uri = avatar_uri;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setScore(String score) {
        this.score = score;
    }

    /**
     * Creating 10 dummy content for list.
     *
     * @param itemCount
     * @return
     */
    public static List<GetLeaderboardUser> createMovies(int itemCount) {
        List<GetLeaderboardUser> getLeaderboardUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int temp = itemCount == 0 ?
                    (itemCount + 1 + i) : (itemCount + i);
            GetLeaderboardUser getLeaderboardUser = new GetLeaderboardUser("R " + temp," ", "User " + temp,"S "+ temp);
            getLeaderboardUsers.add(getLeaderboardUser);
        }
        return getLeaderboardUsers;
    }
}
