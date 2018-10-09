package fr.free.nrw.commons.bookmarks;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class BookmarksPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<BookmarkPages> pages;

    BookmarksPagerAdapter(FragmentManager fm) {
        super(fm);
        pages = new ArrayList<>();
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

    void updatePages(List<BookmarkPages> newPages) {
        pages.clear();
        pages.addAll(newPages);
        notifyDataSetChanged();
    }
}