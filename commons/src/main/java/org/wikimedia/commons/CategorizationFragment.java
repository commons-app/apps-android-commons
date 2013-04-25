package org.wikimedia.commons;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.*;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.MenuInflater;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.wikimedia.commons.category.Category;
import org.wikimedia.commons.category.CategoryContentProvider;
import org.wikimedia.commons.contributions.Contribution;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class CategorizationFragment extends SherlockFragment{
    public static interface OnCategoriesSaveHandler {
        public void onCategoriesSave(ArrayList<String> categories);
    }

    ListView categoriesList;
    EditText categoriesFilter;
    ProgressBar categoriesSearchInProgress;
    TextView categoriesNotFoundView;
    TextView categoriesSkip;

    CategoriesAdapter categoriesAdapter;
    CategoriesUpdater lastUpdater = null;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private OnCategoriesSaveHandler onCategoriesSaveHandler;

    private HashMap<String, ArrayList<String>> categoriesCache;

    private ContentProviderClient client;

    private final int SEARCH_CATS_LIMIT = 25;

    public static class CategoryItem implements Parcelable {
        public String name;
        public boolean selected;

        public static Creator<CategoryItem> CREATOR = new Creator<CategoryItem>() {
            public CategoryItem createFromParcel(Parcel parcel) {
                return new CategoryItem(parcel);
            }

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

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(name);
            parcel.writeInt(selected ? 1 : 0);
        }
    }

    private class CategoriesUpdater extends AsyncTask<Void, Void, ArrayList<String>> {

        private String filter;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            filter = categoriesFilter.getText().toString();
            categoriesSearchInProgress.setVisibility(View.VISIBLE);
            categoriesNotFoundView.setVisibility(View.GONE);

            if(!TextUtils.isEmpty(filter)) {
                // Only hide this on first count of non-empty filter
                categoriesSkip.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> categories) {
            super.onPostExecute(categories);
            ArrayList<CategoryItem> items = new ArrayList<CategoryItem>();
            HashSet<String> existingKeys = new HashSet<String>();
            for(CategoryItem item : categoriesAdapter.getItems()) {
                if(item.selected) {
                    items.add(item);
                    existingKeys.add(item.name);
                }
            }
            for(String category : categories) {
                if(!existingKeys.contains(category)) {
                    items.add(new CategoryItem(category, false));
                }
            }
            categoriesAdapter.setItems(items);
            categoriesAdapter.notifyDataSetInvalidated();
            categoriesSearchInProgress.setVisibility(View.GONE);
            if(!TextUtils.isEmpty(filter) && categories.size() == 0) {
                categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
                categoriesNotFoundView.setVisibility(View.VISIBLE);
            } else {
                // If we found recent cats, hide the skip message!
                categoriesSkip.setVisibility(View.GONE);
            }
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            if(TextUtils.isEmpty(filter)) {
                ArrayList<String> items = new ArrayList<String>();
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
                } catch (RemoteException e) {
                    // faaaail
                    throw new RuntimeException(e);
                }
                return items;
            }
            if(categoriesCache.containsKey(filter)) {
                return categoriesCache.get(filter);
            }
            MWApi api = CommonsApplication.createMWApi();
            ApiResult result;
            ArrayList<String> categories = new ArrayList<String>();
            try {
                result = api.action("query")
                        .param("list", "allcategories")
                        .param("acprefix", filter)
                        .param("aclimit", SEARCH_CATS_LIMIT)
                        .get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ArrayList<ApiResult> categoryNodes = result.getNodes("/api/query/allcategories/c");
            for(ApiResult categoryNode: categoryNodes) {
                categories.add(categoryNode.getDocument().getTextContent());
            }

            categoriesCache.put(filter, categories);

            return categories;
        }
    }

    private class CategoriesAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<CategoryItem> items;

        private CategoriesAdapter(Context context, ArrayList<CategoryItem> items) {
            this.context = context;
            this.items = items;
        }

        public int getCount() {
            return items.size();
        }

        public Object getItem(int i) {
            return items.get(i);
        }

        public ArrayList<CategoryItem> getItems() {
            return items;
        }

        public void setItems(ArrayList<CategoryItem> items) {
            this.items = items;
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            CheckedTextView checkedView;

            if(view == null) {
                checkedView = (CheckedTextView) getSherlockActivity().getLayoutInflater().inflate(R.layout.layout_categories_item, null);

            } else {
                checkedView = (CheckedTextView) view;
            }

            CategoryItem item = (CategoryItem) this.getItem(i);
            checkedView.setChecked(item.selected);
            checkedView.setText(item.name);
            checkedView.setTag(i);

            return checkedView;
        }
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
        try {
            Cursor cursor = client.query(
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
        protected void doInBackground(Void... voids) {
            Category cat = lookupCategory(name);
            cat.incTimesUsed();

            cat.setContentProviderClient(client);
            cat.save();
        }
    }

    private void updateCategoryCount(String name) {
        Utils.executeAsyncTask(new CategoryCountUpdater(name), executor);
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
            public void onClick(View view) {
                getActivity().onBackPressed();
                getActivity().finish();
            }
        });

        ArrayList<CategoryItem> items;
        if(savedInstanceState == null) {
            items = new ArrayList<CategoryItem>();
            categoriesCache = new HashMap<String, ArrayList<String>>();
        } else {
            items = savedInstanceState.getParcelableArrayList("currentCategories");
            categoriesCache = (HashMap<String, ArrayList<String>>) savedInstanceState.getSerializable("categoriesCache");
        }

        categoriesAdapter = new CategoriesAdapter(getActivity(), items);
        categoriesList.setAdapter(categoriesAdapter);

        categoriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                startUpdatingCategoryList();
            }

            public void afterTextChanged(Editable editable) {

            }
        });

        startUpdatingCategoryList();

        return rootView;
    }

    private void startUpdatingCategoryList() {
        if (lastUpdater != null) {
            lastUpdater.cancel(true);
        }
        lastUpdater = new CategoriesUpdater();
        Utils.executeAsyncTask(lastUpdater, executor);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
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
                ArrayList<String> selectedCategories = new ArrayList<String>();
                for(CategoryItem item: categoriesAdapter.getItems()) {
                    if(item.selected) {
                        selectedCategories.add(item.name);
                    }
                }
                onCategoriesSaveHandler.onCategoriesSave(selectedCategories);
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onCategoriesSaveHandler = (OnCategoriesSaveHandler) activity;
    }
}
