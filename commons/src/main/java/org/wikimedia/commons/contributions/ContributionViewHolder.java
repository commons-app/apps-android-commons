package org.wikimedia.commons.contributions;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.wikimedia.commons.R;

class ContributionViewHolder {
    final ImageView imageView;
    final TextView titleView;
    final TextView stateView;
    final TextView seqNumView;
    final ProgressBar progressView;

    String url;

    ContributionViewHolder(View parent) {
        imageView = (ImageView)parent.findViewById(R.id.contributionImage);
        titleView = (TextView)parent.findViewById(R.id.contributionTitle);
        stateView = (TextView)parent.findViewById(R.id.contributionState);
        seqNumView = (TextView)parent.findViewById(R.id.contributionSequenceNumber);
        progressView = (ProgressBar)parent.findViewById(R.id.contributionProgress);
    }
}
