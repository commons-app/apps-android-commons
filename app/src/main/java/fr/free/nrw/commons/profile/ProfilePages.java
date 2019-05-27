package fr.free.nrw.commons.profile;

import androidx.fragment.app.Fragment;

public class ProfilePages {

    private Fragment page;
    private String title;

    ProfilePages(Fragment fragment, String title) {
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