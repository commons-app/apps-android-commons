package fr.free.nrw.commons.category;

import android.os.AsyncTask;

import fr.free.nrw.commons.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.libs.mediawiki_api.ApiResult;
import timber.log.Timber;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to retrieve categories that are related to
 * the title entered in previous screen. The 'srsearch' action-specific parameter is used for this
 * purpose. This class should be subclassed in CategorizationFragment.java to add the results to recent and GPS cats.
 */
public class TitleCategories extends AsyncTask<Void, Void, ArrayList<String>> {

    private final static int SEARCH_CATS_LIMIT = 25;

    private String title;

    public TitleCategories(String title) {
        this.title = title;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {

        MWApi api = CommonsApplication.getInstance().getMWApi();
        ApiResult result;
        ArrayList<String> items = new ArrayList<>();

        //URL https://commons.wikimedia.org/w/api.php?action=query&format=xml&list=search&srwhat=text&srenablerewrites=1&srnamespace=14&srlimit=10&srsearch=
        try {
            result = api.action("query")
                    .param("format", "xml")
                    .param("list", "search")
                    .param("srwhat", "text")
                    .param("srnamespace", "14")
                    .param("srlimit", SEARCH_CATS_LIMIT)
                    .param("srsearch", title)
                    .prepareHttpRequestBuilder("GET")
                    .request();
            Timber.d("Searching for cats for title: %s", result);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            //Return empty arraylist
            return items;
        }

        ArrayList<ApiResult> categoryNodes = result.getNodes("/api/query/search/p/@title");
        for(ApiResult categoryNode: categoryNodes) {
            String cat = categoryNode.getDocument().getTextContent();
            String catString = cat.replace("Category:", "");
            items.add(catString);
        }

        Timber.d("Title cat query results: %s", items);

        return items;
    }

}
