package fr.free.nrw.commons.explore.recent_searches;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;


public class RecentSearchesFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_history, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

}
