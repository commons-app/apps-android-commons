package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationListFragment;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPictureListFragment;

public class BookmarksPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<BookmarkPages> pages;

    BookmarksPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pages = new ArrayList<>();
        pages.add(new BookmarkPages(
                BookmarkPictureListFragment.newInstance(),
                context.getString(R.string.title_page_bookmarks_pictures)
        ));
        pages.add(new BookmarkPages(
                BookmarkLocationListFragment.newInstance(),
                context.getString(R.string.title_page_bookmarks_locations)
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

    public ListAdapter getMediaAdapter() {
        BookmarkPictureListFragment fragment = (BookmarkPictureListFragment)(pages.get(0).getPage());
        return fragment.getAdapter();
    }
}