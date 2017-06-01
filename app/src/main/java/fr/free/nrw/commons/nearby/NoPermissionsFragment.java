package fr.free.nrw.commons.nearby;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.R;

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
        return inflater.inflate(R.layout.fragment_no_permissions, container, false);
    }

}
