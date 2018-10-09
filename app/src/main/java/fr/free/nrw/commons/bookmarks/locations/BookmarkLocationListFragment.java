package fr.free.nrw.commons.bookmarks.locations;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;

public class BookmarkLocationListFragment extends DaggerFragment {

    /**
     * Create an instance of the fragment with the right bundle parameters
     * @return an instance of the fragment
     */
    public static BookmarkLocationListFragment newInstance() {
        return new BookmarkLocationListFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_bookmarks_locations, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
