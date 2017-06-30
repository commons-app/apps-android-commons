package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewHolder;

public class NearbyViewHolder implements ViewHolder<Place> {
    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon) ImageView icon;

    public NearbyViewHolder(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    public void bindModel(Context context, Place place) {
        // Populate the data into the template view using the data object
        tvName.setText(place.name);
        String descriptionText = place.getDescription().getText();
        if (descriptionText.equals("?")) {
            descriptionText = context.getString(R.string.no_description_found);
        }
        tvDesc.setText(descriptionText);
        distance.setText(place.distance);
        icon.setImageResource(place.getDescription().getIcon());
    }
}
