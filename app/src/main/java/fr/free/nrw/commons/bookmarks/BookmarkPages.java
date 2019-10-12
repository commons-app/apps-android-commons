package fr.free.nrw.commons.bookmarks;

import androidx.fragment.app.Fragment;

/**
 * Data class for handling a bookmark fragment and it title
 */
class BookmarkPages {
    private final Fragment page;
    private final String title;

    BookmarkPages(Fragment fragment, String title) {
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
