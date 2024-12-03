package fr.free.nrw.commons.profile.leaderboard

/**
 * This class contains the constant variables for leaderboard
 */
object LeaderboardConstants {
    /**
     * This is the size of the page i.e. number items to load in a batch when pagination is performed
     */
    const val PAGE_SIZE: Int = 100

    /**
     * This is the starting offset, we set it to 0 to start loading from rank 1
     */
    const val START_OFFSET: Int = 0

    /**
     * This is the prefix of the user's homepage url, appending the username will give us complete url
     */
    const val USER_LINK_PREFIX: String = "https://commons.wikimedia.org/wiki/User:"

    sealed class LoadingStatus {
        /**
         * This is the state loading, when the pages are getting loaded we can
         * use this constant to identify if we need to show the progress bar or not
         */
        data object LOADING: LoadingStatus()
        /**
         * This is the state loaded, when the pages are loaded we can
         * use this constant to identify if we need to show the progress bar or not
         */
        data object LOADED: LoadingStatus()
    }

    /**
     * This API endpoint is to update the leaderboard avatar
     */
    const val UPDATE_AVATAR_END_POINT: String = "/update_avatar.py"

    /**
     * This API endpoint is to get leaderboard data
     */
    const val LEADERBOARD_END_POINT: String = "/leaderboard.py"
}
