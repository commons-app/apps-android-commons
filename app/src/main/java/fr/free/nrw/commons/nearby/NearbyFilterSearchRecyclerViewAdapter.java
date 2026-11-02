package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.free.nrw.commons.R;

public class NearbyFilterSearchRecyclerViewAdapter
        extends RecyclerView.Adapter<NearbyFilterSearchRecyclerViewAdapter.RecyclerViewHolder>
        implements Filterable {

    private final LayoutInflater inflater;
    private Context context;

    private final ArrayList<Label> labels;

    private ArrayList<Label> displayedLabels;

    public ArrayList<Label> selectedLabels = new ArrayList<>();

    private int state;

    private Callback callback;

    RecyclerView.SmoothScroller smoothScroller;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public NearbyFilterSearchRecyclerViewAdapter(Context context, ArrayList<Label> labels, RecyclerView recyclerView) {
        this.context = context;
        this.labels = new ArrayList<>(labels);
        displayedLabels = new ArrayList<>(this.labels);

        smoothScroller = new LinearSmoothScroller(context) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        inflater = LayoutInflater.from(context);
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView placeTypeLabel;
        public ImageView placeTypeIcon;
        public ImageView placeSelected;
        public LinearLayout placeTypeLayout;

        public RecyclerViewHolder(View view) {
            super(view);
            placeTypeLabel = view.findViewById(R.id.place_text);
            placeTypeIcon = view.findViewById(R.id.place_icon);
            placeSelected = view.findViewById(R.id.place_selected);
            placeTypeLayout = view.findViewById(R.id.search_list_item);
        }
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(callback.isDarkTheme() ? R.layout.nearby_search_list_item_dark : R.layout.nearby_search_list_item, parent, false);
        return new RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Label label = displayedLabels.get(position);
        holder.placeTypeIcon.setImageResource(label.getIcon());
        holder.placeTypeLabel.setText(label.toString());
        holder.placeTypeLayout.setSelected(label.isSelected());
        holder.placeSelected.setVisibility(label.isSelected() ? View.VISIBLE : View.GONE);

        holder.placeTypeLayout.setOnClickListener(view -> {
            callback.setCheckboxUnknown();

            if (label.isSelected()) {
                selectedLabels.remove(label);
            } else {
                selectedLabels.add(label);
            }

            label.setSelected(!label.isSelected());
            holder.placeTypeLayout.setSelected(label.isSelected());
            holder.placeSelected.setVisibility(label.isSelected() ? View.VISIBLE : View.GONE);

            NearbyFilterState.setSelectedLabels(new ArrayList<>(selectedLabels));
            callback.filterByMarkerType(selectedLabels, 0, false, false);
        });
    }

    @Override
    public long getItemId(int position) {
        return displayedLabels.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return displayedLabels.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<Label> filteredArrayList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredArrayList.addAll(labels);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Label label : labels) { // Iterate over the master list
                        if (label.getText() != null && label.getText().toLowerCase().contains(filterPattern)) {
                            filteredArrayList.add(label);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredArrayList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayedLabels.clear(); // Safe to clear, this no longer destroys the master list
                @SuppressWarnings("unchecked")
                ArrayList<Label> filteredList = (ArrayList<Label>) results.values;
                displayedLabels.addAll(filteredList);
                notifyDataSetChanged();
            }
        };
    }

    public void setRecyclerViewAdapterItemsGreyedOut() {
        state = CheckBoxTriStates.UNCHECKED;

        for (Label label : labels) {
            label.setSelected(false);
            selectedLabels.remove(label);
        }
        NearbyFilterState.setSelectedLabels(new ArrayList<>(selectedLabels));
        notifyDataSetChanged();
    }

    public void setRecyclerViewAdapterAllSelected() {
        state = CheckBoxTriStates.CHECKED;
        for (Label label : labels) {
            label.setSelected(true);
            if (!selectedLabels.contains(label)) {
                selectedLabels.add(label);
            }
        }
        NearbyFilterState.setSelectedLabels(new ArrayList<>(selectedLabels));
        notifyDataSetChanged();
    }

    public interface Callback {
        void setCheckboxUnknown();
        void filterByMarkerType(ArrayList<Label> selectedLabels, int i, boolean b, boolean b1);
        boolean isDarkTheme();
    }
}
