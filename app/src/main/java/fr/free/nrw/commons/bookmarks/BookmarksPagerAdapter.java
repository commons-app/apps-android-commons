package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.os.Bundle;
import android.widget.ListAdapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment;

public class BookmarksPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<BookmarkPages> pages;

    BookmarksPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pages = new ArrayList<>();
        Bundle picturesBundle = new Bundle();
        picturesBundle.putString("categoryName", context.getString(R.string.title_page_bookmarks_pictures));
        picturesBundle.putInt("order", 0);
        pages.add(new BookmarkPages(
                new BookmarkListRootFragment(picturesBundle),
                context.getString(R.string.title_page_bookmarks_pictures)));

        Bundle locationBundle = new Bundle();
        locationBundle.putString("categoryName", context.getString(R.string.title_page_bookmarks_locations));
        locationBundle.putInt("order", 1);
        pages.add(new BookmarkPages(
            new BookmarkListRootFragment(locationBundle),
            context.getString(R.string.title_page_bookmarks_locations)));
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

    /**
     * Return the Adapter used to display the picture gridview
     * @return adapter
     */
    public ListAdapter getMediaAdapter() {
        BookmarkPicturesFragment fragment = (BookmarkPicturesFragment)(pages.get(0).getPage());
        return fragment.getAdapter();
    }

    /**
     * Update the pictures list for the bookmark fragment
     */
    public void requestPictureListUpdate() {
        BookmarkPicturesFragment fragment = (BookmarkPicturesFragment)(pages.get(0).getPage());
        fragment.onResume();
    }
}
