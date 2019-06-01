package fr.free.nrw.commons.leaderboard;

import androidx.fragment.app.Fragment;

public class LeaderboardPages {
    private Fragment page;
    private String title;

    LeaderboardPages(Fragment fragment, String title) {
        this.title = title;
        this.page = fragment;
    }

    /**
     * Return the fragment
     * @return fragment object
     */
    public Fragment getPage() {
        return page;
    }

    /**
     * Return the fragment title
     * @return title
     */
    public String getTitle() {
        return title;
    }
}
