package fr.free.nrw.commons.explore.recentsearches;

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


/**
 * Displays the recent searches screen.
 */
public class RecentSearchesFragment extends CommonsDaggerSupportFragment {
    @Inject RecentSearchesDao recentSearchesDao;
    @BindView(R.id.recent_searches_list) ListView recentSearchesList;
    List<String> recentSearches;
    ArrayAdapter adapter;
    @BindView(R.id.recent_searches_delete_button)
    ImageView recent_searches_delete_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_history, container, false);
        ButterKnife.bind(this, rootView);
        recentSearches = recentSearchesDao.recentSearches(10);
        recent_searches_delete_button.setOnClickListener(v -> new AlertDialog.Builder(getContext())
            .setMessage(getString(R.string.delete_recent_searches_dialog))
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                recentSearchesDao.deleteAll(recentSearches);
                Toast.makeText(getContext(),getString(R.string.search_history_deleted),Toast.LENGTH_SHORT).show();
                recentSearches = recentSearchesDao.recentSearches(10);
                adapter = new ArrayAdapter<String>(getContext(),R.layout.item_recent_searches, recentSearches);
                recentSearchesList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.no, null)
            .create()
            .show());
        adapter = new ArrayAdapter<String>(getContext(),R.layout.item_recent_searches, recentSearches);
        recentSearchesList.setAdapter(adapter);
        recentSearchesList.setOnItemClickListener((parent, view, position, id) -> (
                (SearchActivity)getContext()).updateText(recentSearches.get(position)));
        adapter.notifyDataSetChanged();
        return rootView;
    }

    /**
     * This method is called on back press of activity
     * so we are updating the list from database to refresh the recent searches list.
     */
    @Override
    public void onResume() {
        recentSearches = recentSearchesDao.recentSearches(10);
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    /**
     * This method is called when search query is null to update Recent Searches
     */
    public void updateRecentSearches() {
        recentSearches = recentSearchesDao.recentSearches(10);
        adapter = new ArrayAdapter<String>(getContext(),R.layout.item_recent_searches, recentSearches);
        recentSearchesList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}
