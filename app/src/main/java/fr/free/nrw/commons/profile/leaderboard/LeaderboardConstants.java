package fr.free.nrw.commons.profile.leaderboard;

/**
 * This class contains the constant variables for leaderboard
 */
public class LeaderboardConstants {

    /**
     * This is the size of the page i.e. number items to load in a batch when pagination is performed
     */
    public static final int PAGE_SIZE = 100;

    /**
     * This is the starting offset, we set it to 0 to start loading from rank 1
     */
    public static final int START_OFFSET = 0;

    /**
     * This is the prefix of the user's homepage url, appending the username will give us complete url
     */
    public static final String USER_LINK_PREFIX = "https://commons.wikimedia.org/wiki/User:";

    /**
     * This is the a constant string for the state loading, when the pages are getting loaded we can
     * use this constant to identify if we need to show the progress bar or not
     */
    public final static String LOADING = "Loading";

    /**
     * This is the a constant string for the state loaded, when the pages are loaded we can
     * use this constant to identify if we need to show the progress bar or not
     */
    public final static String LOADED = "Loaded";

    /**
     * This API endpoint is to update the leaderboard avatar
     */
    public final static String UPDATE_AVATAR_END_POINT = "/update_avatar.py";

    /**
     * This API endpoint is to get leaderboard data
     */
    public final static String LEADERBOARD_END_POINT = "/leaderboard.py";

}
