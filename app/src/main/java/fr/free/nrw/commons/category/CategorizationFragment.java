package fr.free.nrw.commons.category;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.MwVolleyApi;
import timber.log.Timber;

/**
 * Displays the category suggestion and selection screen. Category search is initiated here.
 */
public class CategorizationFragment extends Fragment {
    public interface OnCategoriesSaveHandler {
        void onCategoriesSave(ArrayList<String> categories);
    }

    ListView categoriesList;
    protected EditText categoriesFilter;
    ProgressBar categoriesSearchInProgress;
    TextView categoriesNotFoundView;
    TextView categoriesSkip;

    CategoriesAdapter categoriesAdapter;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    private OnCategoriesSaveHandler onCategoriesSaveHandler;

    protected HashMap<String, ArrayList<String>> categoriesCache;

    private ArrayList<String> selectedCategories = new ArrayList<>();

    // LHS guarantees ordered insertions, allowing for prioritized method A results
    private final Set<String> results = new LinkedHashSet<>();
    PrefixUpdater prefixUpdaterSub;
    MethodAUpdater methodAUpdaterSub;

    private final ArrayList<String> titleCatItems = new ArrayList<>();
    final CountDownLatch mergeLatch = new CountDownLatch(1);

    private ContentProviderClient client;

    protected final static int SEARCH_CATS_LIMIT = 25;

    public static class CategoryItem implements Parcelable {
        public String name;
        public boolean selected;

        public static Creator<CategoryItem> CREATOR = new Creator<CategoryItem>() {
            @Override
            public CategoryItem createFromParcel(Parcel parcel) {
                return new CategoryItem(parcel);
            }

            @Override
            public CategoryItem[] newArray(int i) {
                return new CategoryItem[0];
            }
        };

        public CategoryItem(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public CategoryItem(Parcel in) {
            name = in.readString();
            selected = in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(name);
            parcel.writeInt(selected ? 1 : 0);
        }
    }

