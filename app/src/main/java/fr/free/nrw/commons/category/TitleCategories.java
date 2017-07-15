package fr.free.nrw.commons.category;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to retrieve categories that are related to
 * the title entered in previous screen. The 'srsearch' action-specific parameter is used for this
 * purpose. This class should be subclassed in CategorizationFragment.java to add the results to recent and GPS cats.
 */
class TitleCategories extends AsyncTask<Void, Void, List<String>> {

    private final static int SEARCH_CATS_LIMIT = 25;

    private String title;

    TitleCategories(String title) {
        this.title = title;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<String> doInBackground(Void... voids) {

        MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
        List<String> titleCategories = new ArrayList<>();

        //URL https://commons.wikimedia.org/w/api.php?action=query&format=xml&list=search&srwhat=text&srenablerewrites=1&srnamespace=14&srlimit=10&srsearch=
        try {
            titleCategories = api.searchTitles(SEARCH_CATS_LIMIT, this.title);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            //Return empty arraylist
            return titleCategories;
        }

        Timber.d("Title cat query results: %s", titleCategories);

        return titleCategories;
    }

}
