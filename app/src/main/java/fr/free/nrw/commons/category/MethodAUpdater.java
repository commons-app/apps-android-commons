package fr.free.nrw.commons.category;

import android.os.AsyncTask;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to retrieve categories that are close to
 * the keyword typed in by the user. The 'srsearch' action-specific parameter is used for this
 * purpose. This class should be subclassed in CategorizationFragment.java to aggregate the results.
 */
class MethodAUpdater extends AsyncTask<Void, Void, List<String>> {

    private String filter;
    private CategorizationFragment catFragment;

    MethodAUpdater(CategorizationFragment catFragment) {
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
     *
     * @param items Unfiltered list of categories
     * @return Filtered category list
     */
    private List<String> filterYears(List<String> items) {

        Iterator<String> iterator;

        //Check for current and previous year to exclude these categories from removal
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String yearInString = String.valueOf(year);
        Timber.d("Year: %s", yearInString);

        int prevYear = year - 1;
        String prevYearInString = String.valueOf(prevYear);
        Timber.d("Previous year: %s", prevYearInString);

        //Copy to Iterator to prevent ConcurrentModificationException when removing item
        for (iterator = items.iterator(); iterator.hasNext(); ) {
            String s = iterator.next();

            //Check if s contains a 4-digit word anywhere within the string (.* is wildcard)
            //And that s does not equal the current year or previous year
            if (s.matches(".*(19|20)\\d{2}.*") && !s.contains(yearInString) && !s.contains(prevYearInString)) {
                Timber.d("Filtering out year %s", s);
                iterator.remove();
            }
        }

        Timber.d("Items: %s", items);
        return items;
    }

    @Override
    protected List<String> doInBackground(Void... voids) {

        //otherwise if user has typed something in that isn't in cache, search API for matching categories
        MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
        List<String> categories = new ArrayList<>();

        //URL https://commons.wikimedia.org/w/api.php?action=query&format=xml&list=search&srwhat=text&srenablerewrites=1&srnamespace=14&srlimit=10&srsearch=
        try {
            categories = api.searchCategories(CategorizationFragment.SEARCH_CATS_LIMIT, filter);
            Timber.d("Method A URL filter %s", categories);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            //Return empty arraylist
            return categories;
        }

        Timber.d("Found categories from Method A search, waiting for filter");
        return new ArrayList<>(filterYears(categories));
    }
}
