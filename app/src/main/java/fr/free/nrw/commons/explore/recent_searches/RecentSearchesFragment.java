package fr.free.nrw.commons.explore.recent_searches;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
    ArrayAdapter adapter;
    @BindView(R.id.recent_searches_delete_button) ImageView recent_searches_delete_button;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_history, container, false);
        ButterKnife.bind(this, rootView);
        recentSearches = recentSearchesDao.recentSearches(10);
        recent_searches_delete_button.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setMessage(getString(R.string.delete_recent_searches_dialog))
                    .setPositiveButton("YES", (dialog, which) -> {
                        recentSearchesDao.deleteAll(recentSearches);
                        Toast.makeText(getContext(),getString(R.string.search_history_deleted),Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("NO", null)
                    .create()
                    .show();

        });
        adapter = new ArrayAdapter<String>(getContext(),R.layout.item_recent_searches,recentSearches);
        recentSearchesList.setAdapter(adapter);
        recentSearchesList.setOnItemClickListener((parent, view, position, id) -> (
                (SearchActivity)getContext()).updateText(recentSearches.get(position)));
        adapter.notifyDataSetChanged();
        return rootView;
    }

    @Override
    public void onResume() {
        recentSearches = recentSearchesDao.recentSearches(10);
        adapter.notifyDataSetChanged();
        super.onResume();
    }
}
