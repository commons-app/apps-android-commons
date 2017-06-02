package fr.free.nrw.commons.nearby;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class NoPermissionsFragment extends Fragment {


    public NoPermissionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Timber.d("NoPermissionsFragment created");
        View view = inflater.inflate(R.layout.fragment_no_permissions, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

}
