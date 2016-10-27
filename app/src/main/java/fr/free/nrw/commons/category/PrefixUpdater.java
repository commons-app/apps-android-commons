package fr.free.nrw.commons.category;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import fr.free.nrw.commons.CommonsApplication;

import static android.R.id.list;

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

    /**
     * Remove categories that contain a year in them (starting with 19__ or 20__), except for this year
     * and previous year
     * Rationale: https://github.com/commons-app/apps-android-commons/issues/47
     * @param items Unfiltered list of categories
     * @return Filtered category list
     */
    private ArrayList<String> filterYears(ArrayList<String> items) {

        Iterator<String> iterator;

        //Check for current and previous year to exclude these categories from removal
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String yearInString = String.valueOf(year);
        Log.d(TAG, "Year: " + yearInString);

        int prevYear = year - 1;
        String prevYearInString = String.valueOf(prevYear);
        Log.d(TAG, "Previous year: " + prevYearInString);

        //Copy to Iterator to prevent ConcurrentModificationException when removing item
        for(iterator = items.iterator(); iterator.hasNext();) {
            String s = iterator.next();

            //Check if s contains a 4-digit word anywhere within the string (.* is wildcard)
            //And that s does not equal the current year or previous year
            if(s.matches(".*(19|20)\\d{2}.*") && !s.contains(yearInString) && !s.contains(prevYearInString)) {
                Log.d(TAG, "Filtering out year " + s);
                iterator.remove();
            }
        }

        Log.d(TAG, "Items: " + items.toString());
        return items;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if(TextUtils.isEmpty(filter)) {
            ArrayList<String> mergedItems = new ArrayList<String>(catFragment.mergeItems());
            Log.d(TAG, "Merged items, waiting for filter");
            ArrayList<String> filteredItems = new ArrayList<String>(filterYears(mergedItems));
            return filteredItems;
        }

        //if user types in something that is in cache, return cached category
        if(catFragment.categoriesCache.containsKey(filter)) {
            ArrayList<String> cachedItems = new ArrayList<String>(catFragment.categoriesCache.get(filter));
            Log.d(TAG, "Found cache items, waiting for filter");
            ArrayList<String> filteredItems = new ArrayList<String>(filterYears(cachedItems));
            return filteredItems;
        }

        //otherwise if user has typed something in that isn't in cache, search API for matching categories
        //URL: https://commons.wikimedia.org/w/api.php?action=query&list=allcategories&acprefix=filter&aclimit=25
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

        Log.d(TAG, "Found categories from Prefix search, waiting for filter");
        ArrayList<String> filteredItems = new ArrayList<String>(filterYears(categories));
        return filteredItems;
    }
}
