package fr.free.nrw.commons.contributions;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;

class ContributionViewHolder {
    final MediaWikiImageView imageView;
    final TextView titleView;
    final TextView stateView;
    final TextView seqNumView;
    final ProgressBar progressView;

    ContributionViewHolder(View parent) {
        imageView = parent.findViewById(R.id.contributionImage);
        titleView = parent.findViewById(R.id.contributionTitle);
        stateView = parent.findViewById(R.id.contributionState);
        seqNumView = parent.findViewById(R.id.contributionSequenceNumber);
        progressView = parent.findViewById(R.id.contributionProgress);
    }
}
