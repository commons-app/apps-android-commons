package fr.free.nrw.commons.explore;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewHolder;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.ResourceUtils;

/**
 * Created by nesli on 03.06.2017.
 */

public class ExploreViewHolder implements ViewHolder<Place> {
    @BindView(R.id.exploreImage) MediaWikiImageView imageView;
    @BindView(R.id.exploreTitle) TextView titleView;
    @BindView(R.id.exploreDescription) TextView descriptionView;
    @BindView(R.id.exploreSequenceNumber) TextView seqNumView;
    @BindView(R.id.exploreProgress) ProgressBar progressView;
    @BindView(R.id.exploreDistance) TextView distanceView;
    private int seqNum;

    ExploreViewHolder(View parent, int seqNum) {
        ButterKnife.bind(this, parent);
        this.seqNum = seqNum;
    }

    @Override
    public void bindModel(Context context, Place place) {
        titleView.setText(place.name);
        String description = place.description;
        if ( description == null || description.isEmpty() || description.equals("?")) {
            description = context.getString(R.string.no_description_found);
        }
        descriptionView.setText(description);
        distanceView.setText(place.distance);
        seqNumView.setText(seqNum+"");
        imageView.setImageBitmap(place.image);
    }
}