    /**
     * Retrieves category suggestions from title input
     * @return a list containing title-related categories
     */
    protected ArrayList<String> titleCatQuery() {

        TitleCategories titleCategoriesSub;

        //Retrieve the title that was saved when user tapped submit icon
        SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String title = titleDesc.getString("Title", "");
        Timber.d("Title: %s", title);

        //Override onPostExecute to access the results of async API call
        titleCategoriesSub = new TitleCategories(title) {
            @Override
            protected void onPostExecute(ArrayList<String> result) {
                super.onPostExecute(result);
                Timber.d("Results in onPostExecute: %s", result);
                titleCatItems.addAll(result);
                Timber.d("TitleCatItems in onPostExecute: %s", titleCatItems);
                mergeLatch.countDown();
            }
        };

        titleCategoriesSub.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Timber.d("TitleCatItems in titleCatQuery: %s", titleCatItems);

        //Only return titleCatItems after API call has finished
        try {
            mergeLatch.await(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Timber.e(e, "Interrupted exception: ");
        }
        return titleCatItems;
    }

    /**
     * Retrieves recently-used categories
     * @return a list containing recent categories
     */
    protected ArrayList<String> recentCatQuery() {
        ArrayList<String> items = new ArrayList<>();

        try {
            Cursor cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    Category.Table.COLUMN_LAST_USED + " DESC");
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor.moveToNext() && cursor.getPosition() < SEARCH_CATS_LIMIT) {
                Category cat = Category.fromCursor(cursor);
                items.add(cat.getName());
            }
            cursor.close();
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    /**
     * Merges nearby categories, categories suggested based on title, and recent categories... without duplicates.
     * @return a list containing merged categories
     */
    protected ArrayList<String> mergeItems() {

        Set<String> mergedItems = new LinkedHashSet<>();

        Timber.d("Calling APIs for GPS cats, title cats and recent cats...");

        List<String> gpsItems = new ArrayList<>();
        if (MwVolleyApi.GpsCatExists.getGpsCatExists()) {
            gpsItems.addAll(MwVolleyApi.getGpsCat());
        }
        List<String> titleItems = new ArrayList<>(titleCatQuery());
        List<String> recentItems = new ArrayList<>(recentCatQuery());

        //Await results of titleItems, which is likely to come in last
        try {
            mergeLatch.await(5L, TimeUnit.SECONDS);
            Timber.d("Waited for merge");
        } catch (InterruptedException e) {
            Timber.e(e, "Interrupted Exception: ");
        }

        mergedItems.addAll(gpsItems);
        Timber.d("Adding GPS items: %s", gpsItems);
        mergedItems.addAll(titleItems);
        Timber.d("Adding title items: %s", titleItems);
        mergedItems.addAll(recentItems);
        Timber.d("Adding recent items: %s", recentItems);
        
        //Needs to be an ArrayList and not a List unless we want to modify a big portion of preexisting code
        ArrayList<String> mergedItemsList = new ArrayList<>(mergedItems);

        Timber.d("Merged item list: %s", mergedItemsList);
        return mergedItemsList;
    }

    /**
     * Displays categories found to the user as they type in the search box
     * @param categories a list of all categories found for the search string
     * @param filter the search string
     */
    protected void setCatsAfterAsync(ArrayList<String> categories, String filter) {

        if (getActivity() != null) {
            ArrayList<CategoryItem> items = new ArrayList<>();
            HashSet<String> existingKeys = new HashSet<>();
            for (CategoryItem item : categoriesAdapter.getItems()) {
                if (item.selected) {
                    items.add(item);
                    existingKeys.add(item.name);
                }
            }
            for (String category : categories) {
                if (!existingKeys.contains(category)) {
                    items.add(new CategoryItem(category, false));
                }
            }

            categoriesAdapter.setItems(items);
            categoriesAdapter.notifyDataSetInvalidated();
            categoriesSearchInProgress.setVisibility(View.GONE);

            if (categories.isEmpty()) {
                if (TextUtils.isEmpty(filter)) {
                    // If we found no recent cats, show the skip message!
                    categoriesSkip.setVisibility(View.VISIBLE);
                } else {
                    categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
                    categoriesNotFoundView.setVisibility(View.VISIBLE);
                }
            } else {
                categoriesList.smoothScrollToPosition(existingKeys.size());
            }
        }
        else {
            Timber.e("Error: Fragment is null");
        }
    }


    /**
     * Makes asynchronous calls to the Commons MediaWiki API via anonymous subclasses of
     * 'MethodAUpdater' and 'PrefixUpdater'. Some of their methods are overridden in order to
     * aggregate the results. A CountDownLatch is used to ensure that MethodA results are shown
     * above Prefix results.
     */
    private void requestSearchResults() {

        final CountDownLatch latch = new CountDownLatch(1);

        prefixUpdaterSub = new PrefixUpdater(this) {
            @Override
            protected ArrayList<String> doInBackground(Void... voids) {
                ArrayList<String> result = new ArrayList<>();
                try {
                    result = super.doInBackground();
                    latch.await();
                }
                catch (InterruptedException e) {
                    Timber.w(e);
                    //Thread.currentThread().interrupt();
                }
                return result;
            }

            @Override
            protected void onPostExecute(ArrayList<String> result) {
                super.onPostExecute(result);

                results.addAll(result);
                Timber.d("Prefix result: %s", result);

                String filter = categoriesFilter.getText().toString();
                ArrayList<String> resultsList = new ArrayList<>(results);
                categoriesCache.put(filter, resultsList);
                Timber.d("Final results List: %s", resultsList);

                categoriesAdapter.notifyDataSetChanged();
                setCatsAfterAsync(resultsList, filter);
            }
        };

        methodAUpdaterSub = new MethodAUpdater(this) {
            @Override
            protected void onPostExecute(ArrayList<String> result) {
                results.clear();
                super.onPostExecute(result);

                results.addAll(result);
                Timber.d("Method A result: %s", result);
                categoriesAdapter.notifyDataSetChanged();

                latch.countDown();
            }
        };
        prefixUpdaterSub.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        methodAUpdaterSub.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void startUpdatingCategoryList() {

        if (prefixUpdaterSub != null) {
            prefixUpdaterSub.cancel(true);
        }

        if (methodAUpdaterSub != null) {
            methodAUpdaterSub.cancel(true);
        }

        requestSearchResults();
    }

    public int getCurrentSelectedCount() {
        int count = 0;
        for(CategoryItem item: categoriesAdapter.getItems()) {
            if(item.selected) {
                count++;
            }
        }
        return count;
    }

    private Category lookupCategory(String name) {
        Cursor cursor = null;
        try {
            cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    Category.Table.COLUMN_NAME + "=?",
                    new String[] {name},
                    null);
            if (cursor.moveToFirst()) {
                Category cat = Category.fromCursor(cursor);
                return cat;
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }

        // Newly used category...
        Category cat = new Category();
        cat.setName(name);
        cat.setLastUsed(new Date());
        cat.setTimesUsed(0);
        return cat;
    }

    private class CategoryCountUpdater extends AsyncTask<Void, Void, Void> {

        private String name;

        public CategoryCountUpdater(String name) {
            this.name = name;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Category cat = lookupCategory(name);
            cat.incTimesUsed();

            cat.setContentProviderClient(client);
            cat.save();

            return null; // Make the compiler happy.
        }
    }

    private void updateCategoryCount(String name) {
        new CategoryCountUpdater(name).executeOnExecutor(executor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_categorization, null);
        categoriesList = (ListView) rootView.findViewById(R.id.categoriesListBox);
        categoriesFilter = (EditText) rootView.findViewById(R.id.categoriesSearchBox);
        categoriesSearchInProgress = (ProgressBar) rootView.findViewById(R.id.categoriesSearchInProgress);
        categoriesNotFoundView = (TextView) rootView.findViewById(R.id.categoriesNotFound);
        categoriesSkip = (TextView) rootView.findViewById(R.id.categoriesExplanation);

        categoriesSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
                getActivity().finish();
            }
        });

