package fr.free.nrw.commons.profile;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

import fr.free.nrw.commons.profile.achievements.ProfileAchievementsFragment;
import fr.free.nrw.commons.profile.leaderboard.ProfileLeaderboardFragment;

public class ProfilePagerAdapter extends FragmentPagerAdapter {

    private ArrayList<ProfilePages> pages;

    ProfilePagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pages = new ArrayList<>();
        pages.add(new ProfilePages(
                ProfileAchievementsFragment.newInstance(),
                "Achievements"
        ));
        pages.add(new ProfilePages(
                ProfileLeaderboardFragment.newInstance(),
                "Leaderboard"
        ));

        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        return pages.get(position).getPage();
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return pages.get(position).getTitle();
    }
}
