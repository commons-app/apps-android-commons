package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.BookmarksActivity;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class LeaderboardActivity extends NavigationBaseActivity {

    /**
     * This method helps in the creation Leaderboard screen
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        ButterKnife.bind(this);

        initDrawer();
    }
}