        ArrayList<CategoryItem> items;
        if(savedInstanceState == null) {
            items = new ArrayList<>();
            categoriesCache = new HashMap<>();
        } else {
            items = savedInstanceState.getParcelableArrayList("currentCategories");
            categoriesCache = (HashMap<String, ArrayList<String>>) savedInstanceState.getSerializable("categoriesCache");
        }

        categoriesAdapter = new CategoriesAdapter(getActivity(), items);
        categoriesList.setAdapter(categoriesAdapter);

        categoriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long id) {
                CheckedTextView checkedView = (CheckedTextView) view;
                CategoryItem item = (CategoryItem) adapterView.getAdapter().getItem(index);
                item.selected = !item.selected;
                checkedView.setChecked(item.selected);
                if (item.selected) {
                    updateCategoryCount(item.name);
                }
            }
        });

        categoriesFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                startUpdatingCategoryList();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        startUpdatingCategoryList();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.fragment_categorization, menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.categories_activity_title);
        client = getActivity().getContentResolver().acquireContentProviderClient(CategoryContentProvider.AUTHORITY);
    }

    @Override
    public void onResume() {
        super.onResume();

        View rootView = getView();
        if (rootView != null) {
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
            rootView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        backButtonDialog();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void backButtonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage("Are you sure you want to go back? The image will not have any categories saved.")
                .setTitle("Warning");
        builder.setPositiveButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //No need to do anything, user remains on categorization screen
            }
        });
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                getActivity().finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("currentCategories", categoriesAdapter.getItems());
        outState.putSerializable("categoriesCache", categoriesCache);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.menu_save_categories:

                int numberSelected = 0;

                for(CategoryItem item: categoriesAdapter.getItems()) {
                    if(item.selected) {
                        selectedCategories.add(item.name);
                        numberSelected++;
                    }
                }

                //If no categories selected, display warning to user
                if (numberSelected == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setMessage("Images without categories are rarely usable. Are you sure you want to submit without selecting categories?")
                            .setTitle("No Categories Selected");
                    builder.setPositiveButton("No, go back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //Exit menuItem so user can select their categories
                            return;
                        }
                    });
                    builder.setNegativeButton("Yes, submit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //Proceed to submission
                            onCategoriesSaveHandler.onCategoriesSave(selectedCategories);
                            return;
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    //Proceed to submission
                    onCategoriesSaveHandler.onCategoriesSave(selectedCategories);
                    return true;
                }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onCategoriesSaveHandler = (OnCategoriesSaveHandler) activity;
    }
}
