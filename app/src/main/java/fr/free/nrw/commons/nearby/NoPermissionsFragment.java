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
 * Tells user that Nearby Places cannot be displayed if location permissions are denied
 */
public class NoPermissionsFragment extends Fragment {

    public NoPermissionsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("NoPermissionsFragment created");
        View view = inflater.inflate(R.layout.fragment_no_permissions, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

}
