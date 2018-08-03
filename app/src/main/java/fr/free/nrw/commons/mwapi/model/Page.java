package fr.free.nrw.commons.mwapi.model;

import android.support.annotation.NonNull;

public class Page {
    public String title;
    public PageCategory[] categories;
    public PageCategory category;

    public Page() {
    }

    @NonNull
    public PageCategory[] getCategories() {
        return categories != null ? categories : new PageCategory[0];
    }
}
