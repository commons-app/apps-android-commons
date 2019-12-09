package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;

public class NearbyFilterSearchRecyclerViewAdapter
        extends RecyclerView.Adapter<NearbyFilterSearchRecyclerViewAdapter.RecyclerViewHolder>
        implements Filterable {

    private final LayoutInflater inflater;
    private Context context;
    private RecyclerView recyclerView;
    private ArrayList<Label> labels;
    private ArrayList<Label> displayedLabels;
    public ArrayList<Label> selectedLabels = new ArrayList<>();

    private int state;

    RecyclerView.SmoothScroller smoothScroller;

    public NearbyFilterSearchRecyclerViewAdapter(Context context, ArrayList<Label> labels, RecyclerView recyclerView) {
        this.context = context;
        this.labels = labels;
        this.displayedLabels = labels;
        this.recyclerView = recyclerView;
        smoothScroller = new
                LinearSmoothScroller(context) {
                    @Override protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }
                };
        inflater = LayoutInflater.from(context);
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView placeTypeLabel;
        public ImageView placeTypeIcon;
        public LinearLayout placeTypeLayout;

        public RecyclerViewHolder(View view) {
            super(view);
            placeTypeLabel = view.findViewById(R.id.place_text);
            placeTypeIcon = view.findViewById(R.id.place_icon);
            placeTypeLayout = view.findViewById(R.id.search_list_item);
        }
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.nearby_search_list_item, parent, false);
        return new RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Label label = displayedLabels.get(position);
        holder.placeTypeIcon.setImageResource(label.getIcon());
        holder.placeTypeLabel.setText(label.toString());

        holder.placeTypeLayout.setBackgroundColor(label.isSelected() ? ContextCompat.getColor(context, R.color.divider_grey) : Color.WHITE);
        holder.placeTypeLayout.setOnClickListener(view -> {
            NearbyParentFragmentPresenter.getInstance().setCheckboxUnknown();
            if (label.isSelected()) {
                selectedLabels.remove(label);
            } else {
                selectedLabels.add(label);
            }
            label.setSelected(!label.isSelected());
            holder.placeTypeLayout.setBackgroundColor(label.isSelected() ? ContextCompat.getColor(context, R.color.divider_grey) : Color.WHITE);
            NearbyParentFragmentPresenter.getInstance().filterByMarkerType(selectedLabels, 0, false, false);
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
                FilterResults results = new FilterResults();
                ArrayList<Label> filteredArrayList = new ArrayList<>();

                if (labels == null) {
                    labels = new ArrayList<>(displayedLabels);
                }

                if (constraint == null || constraint.length() == 0) {
                    // set the Original result to return
                    results.count = labels.size();
                    results.values = labels;
                } else {
                    constraint = constraint.toString().toLowerCase();

                    for (Label label : labels) {
                        String data = label.toString();
                        if (data.toLowerCase().startsWith(constraint.toString())) {
                            filteredArrayList.add(Label.fromText(label.getText()));
                        }
                    }

                    // set the Filtered result to return
                    results.count = filteredArrayList.size();
                    results.values = filteredArrayList;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayedLabels = (ArrayList<Label>) results.values; // has the filtered values
                notifyDataSetChanged();  // notifies the data with new filtered values
            }
        };
    }

    public void setRecyclerViewAdapterItemsGreyedOut() {
        state = CheckBoxTriStates.UNCHECKED;
        for (Label label : labels) {
            label.setSelected(false);
            selectedLabels.remove(label);
        }
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
        notifyDataSetChanged();
    }

    public void setRecyclerViewAdapterNeutral() {
        state = CheckBoxTriStates.UNKNOWN;
    }

}
