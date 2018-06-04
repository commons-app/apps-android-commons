package fr.free.nrw.commons.explore.recent_searches;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.SearchActivity;


public class RecentSearchesFragment extends CommonsDaggerSupportFragment {
    @Inject RecentSearchesDao recentSearchesDao;
    @BindView(R.id.recent_searches_list) ListView recentSearchesList;
    List<String> recentSearches;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_history, container, false);
        ButterKnife.bind(this, rootView);

        recentSearches = recentSearchesDao.recentSearches(10);
        ArrayAdapter adapter = new ArrayAdapter<String>(getContext(),R.layout.item_recent_searches,recentSearchesDao.recentSearches(10));
        recentSearchesList.setAdapter(adapter);
        recentSearchesList.setOnItemClickListener((parent, view, position, id) -> {
            ((SearchActivity)getContext()).updateText(recentSearches.get(position));
            Toast.makeText(getContext(),recentSearches.get(position),Toast.LENGTH_SHORT).show();
        });
        adapter.notifyDataSetChanged();
        return rootView;
    }


}
