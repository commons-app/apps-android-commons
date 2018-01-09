package fr.free.nrw.commons.featured;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import timber.log.Timber;

import static android.view.View.GONE;

/**
 * Created by root on 09.01.2018.
 */

public class FeaturedImagesListFragment extends DaggerFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, v);
        return v;
    }
}
