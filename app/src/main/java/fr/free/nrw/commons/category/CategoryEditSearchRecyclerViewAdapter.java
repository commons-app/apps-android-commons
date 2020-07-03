package fr.free.nrw.commons.category;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter.RecyclerViewHolder;
import fr.free.nrw.commons.nearby.Label;
import java.util.ArrayList;
import java.util.List;

public class CategoryEditSearchRecyclerViewAdapter
    extends RecyclerView.Adapter<RecyclerViewHolder>
    implements Filterable {

    private List<String> displayedCategories;
    private List<String> categories = new ArrayList<>();
    private List<String> newCategories = new ArrayList<>();
    private final LayoutInflater inflater;
    private CategoryClient categoryClient;
    private Context context;

    private Callback callback;

    public CategoryEditSearchRecyclerViewAdapter(Context context, ArrayList<Label> labels,
        RecyclerView categoryRecyclerView, CategoryClient categoryClient, Callback callback) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.categoryClient = categoryClient;
        this.callback = callback;
    }

    public void addToCategories(List<String> categories) {
        for(String category : categories) {
            if (!this.categories.contains(category)) {
                this.categories.add(category);
            }
        }
    }

    public void addToCategories(String categoryToBeAdded) {
        if (!categories.contains(categoryToBeAdded)) {
            categories.add(categoryToBeAdded);
        }
    }

    public void removeFromCategories(String categoryToBeRemoved) {
        if (categories.contains(categoryToBeRemoved)) {
            categories.remove(categoryToBeRemoved);
        }
    }

    public void removeFromNewCategories(String categoryToBeRemoved) {
        if (newCategories.contains(categoryToBeRemoved)) {
            newCategories.remove(categoryToBeRemoved);
        }
    }

    public void addToNewCategories(List<String> newCategories) {
        for(String category : newCategories) {
            if (!this.newCategories.contains(category)) {
                this.newCategories.add(category);
            }
        }
    }

    public void addToNewCategories(String addedCategory) {
        if (!newCategories.contains(addedCategory)) {
            newCategories.add(addedCategory);
        }
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getNewCategories() {
        return newCategories;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<String> resultCategories = categoryClient.searchCategories(constraint.toString(), 10).blockingGet();
                results.values = resultCategories;
                results.count = resultCategories.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                List<String> resultList = (List<String>)results.values;
                // Do not re-add already added categories
                for (String category : categories) {
                    if (resultList.contains(category)) {
                        resultList.remove(category);
                    }
                }

                displayedCategories = resultList;
                notifyDataSetChanged();
                if (displayedCategories.size()==0) {
                    callback.noResultsFound();
                } else {
                    callback.someResultsFound();
                }
            }
        };
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public CheckBox categoryCheckBox;
        public TextView categoryTextView;

        public RecyclerViewHolder(View view) {
            super(view);
            categoryCheckBox = view.findViewById(R.id.category_checkbox);
            categoryTextView = view.findViewById(R.id.category_text);
            categoryCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        addToNewCategories(categoryTextView.getText().toString());
                    } else {
                        removeFromNewCategories(categoryTextView.getText().toString());
                    }
                    List<String> allCategories = new ArrayList<>();
                    allCategories.addAll(categories);
                    allCategories.addAll(newCategories);
                    callback.updateSelectedCategoriesTextView(allCategories);
                }
            });
        }
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.layout_edit_category_item , parent, false);
        return new RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        holder.categoryTextView.setText(displayedCategories.get(position));
    }

    @Override
    public long getItemId(int position) {
        return displayedCategories.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return (displayedCategories == null) ? 0 : displayedCategories.size();
    }

    public interface  Callback {
        void updateSelectedCategoriesTextView(List<String> selectedCategories);
        void noResultsFound();
        void someResultsFound();
    }
}
