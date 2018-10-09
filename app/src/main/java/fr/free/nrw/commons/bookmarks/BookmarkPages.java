package fr.free.nrw.commons.bookmarks;

import android.support.v4.app.Fragment;

public class BookmarkPages {
    private Fragment page;
    private String title;

    BookmarkPages(Fragment fragment, String title) {
        this.title = title;
        this.page = fragment;
    }

    public Fragment getPage() {
        return page;
    }

    public String getTitle() {
        return title;
    }
}
