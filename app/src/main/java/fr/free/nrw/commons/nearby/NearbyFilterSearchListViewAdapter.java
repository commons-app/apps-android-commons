package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import fr.free.nrw.commons.R;

public class NearbyFilterSearchListViewAdapter extends ArrayAdapter<Label> {

    private final LayoutInflater inflater;
    private ViewHolder viewHolder;
    private final ArrayList<Label> labels;

    public NearbyFilterSearchListViewAdapter(Context context, ArrayList<Label> labels) {
        super(context,0, labels);
        this.labels = labels;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return labels.size();
    }

    @Override
    public Label getItem(int position) {
        return labels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return labels.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {

            convertView = inflater.inflate(R.layout.nearby_search_list_item, null);

            viewHolder = new ViewHolder();
            viewHolder.placeLabel = convertView.findViewById(R.id.place_text);
            viewHolder.placeIcon = convertView.findViewById(R.id.place_icon);
            convertView.setTag(viewHolder);

        }
        else{
            //Get viewholder we already created
            viewHolder = (ViewHolder)convertView.getTag();
        }

        Label label = labels.get(position);
        if(label != null){
            viewHolder.placeIcon.setImageResource(label.getIcon());
            viewHolder.placeLabel.setText(label.toString());
        }
        return convertView;
    }

    //View Holder Pattern for better performance
    private static class ViewHolder {
        TextView placeLabel;
        ImageView placeIcon;
    }

}
