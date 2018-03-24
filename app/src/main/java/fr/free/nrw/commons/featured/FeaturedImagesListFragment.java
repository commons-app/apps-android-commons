package fr.free.nrw.commons.featured;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.util.ArrayList;

import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;


/**
 * Created by root on 09.01.2018.
 */

public class FeaturedImagesListFragment extends DaggerFragment {
    private GridView gridView;
    private MockGridViewAdapter gridAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_featured_images, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridView = getView().findViewById(R.id.featuredImagesList);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        gridAdapter = new MockGridViewAdapter(this.getContext(), R.layout.layout_featured_images, getMockFeaturedImages());
        gridView.setAdapter(gridAdapter);

    }

    private ArrayList<FeaturedImage> getMockFeaturedImages(){
        ArrayList<FeaturedImage> featuredImages = new ArrayList<>();
        for (int i=0; i<10; i++){
            featuredImages.add(new FeaturedImage(new Media("test.jpg"), "username: test", "test file name"));
        }
        return featuredImages;
    }

    public ListAdapter getAdapter() {
        return gridView.getAdapter();
    }
}
