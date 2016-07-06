package fr.free.nrw.commons.category;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import javax.net.ssl.SSLPeerUnverifiedException;

import fr.free.nrw.commons.CommonsApplication;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to retrieve categories that share the
 * same prefix as the keyword typed in by the user. The 'acprefix' action-specific parameter is used
 * for this purpose. This class should be subclassed in CategorizationFragment.java to aggregate
 * the results.
 */
public class PrefixUpdater extends AsyncTask<Void, Void, ArrayList<String>> {

    private String filter;
    private static final String TAG = PrefixUpdater.class.getName();
    private CategorizationFragment catFragment;

    public PrefixUpdater(CategorizationFragment catFragment) {
        this.catFragment = catFragment;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        filter = catFragment.categoriesFilter.getText().toString();
        catFragment.categoriesSearchInProgress.setVisibility(View.VISIBLE);
        catFragment.categoriesNotFoundView.setVisibility(View.GONE);

        catFragment.categoriesSkip.setVisibility(View.GONE);
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if(TextUtils.isEmpty(filter)) {
            return catFragment.recentCatQuery();
        }

        //if user types in something that is in cache, return cached category
        if(catFragment.categoriesCache.containsKey(filter)) {
            return catFragment.categoriesCache.get(filter);
        }

        //otherwise if user has typed something in that isn't in cache, search API for matching categories
        MWApi api = CommonsApplication.createMWApi();
        ApiResult result;
        ArrayList<String> categories = new ArrayList<String>();
        try {
            result = api.action("query")
                    .param("list", "allcategories")
                    .param("acprefix", filter)
                    .param("aclimit", catFragment.SEARCH_CATS_LIMIT)
                    .get();
            Log.d(TAG, "Prefix URL filter" + result.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: ", e);
            //Return empty arraylist
            return categories;
        }

        ArrayList<ApiResult> categoryNodes = result.getNodes("/api/query/allcategories/c");
        for(ApiResult categoryNode: categoryNodes) {
            categories.add(categoryNode.getDocument().getTextContent());
        }

        return categories;
    }
}
