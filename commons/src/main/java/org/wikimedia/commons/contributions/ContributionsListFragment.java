package org.wikimedia.commons.contributions;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.wikimedia.commons.R;
import org.wikimedia.commons.UploadService;
import org.wikimedia.commons.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ContributionsListFragment extends SherlockFragment {

    private GridView contributionsList;

    private ContributionsListAdapter contributionsAdapter;

    private DisplayImageOptions contributionDisplayOptions;
    private Cursor allContributions;

    private class ContributionsListAdapter extends CursorAdapter {

        public ContributionsListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return getActivity().getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView imageView = (ImageView)view.findViewById(R.id.contributionImage);
            TextView titleView = (TextView)view.findViewById(R.id.contributionTitle);
            TextView stateView = (TextView)view.findViewById(R.id.contributionState);

            Contribution contribution = Contribution.fromCursor(cursor);

            String actualUrl = TextUtils.isEmpty(contribution.getImageUrl()) ? contribution.getLocalUri().toString() : contribution.getThumbnailUrl(320);
            Log.d("Commons", "Trying URL " + actualUrl);

            Log.d("Commons", "For " + contribution.toContentValues());

            if(imageView.getTag() == null || !imageView.getTag().equals(actualUrl)) {
                Log.d("Commons", "Tag is " + imageView.getTag() + " url is " + actualUrl); //+ " equals is " + imageView.getTag().equals(actualUrl) + " the other thing is " + (imageView.getTag() == null));

                ImageLoader.getInstance().displayImage(actualUrl, imageView, contributionDisplayOptions);
                imageView.setTag(actualUrl);
            }

            titleView.setText(Utils.displayTitleFromTitle(contribution.getFilename()));
            switch(contribution.getState()) {
                case Contribution.STATE_COMPLETED:
                    Date uploaded = contribution.getDateUploaded();
                    stateView.setText(SimpleDateFormat.getDateInstance().format(uploaded));
                    break;
                case Contribution.STATE_QUEUED:
                    stateView.setText(R.string.contribution_state_queued);
                    break;
                case Contribution.STATE_IN_PROGRESS:
                    stateView.setText(R.string.contribution_state_starting);
                    long total = contribution.getDataLength();
                    long transferred = contribution.getTransferred();
                    String stateString = String.format(getString(R.string.contribution_state_in_progress), (int)(((double)transferred / (double)total) * 100));
                    stateView.setText(stateString);
                    break;
                case Contribution.STATE_FAILED:
                    stateView.setText(R.string.contribution_state_failed);
                    break;
            }

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contributions, container, false);
    }

    public void setCursor(Cursor cursor) {
        if(allContributions == null) {
            contributionsAdapter = new ContributionsListAdapter(this.getActivity(), cursor, 0);
            contributionsList.setAdapter(contributionsAdapter);
        }
        allContributions = cursor;
        contributionsAdapter.swapCursor(cursor);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contributionsList = (GridView)getView().findViewById(R.id.contributionsList);
        contributionDisplayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .cacheInMemory()
                .cacheOnDisc()
                .resetViewBeforeLoading().build();

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());

    }
}
