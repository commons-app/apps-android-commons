package fr.free.nrw.commons.upload;


import android.os.AsyncTask;
import android.util.Log;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to check that file doesn't already exist
 */
public class ExistingFileAsync extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = fr.free.nrw.commons.upload.ExistingFileAsync.class.getName();
    private String fileSHA1;

    public ExistingFileAsync(String fileSHA1) {
        super();
        this.fileSHA1 = fileSHA1;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        MWApi api = CommonsApplication.createMWApi();
        ApiResult result;

        // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
        try {
            result = api.action("query")
                    .param("format", "xml")
                    .param("list", "allimages")
                    .param("aisha1", fileSHA1)
                    .get();
            Log.d(TAG, "Searching Commons API for existing file: " + result.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: ", e);
            //Return empty arraylist
            return false;
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