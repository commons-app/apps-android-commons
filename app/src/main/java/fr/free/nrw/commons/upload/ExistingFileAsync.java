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
 * Returns true if file exists, false if it doesn't
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
            return false;
        }

        ArrayList<ApiResult> resultNodes = result.getNodes("/api/query/allimages/");

        Log.d(TAG, "Result nodes: " + resultNodes);

        boolean fileExists;
        if (resultNodes!=null) {
            fileExists = true;
        } else {
            fileExists = false;
        }
        
        //FIXME: This always returns false even when file (and nodes) exists, why?
        Log.d(TAG, "File already exists in Commons:" + fileExists);

        return fileExists;
    }
}