package fr.free.nrw.commons.leaderboard;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

import fr.free.nrw.commons.R;

public class LeaderboardPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<LeaderboardPages> pages;

    LeaderboardPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pages = new ArrayList<>();
        pages.add(new LeaderboardPages(
                LeaderboardUploadFragment.newInstance(),
                context.getString(R.string.leaderboard_upload_title)
        ));
        pages.add(new LeaderboardPages(
                LeaderboardNearbyFragment.newInstance(),
                context.getString(R.string.leaderboard_nearby_title)
        ));
        pages.add(new LeaderboardPages(
                LeaderboardUsedFragment.newInstance(),
                context.getString(R.string.leaderboard_used_title)
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
