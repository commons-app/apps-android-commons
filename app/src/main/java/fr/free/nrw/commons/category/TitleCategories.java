package fr.free.nrw.commons.category;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;


public class TitleCategories extends AsyncTask<Void, Void, ArrayList<String>> {

    private final static int SEARCH_CATS_LIMIT = 25;
    private static final String TAG = TitleCategories.class.getName();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {


        MWApi api = CommonsApplication.createMWApi();
        ApiResult result;

        //URL https://commons.wikimedia.org/w/api.php?action=query&format=xml&list=search&srwhat=text&srenablerewrites=1&srnamespace=14&srlimit=10&srsearch=
        try {
            result = api.action("query")
                    .param("format", "xml")
                    .param("list", "search")
                    .param("srwhat", "text")
                    .param("srnamespace", "14")
                    .param("srlimit", SEARCH_CATS_LIMIT)
                    .param("srsearch", title)
                    .get();
            Log.d(TAG, "Searching for cats for title: " + result.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: ", e);
            //Return empty arraylist
            return items;
        }

        ArrayList<ApiResult> categoryNodes = result.getNodes("/api/query/search/p/@title");
        for(ApiResult categoryNode: categoryNodes) {
            String cat = categoryNode.getDocument().getTextContent();
            String catString = cat.replace("Category:", "");
            items.add(catString);
        }

        Log.d(TAG, "Title cat query results: " + items);

        return items;
    }

}
