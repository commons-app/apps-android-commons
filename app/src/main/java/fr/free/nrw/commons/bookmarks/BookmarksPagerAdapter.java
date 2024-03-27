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
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment;

public class BookmarksPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<BookmarkPages> pages;

    /**
     * Default Constructor
     * @param fm
     * @param context
     * @param onlyPictures is true if the fragment requires only BookmarkPictureFragment
     *                     (i.e. when no user is logged in).
     */
    BookmarksPagerAdapter(FragmentManager fm, Context context,boolean onlyPictures) {
        super(fm);
        pages = new ArrayList<>();
        Bundle picturesBundle = new Bundle();
        picturesBundle.putString("categoryName", context.getString(R.string.title_page_bookmarks_pictures));
        picturesBundle.putInt("order", 0);
        pages.add(new BookmarkPages(
                new BookmarkListRootFragment(picturesBundle, this),
                context.getString(R.string.title_page_bookmarks_pictures)));
        if (!onlyPictures) {
            // if onlyPictures is false we also add the location fragment.
            Bundle locationBundle = new Bundle();
            locationBundle.putString("categoryName",
                context.getString(R.string.title_page_bookmarks_locations));
            locationBundle.putInt("order", 1);
            pages.add(new BookmarkPages(
                new BookmarkListRootFragment(locationBundle, this),
                context.getString(R.string.title_page_bookmarks_locations)));

            locationBundle.putInt("orderItem", 2);
            pages.add(new BookmarkPages(
                new BookmarkListRootFragment(locationBundle, this),
                context.getString(R.string.title_page_bookmarks_items)));
        }
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
        BookmarkPicturesFragment fragment = (BookmarkPicturesFragment)(((BookmarkListRootFragment)pages.get(0).getPage()).listFragment);
        return fragment.getAdapter();
    }

    /**
     * Update the pictures list for the bookmark fragment
     */
    public void requestPictureListUpdate() {
        BookmarkPicturesFragment fragment = (BookmarkPicturesFragment)(((BookmarkListRootFragment)pages.get(0).getPage()).listFragment);
        fragment.onResume();
    }
}
