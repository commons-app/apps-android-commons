package fr.free.nrw.commons.category;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryEditSearchRecyclerViewAdapter.RecyclerViewHolder;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.nearby.Label;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class CategoryEditSearchRecyclerViewAdapter
    extends RecyclerView.Adapter<RecyclerViewHolder>
    implements Filterable {

    private List<String> displayedCategories;
    private List<String> selectedCategories = new ArrayList<>();
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

    public void addToSelectedCategories(List<String> selectedCategories) {
        for(String category : selectedCategories) {
            if (!this.selectedCategories.contains(category)) {
                this.selectedCategories.add(category);
            }
        }
    }

    public void addToSelectedCategories(String selectedCategory) {
        if (!selectedCategories.contains(selectedCategory)) {
            selectedCategories.add(selectedCategory);
        }
    }

    public void removeFromSelectedCategories(String deselectedCategory) {
        if (selectedCategories.contains(deselectedCategory)) {
            selectedCategories.remove(deselectedCategory);
        }
    }

    public List<String> getSelectedCategories() {
        return selectedCategories;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<String> resultCategories = categoryClient.searchCategories(constraint.toString(), 10).blockingSingle();
                results.values = resultCategories;
                results.count = resultCategories.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayedCategories = (List<String>)results.values;
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
                        addToSelectedCategories(categoryTextView.getText().toString());
                    } else {
                        removeFromSelectedCategories(categoryTextView.getText().toString());
                    }
                    callback.updateSelectedCategoriesTextView(selectedCategories);
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
