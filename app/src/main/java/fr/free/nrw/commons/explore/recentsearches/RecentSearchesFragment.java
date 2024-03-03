package fr.free.nrw.commons.explore.recentsearches;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.FragmentSearchHistoryBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.SearchActivity;
import java.util.List;
import javax.inject.Inject;


/**
 * Displays the recent searches screen.
 */
public class RecentSearchesFragment extends CommonsDaggerSupportFragment {

    @Inject
    RecentSearchesDao recentSearchesDao;
    List<String> recentSearches;
    ArrayAdapter adapter;

    private FragmentSearchHistoryBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        binding = FragmentSearchHistoryBinding.inflate(inflater, container, false);

        recentSearches = recentSearchesDao.recentSearches(10);

        if (recentSearches.isEmpty()) {
            binding.recentSearchesDeleteButton.setVisibility(View.GONE);
            binding.recentSearchesTextView.setText(R.string.no_recent_searches);
        }

        binding.recentSearchesDeleteButton.setOnClickListener(v -> {
            showDeleteRecentAlertDialog(requireContext());
        });

        adapter = new ArrayAdapter<>(requireContext(), R.layout.item_recent_searches,
            recentSearches);
        binding.recentSearchesList.setAdapter(adapter);
        binding.recentSearchesList.setOnItemClickListener((parent, view, position, id) -> (
            (SearchActivity) getContext()).updateText(recentSearches.get(position)));
        binding.recentSearchesList.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteAlertDialog(requireContext(), position);
            return true;
        });
        updateRecentSearches();

        return binding.getRoot();
    }

    private void showDeleteRecentAlertDialog(@NonNull final Context context) {
        new AlertDialog.Builder(context)
            .setMessage(getString(R.string.delete_recent_searches_dialog))
            .setPositiveButton(android.R.string.yes,
                (dialog, which) -> setDeleteRecentPositiveButton(context, dialog))
            .setNegativeButton(android.R.string.no, null)
            .create()
            .show();
    }

    private void setDeleteRecentPositiveButton(@NonNull final Context context,
        final DialogInterface dialog) {
        recentSearchesDao.deleteAll();
        if (binding != null) {
            binding.recentSearchesDeleteButton.setVisibility(View.GONE);
            binding.recentSearchesTextView.setText(R.string.no_recent_searches);
            Toast.makeText(getContext(), getString(R.string.search_history_deleted),
                Toast.LENGTH_SHORT).show();
            recentSearches = recentSearchesDao.recentSearches(10);
            adapter = new ArrayAdapter<>(context, R.layout.item_recent_searches,
                recentSearches);
            binding.recentSearchesList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        dialog.dismiss();
    }

    private void showDeleteAlertDialog(@NonNull final Context context, final int position) {
        new AlertDialog.Builder(context)
            .setMessage(R.string.delete_search_dialog)
            .setPositiveButton(getString(R.string.delete).toUpperCase(),
                ((dialog, which) -> setDeletePositiveButton(context, dialog, position)))
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show();
    }

    private void setDeletePositiveButton(@NonNull final Context context,
        final DialogInterface dialog, final int position) {
        recentSearchesDao.delete(recentSearchesDao.find(recentSearches.get(position)));
        recentSearches = recentSearchesDao.recentSearches(10);
        adapter = new ArrayAdapter<>(context, R.layout.item_recent_searches,
            recentSearches);
        if (binding != null){
            binding.recentSearchesList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        dialog.dismiss();
    }

    /**
     * This method is called on back press of activity so we are updating the list from database to
     * refresh the recent searches list.
     */
    @Override
    public void onResume() {
        updateRecentSearches();
        super.onResume();
    }

    /**
     * This method is called when search query is null to update Recent Searches
     */
    public void updateRecentSearches() {
        recentSearches = recentSearchesDao.recentSearches(10);
        adapter.notifyDataSetChanged();

        if (!recentSearches.isEmpty()) {
            if (binding!= null) {
                binding.recentSearchesDeleteButton.setVisibility(View.VISIBLE);
                binding.recentSearchesTextView.setText(R.string.search_recent_header);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (binding != null) {
            binding = null;
        }
    }
}
