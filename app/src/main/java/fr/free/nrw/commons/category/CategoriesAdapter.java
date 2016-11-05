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
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    private Context context;
    private LayoutInflater mInflater;

    //FIXME: Might have issue here, headers need to be a String type so you can't just add them to an ArrayList of CategoryItem
    private ArrayList<CategorizationFragment.CategoryItem> items;

    private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

    public CategoriesAdapter(Context context, ArrayList<CategorizationFragment.CategoryItem> items) {
        this.context = context;
        this.items = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return items.size();
    }

    public Object getItem(int i) {
        return items.get(i);
    }

    public ArrayList<CategorizationFragment.CategoryItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<CategorizationFragment.CategoryItem> items) {
        this.items = items;
    }

    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int position) {
        // If type is 1, the line is a header, otherwise it is an item
        return sectionHeader.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

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

    /*
    // TODO: Separator getView reference
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM:
                    convertView = mInflater.inflate(R.layout.snippet_item1, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.text);
                    break;
                case TYPE_SEPARATOR:
                    convertView = mInflater.inflate(R.layout.snippet_item2, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.textSeparator);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(mData.get(position));

        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
    }
    */
}