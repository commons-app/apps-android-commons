package fr.free.nrw.commons.bookmarks;

import android.net.Uri;

import fr.free.nrw.commons.BuildConfig;

public class BookmarkContentProvider {

    private static final String BASE_PATH = "bookmarks";
    public static final Uri BASE_URI = Uri.parse("content://" + BuildConfig.CONTRIBUTION_AUTHORITY + "/" + BASE_PATH);

    public static Uri uriForName(String name) {
        return Uri.parse(BASE_URI.toString() + "/" + name);
    }

}
