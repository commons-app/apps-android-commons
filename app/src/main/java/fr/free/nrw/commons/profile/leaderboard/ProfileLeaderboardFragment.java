package fr.free.nrw.commons.profile.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;

public class ProfileLeaderboardFragment extends DaggerFragment {

    public static ProfileLeaderboardFragment newInstance() {
        return new ProfileLeaderboardFragment();
    }
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_profile_leaderboard, container, false);
        ButterKnife.bind(this, v);
        return v;
    }
}