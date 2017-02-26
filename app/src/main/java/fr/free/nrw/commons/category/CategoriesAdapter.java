package fr.free.nrw.commons.category;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeSet;

import fr.free.nrw.commons.R;

public class CategoriesAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;

    private ArrayList<CategorizationFragment.CategoryItem> items;

    public CategoriesAdapter(Context context, ArrayList<CategorizationFragment.CategoryItem> items) {
        this.context = context;
        this.items = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    public ArrayList<CategorizationFragment.CategoryItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<CategorizationFragment.CategoryItem> items) {
        this.items = items;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        CheckedTextView checkedView;

        if(view == null) {
            checkedView = (CheckedTextView) mInflater.inflate(R.layout.layout_categories_item, null);

        } else {
            checkedView = (CheckedTextView) view;
        }

        CategorizationFragment.CategoryItem item = (CategorizationFragment.CategoryItem) this.getItem(i);
        checkedView.setChecked(item.selected);
        checkedView.setText(item.name);
        checkedView.setTag(i);

        return checkedView;
    }
}